/*
 *  Copyright (c) 2022-2022 ForteScarlet <ForteScarlet@163.com>
 *
 *  本文件是 simply-robot (或称 simple-robot 3.x 、simbot 3.x ) 的一部分。
 *
 *  simply-robot 是自由软件：你可以再分发之和/或依照由自由软件基金会发布的 GNU 通用公共许可证修改之，无论是版本 3 许可证，还是（按你的决定）任何以后版都可以。
 *
 *  发布 simply-robot 是希望它能有用，但是并无保障;甚至连可销售和符合某个特定的目的都不保证。请参看 GNU 通用公共许可证，了解详情。
 *
 *  你应该随程序获得一份 GNU 通用公共许可证的复本。如果没有，请看:
 *  https://www.gnu.org/licenses
 *  https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *  https://www.gnu.org/licenses/lgpl-3.0-standalone.html
 *
 */

package love.forte.simbot.core.event

import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.serialization.modules.SerializersModule
import love.forte.simbot.*
import love.forte.simbot.core.scope.SimpleScope
import love.forte.simbot.event.*
import love.forte.simbot.event.EventListener
import love.forte.simbot.utils.ListView
import love.forte.simbot.utils.view
import org.slf4j.Logger
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

internal class SimpleEventListenerManagerImpl internal constructor(
    configuration: SimpleListenerManagerConfiguration,
) : SimpleEventListenerManager {
    private companion object {
        private val counter: AtomicInteger = AtomicInteger(0)
        private val logger: Logger =
            LoggerFactory.getLogger("love.forte.simbot.core.event.SimpleEventListenerManagerImpl")
    }
    
    private val managerCoroutineContext: CoroutineContext
    private val managerScope: CoroutineScope
    
    /**
     * 异常处理器。
     */
    private val listenerExceptionHandler: ((Throwable) -> EventResult)?
    
    
    /**
     * 事件过程拦截器入口。
     */
    private val processingInterceptEntrance: EventInterceptEntrance<EventProcessingInterceptor.Context, EventProcessingResult, EventProcessingContext>
    
    /**
     * 监听函数拦截器集。
     */
    private val listenerIntercepts: List<EventListenerInterceptor>
    
    /**
     * 监听函数列表。ID唯一
     */
    private val listeners: MutableMap<String, EventListener>
    
    
    /**
     * 完成缓存与处理的监听函数队列.
     */
    private val resolvedInvokers: MutableMap<Event.Key<*>, List<ListenerInvoker>> = LinkedHashMap()
    
    
    init {
        val simpleListenerManagerConfig: SimpleListenerManagerConfig = configuration.build()
        val context = simpleListenerManagerConfig.coroutineContext
        
        managerCoroutineContext =
            context.minusKey(Job) + CoroutineName("SimpleListenerManager#${counter.getAndIncrement()}")
        
        managerScope = CoroutineScope(managerCoroutineContext)
        
        listenerExceptionHandler = simpleListenerManagerConfig.exceptionHandler
        
        processingInterceptEntrance =
            EventInterceptEntrance.eventProcessingInterceptEntrance(simpleListenerManagerConfig.processingInterceptors.values.sortedBy { it.priority })
        
        listenerIntercepts = simpleListenerManagerConfig.listenerInterceptors.values.sortedBy { it.priority }
        
        listeners = simpleListenerManagerConfig.listeners.associateByTo(mutableMapOf()) { it.id }
    }
    
    
    private fun getInvokers(type: Event.Key<*>): List<ListenerInvoker> {
        val cached = resolvedInvokers[type]
        if (cached != null) return cached
        synchronized(this) {
            val entrant = resolvedInvokers[type]
            if (entrant != null) return entrant
            // 计算缓存
            val compute = listeners.values
                .filter { it.isTarget(type) }
                .map(::ListenerInvoker)
                .sortedWith { o1, o2 ->
                    if (o1.isAsync == o2.isAsync) {
                        o1.listener.priority.compareTo(o2.listener.priority)
                    } else {
                        if (o1.isAsync) 1 else 0
                    }
                }.ifEmpty { emptyList() }
            resolvedInvokers[type] = compute
            return compute
        }
    }
    
    
    /**
     * 注册一个监听函数。
     *
     * 每次注册监听函数都会直接清空缓存。
     *
     */
    @FragileSimbotApi
    override fun register(listener: EventListener) {
        synchronized(this) {
            val id = listener.id
            listeners.compute(id) { _, old ->
                if (old != null) throw IllegalStateException("The event listener with ID $id already exists")
                listener.also {
                    resolvedInvokers.clear()
                }
            }
        }
    }
    
    /**
     * 获取一个监听函数。
     */
    override fun get(id: String): EventListener? = synchronized(this) { listeners[id] }
    
    /**
     * 判断指定事件类型在当前事件管理器中是否能够被执行（存在任意对应的监听函数）。
     */
    override operator fun contains(eventType: Event.Key<*>): Boolean {
        return getInvokers(eventType).isNotEmpty()
    }
    
    override fun isProcessable(eventKey: Event.Key<*>): Boolean {
        return resolver.isProcessable(eventKey) || getInvokers(eventKey).isNotEmpty()
    }
    
    /**
     * 推送一个事件。
     */
    override suspend fun push(event: Event): EventProcessingResult {
        val invokers = getInvokers(event.key)
        if (invokers.isEmpty()) {
            if (resolver.isProcessable(event.key)) {
                resolver.resolveEventToContext(event, 0)
            }
            return EventProcessingResult
        }
        
        return resolveToContext(event, invokers.size)?.let {
            doInvoke(it, invokers)
        } ?: EventProcessingResult
        
    }
    
    @Api4J
    override fun pushAsync(event: Event): CompletableFuture<EventProcessingResult> {
        val invokers = getInvokers(event.key)
        if (invokers.isEmpty()) {
            managerScope.launch {
                if (resolver.isProcessable(event.key)) {
                    resolver.resolveEventToContext(event, 0)
                }
            }
            return CompletableFuture<EventProcessingResult>().also {
                it.complete(EventProcessingResult)
            }
        }
        
        
        val deferred = managerScope.async {
            resolveToContext(event, invokers.size)?.let { context ->
                doInvoke(context, invokers)
            } ?: EventProcessingResult
        }
        
        return deferred.asCompletableFuture()
    }
    
    
    /**
     * 切换到当前管理器中的调度器并触发对应事件的内容。
     */
    private suspend fun doInvoke(
        context: SimpleEventProcessingContext,
        invokers: List<ListenerInvoker>,
    ): EventProcessingResult {
        val currentBot = context.event.bot
        // val dispatchContext = currentBot.coroutineContext + managerCoroutineContext
        
        return withContext(managerCoroutineContext + context) {
            kotlin.runCatching {
                processingInterceptEntrance.doIntercept(context) { processingContext ->
                    // do invoke with intercept
                    for (invoker in invokers) {
                        val listenerContext = processingContext.withListener(invoker.listener)
                        val handleResult = runForEventResultWithHandler {
                            // maybe scope use bot?
                            invoker(managerScope, listenerContext)
                        }
                        val result = if (handleResult.isFailure) {
                            if (logger.isErrorEnabled) {
                                val err = handleResult.exceptionOrNull()
                                logger.error(
                                    "Listener [${invoker.listener.id}] process failed: $err",
                                    err!!
                                )
                            }
                            EventResult.invalid()
                        } else {
                            handleResult.getOrNull()!!
                        }
                        
                        // append result
                        val type = appendResult(context, result)
                        if (type == ListenerInvokeType.TRUNCATED) {
                            break
                        }
                    }
                    
                    // resolve to processing result
                    SimpleEventProcessingResult(context.results)
                }
            }.getOrElse {
                currentBot.logger.error("Event process failed.", it)
                EventProcessingResult
            }
        }
    }
    
    private inline fun runForEventResultWithHandler(block: () -> EventResult): Result<EventResult> {
        val result = runCatching(block)
        if (result.isSuccess || listenerExceptionHandler == null) return result
        
        val exception = result.exceptionOrNull()!!
        
        val result0 = runCatching {
            listenerExceptionHandler!!.invoke(exception)
        }
        if (result0.isSuccess) return result0
        val ex2 = result0.exceptionOrNull()!!
        ex2.addSuppressed(exception)
        return Result.failure(ex2)
    }
    
    
    private val resolver: SimpleEventProcessingContextResolver = SimpleEventProcessingContextResolver(managerScope)
    
    
    @ExperimentalSimbotApi
    override val globalScopeContext: ScopeContext
        get() = resolver.globalContext
    
    @ExperimentalSimbotApi
    override val continuousSessionContext: ContinuousSessionContext
        get() = resolver.continuousSessionContext
    
    /**
     * 通过 [Event] 得到一个 [EventProcessingContext].
     */
    private suspend fun resolveToContext(event: Event, listenerSize: Int): SimpleEventProcessingContext? {
        return resolver.resolveEventToContext(event, listenerSize)
    }
    
    private suspend fun appendResult(context: SimpleEventProcessingContext, result: EventResult): ListenerInvokeType {
        return resolver.appendResultIntoContext(context, result)
    }
    
    
    internal inner class ListenerInvoker(
        val listener: EventListener,
    ) : suspend (CoroutineScope, EventListenerProcessingContext) -> EventResult {
        val isAsync = listener.isAsync
        
        // private val listenerInterceptEntranceWithPoint: Map<EventListenerInterceptor.Point, EventInterceptEntrance<EventListenerInterceptor.Context, EventResult, EventListenerProcessingContext>>
        private val function: suspend (CoroutineScope, EventListenerProcessingContext) -> EventResult
        
        init {
            val listenerInterceptsPointMap =
                EnumMap<EventListenerInterceptor.Point, EventInterceptEntrance<EventListenerInterceptor.Context, EventResult, EventListenerProcessingContext>>(
                    EventListenerInterceptor.Point::class.java
                )
            
            listenerIntercepts.groupBy { interceptor -> interceptor.point }
                .mapValuesTo(listenerInterceptsPointMap) { (_, listenerInterceptsInCurrentPoint) ->
                    EventInterceptEntrance.eventListenerInterceptEntrance(listenerInterceptsInCurrentPoint)
                }
            
            
            suspend fun runner(
                listener: EventListener,
                context: EventListenerProcessingContext,
            ): EventResult {
                val defaultEntrance = listenerInterceptsPointMap.getOrDefault(
                    EventListenerInterceptor.Point.DEFAULT,
                    EventInterceptEntrance.eventListenerInterceptEntrance()
                )
                val afterMatchEntrance = listenerInterceptsPointMap.getOrDefault(
                    EventListenerInterceptor.Point.AFTER_MATCH,
                    EventInterceptEntrance.eventListenerInterceptEntrance()
                )
                
                return defaultEntrance.doIntercept(context) { innerContext ->
                    if (listener.match(innerContext)) {
                        afterMatchEntrance.doIntercept(innerContext) { afterInnerContext ->
                            listener.invoke(afterInnerContext)
                        }
                    } else {
                        EventResult.Invalid
                    }
                }
            }
            
            suspend fun asyncFunctionRunner(
                listener: EventListener,
                scope: CoroutineScope,
                context: EventListenerProcessingContext,
            ): EventResult {
                val asyncDeferred = scope.async {
                    runner(listener, context)
                }
                asyncDeferred.start()
                return EventResult.async(asyncDeferred)
            }
            
            
            suspend fun suspendFunctionRunner(
                listener: EventListener,
                context: EventListenerProcessingContext,
            ): EventResult {
                return runner(listener, context)
            }
            
            
            val functionRunner: suspend (CoroutineScope, EventListenerProcessingContext) -> EventResult = if (isAsync) {
                { s, c -> asyncFunctionRunner(listener, s, c) }
            } else {
                { _, c -> suspendFunctionRunner(listener, c) }
            }
            
            function = functionRunner
        }
        
        
        override suspend fun invoke(scope: CoroutineScope, context: EventListenerProcessingContext): EventResult {
            return try {
                function(scope, context)
            } catch (listenerEx: EventListenerProcessingException) {
                throw listenerEx
            } catch (invocationEx: InvocationTargetException) {
                throw EventListenerProcessingException(invocationEx.targetException)
            } catch (anyEx: Throwable) {
                throw EventListenerProcessingException(anyEx)
            }
        }
        
    }
    
}


private data class SimpleEventProcessingResult(override val results: List<EventResult>) : EventProcessingResult


/**
 * 向当前的 [EventProcessingContext] 提供一个监听函数 [listener], 使其成为 [EventListenerProcessingContext].
 *
 * @param listener 监听函数
 * @receiver 事件处理上下文
 * @return 监听函数处理上下文
 */
private fun EventProcessingContext.withListener(listener: EventListener): EventListenerProcessingContext =
    CoreEventListenerProcessingContext(this, listener)


private class CoreEventListenerProcessingContext(
    processingContext: EventProcessingContext,
    override val listener: EventListener,
) : EventListenerProcessingContext, EventProcessingContext by processingContext {
    override var textContent: String? = with(processingContext.event) {
        if (this is MessageEvent) messageContent.plainText else null
    }
}


@OptIn(ExperimentalSimbotApi::class)
internal class SimpleEventProcessingContext(
    override val event: Event,
    override val messagesSerializersModule: SerializersModule,
    private val globalScopeContext: GlobalScopeContext,
    private val continuousSessionContext: SimpleContinuousSessionContext,
    resultInitSize: Int,
) : EventProcessingContext {
    
    private val _results = ArrayList<EventResult>(resultInitSize)
    
    @Volatile
    private var resultView: ListView<EventResult>? = null
    
    override val results: List<EventResult> // = _results.view()
        get() {
            // dont care sync
            return resultView ?: _results.view().also {
                resultView = it
            }
        }
    
    internal fun addResult(result: EventResult) {
        _results.add(result)
    }
    
    private lateinit var instantScope0: InstantScopeContext
    private val instantScope: InstantScopeContext
        get() {
            if (::instantScope0.isInitialized) {
                return instantScope0
            }
            return synchronized(this) {
                if (::instantScope0.isInitialized) {
                    instantScope0
                } else {
                    SimpleEventProcessingContextResolver.InstantScopeContextImpl(
                        AttributeMutableMap(ConcurrentHashMap())
                    ).also {
                        instantScope0 = it
                    }
                }
            }
        }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAttribute(attribute: Attribute<T>): T? {
        return when (attribute) {
            SimpleScope.Global -> globalScopeContext as T
            SimpleScope.ContinuousSession -> continuousSessionContext as T
            else -> instantScope[attribute]
        }
    }
    
    override fun <T : Any> get(attribute: Attribute<T>): T? {
        return getAttribute(attribute)
    }
    
    override fun <T : Any> contains(attribute: Attribute<T>): Boolean {
        return when (attribute) {
            SimpleScope.Global -> true
            SimpleScope.ContinuousSession -> true
            else -> attribute in instantScope
        }
    }
    
    override fun size(): Int {
        return instantScope.size() + 2
    }
    
    override fun <T : Any> put(attribute: Attribute<T>, value: T): T? {
        return instantScope.put(attribute, value)
    }
    
    override fun <T : Any> merge(attribute: Attribute<T>, value: T, remapping: (T, T) -> T): T {
        return instantScope.merge(attribute, value, remapping)
    }
    
    override fun <T : Any> computeIfAbsent(attribute: Attribute<T>, mappingFunction: (Attribute<T>) -> T): T {
        return instantScope.computeIfAbsent(attribute, mappingFunction)
    }
    
    override fun <T : Any> computeIfPresent(attribute: Attribute<T>, remappingFunction: (Attribute<T>, T) -> T?): T? {
        return instantScope.computeIfPresent(attribute, remappingFunction)
    }
    
    override fun <T : Any> remove(attribute: Attribute<T>): T? {
        return instantScope.remove(attribute)
    }
}

