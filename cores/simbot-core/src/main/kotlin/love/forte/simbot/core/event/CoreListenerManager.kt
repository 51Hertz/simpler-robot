/*
 *  Copyright (c) 2021-2022 ForteScarlet <ForteScarlet@163.com>
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
import love.forte.simbot.event.*
import love.forte.simbot.utils.view
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


/**
 *
 * 监听函数执行的异常处理器。
 * 当 [CoreListenerManager] 中的某一个 [EventListener] 执行过程中出现了异常（包括其过程中所经过的拦截器或过滤器），
 * 则本次执行内容与对应异常将会交由一个 **唯一** 的一场管理器进行处理，并得到一个应得的结果。
 *
 * 原则上异常处理器内部应当尽可能避免再次出现异常。
 *
 * TODO
 */
public typealias EventListenerExceptionHandler = suspend (EventListenerProcessingContext, Throwable) -> EventResult


/**
 * 核心监听函数管理器。
 *
 * ## 调度
 * [CoreListenerManager] 遵守 [EventProcessor] 接口描述，通过当前事件中的 [bot][Event.bot] 所提供的作用域进行事件调度。
 *
 *
 * ## 异步函数
 * [CoreListenerManager] 中，对于一个异步函数 ([EventListener.isAsync] == true 的函数) 的处理方式与其接口定义的描述相同，
 * 对于这个异步函数的拦截器会与当前异步函数共同进入一个由当前事件管理器所提供的异步任务中，并对当前的 [EventProcessingContext]
 * 立即返回一个 [AsyncEventResult].
 *
 *
 *
 * ## 监听函数的解析、缓存与获取
 * 在 [CoreListenerManager] 中，真正被执行的监听函数是经过缓存与转化的，它们只会在遇到一个目前缓存中未知的事件类型的时候进行同步转化缓存。
 *
 * 因此先通过 [isProcessable] 判断是否支持当前事件类型，再进行事件的构建是个不错的优化方案：
 * ```kotlin
 * if(eventManager.isProcessable(FooEvent)) {
 *    val event = FooEvent(...)
 *    eventManager.push(event)
 * }
 * ```
 * 或使用扩展函数 [pushIfProcessable]：
 * ```kotlin
 * eventManager(FooEvent) {
 *    val event = FooEvent(...)
 *    eventManager.push(event)
 * }
 * ```
 *
 * 这样可以一定程度上避免或降低频繁创建不会被使用的事件对象。
 *
 * 但是这存在例外：当存在任何通过 [ContinuousSessionContext] 而注册的持续会话监听函数的时候，[isProcessable] 将会始终得到 `true`。
 *
 *
 *
 */
public interface CoreListenerManager : EventListenerManager {
    
    
    /**
     * 判断指定事件类型在当前事件管理器中是否能够被执行（存在任意对应的监听函数）。
     *
     * e.g.
     * ```kotlin
     * if (FriendMessageEvent in manager) { ... }
     * ```
     */
    public operator fun contains(eventType: Event.Key<*>): Boolean
    
    
    public companion object {
        /**
         * 通过配置信息构建一个 [CoreListenerManager] 实例。
         */
        @JvmStatic
        public fun newInstance(configuration: CoreListenerManagerConfiguration): CoreListenerManager =
            CoreListenerManagerImpl(configuration)
        
    }
}


internal class CoreListenerManagerImpl internal constructor(
    configuration: CoreListenerManagerConfiguration,
) : CoreListenerManager {
    private companion object {
        private val counter: AtomicInteger = AtomicInteger(0)
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
    private val listeners: MutableMap<CharSequenceID, EventListener> // =
    
    
    /**
     * 完成缓存与处理的监听函数队列.
     */
    private val resolvedInvokers: MutableMap<Event.Key<*>, List<ListenerInvoker>> = LinkedHashMap()
    
    
    init {
        val coreListenerManagerConfig: CoreListenerManagerConfig = configuration.build()
        val context = coreListenerManagerConfig.coroutineContext
        // TODO Job?
        // context.minusKey(Job) + CoroutineName("CoreListenerManager#${counter.getAndIncrement()}")
        
        managerCoroutineContext =
            context.minusKey(Job) + CoroutineName("CoreListenerManager#${counter.getAndIncrement()}")
        
        managerScope = CoroutineScope(managerCoroutineContext)
        
        listenerExceptionHandler = coreListenerManagerConfig.exceptionHandler
        
        processingInterceptEntrance =
            EventInterceptEntrance.eventProcessingInterceptEntrance(coreListenerManagerConfig.processingInterceptors.values.sortedBy { it.priority })
        
        listenerIntercepts = coreListenerManagerConfig.listenerInterceptors.values.sortedBy { it.priority }
        
        listeners = coreListenerManagerConfig.listeners.associateByTo(mutableMapOf()) { it.id.toCharSequenceID() }
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
            val id = listener.id.toCharSequenceID()
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
    override fun get(id: ID): EventListener? = synchronized(this) { listeners[id.toCharSequenceID()] }
    
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
        
        return doInvoke(resolveToContext(event, invokers.size), invokers)
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
        
        
        val deferred = managerScope.async { doInvoke(resolveToContext(event, invokers.size), invokers) }
        return deferred.asCompletableFuture()
    }
    
    
    /**
     * 切换到当前管理器中的调度器并触发对应事件的内容。
     */
    private suspend fun doInvoke(
        context: CoreEventProcessingContext,
        invokers: List<ListenerInvoker>,
    ): EventProcessingResult {
        val currentBot = context.event.bot
        // val dispatchContext = currentBot.coroutineContext + managerCoroutineContext
        
        return withContext(managerCoroutineContext + context) {
            kotlin.runCatching {
                processingInterceptEntrance.doIntercept(context) {
                    // do invoke with intercept
                    for (invoker in invokers) {
                        val listenerContext = context.withListener(invoker.listener)
                        val handleResult = runForEventResultWithHandler {
                            // maybe scope use bot?
                            invoker(managerScope, listenerContext)
                        }
                        val result = if (handleResult.isFailure) {
                            val err = handleResult.exceptionOrNull()
                            invoker.listener.logger.error(
                                "Listener process failed: $err",
                                err!!
                            )
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
                    CoreEventProcessingResult(context.results)
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
    
    
    private val resolver: CoreEventProcessingContextResolver = CoreEventProcessingContextResolver(managerScope)
    
    
    @ExperimentalSimbotApi
    override val globalScopeContext: ScopeContext
        get() = resolver.globalContext
    
    @ExperimentalSimbotApi
    override val continuousSessionContext: ContinuousSessionContext
        get() = resolver.continuousSessionContext
    
    /**
     * 通过 [Event] 得到一个 [EventProcessingContext].
     */
    private suspend fun resolveToContext(event: Event, listenerSize: Int): CoreEventProcessingContext {
        return resolver.resolveEventToContext(event, listenerSize)
    }
    
    private suspend fun appendResult(context: CoreEventProcessingContext, result: EventResult): ListenerInvokeType {
        return resolver.appendResultIntoContext(context, result)
    }
    
    
    internal inner class ListenerInvoker(
        val listener: EventListener,
    ) : suspend (CoroutineScope, EventListenerProcessingContext) -> EventResult {
        val isAsync = listener.isAsync
        private val listenerInterceptEntrance =
            EventInterceptEntrance.eventListenerInterceptEntrance(listener, listenerIntercepts)
        
        private val function: suspend (CoroutineScope, EventListenerProcessingContext) -> EventResult =
            if (isAsync) {
                { scope, context ->
                    val asyncDeferred = scope.async {
                        listenerInterceptEntrance.doIntercept(context, listener::invoke)
                    }
                    asyncDeferred.start()
                    EventResult.async(asyncDeferred)
                }
            } else {
                { _, context -> listenerInterceptEntrance.doIntercept(context, listener::invoke) }
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


/**
 * 事件流程上下文的管理器，[CoreListenerManager] 通过此接口实例完成对 [EventProcessingContext] 的统一管理。
 *
 *  在 [CoreListenerManager] 中仅会使用同一个 [EventProcessingContextResolver] 实例。
 *
 */
public interface EventProcessingContextResolver<C : EventProcessingContext> {
    
    /**
     * 获取为当前manager服务的全局作用域对象。
     * 作为一个全局作用域，它理应能够脱离事件调用流程之外而获取。
     */
    @ExperimentalSimbotApi
    public val globalContext: ScopeContext
    
    /**
     * 获取为当前manager服务的持续会话作用域。
     * 持续会话作用域与一个独立的监听函数无关，因此应当能够脱离监听函数流程之外而获取。
     */
    @ExperimentalSimbotApi
    public val continuousSessionContext: ContinuousSessionContext
    
    /**
     * 检测当前事件是否允许监听。
     * 会在监听函数管理器检测前进行检测， [isProcessable] 与 [EventListenerManager.isProcessable] 任意结果为true均会触发事件监听。
     *
     */
    public fun isProcessable(eventKey: Event.Key<*>): Boolean
    
    /**
     * 根据一个事件得到对应的流程上下文。
     * 只有在对应事件存在至少一个对应的监听函数的时候才会被触发。
     */
    @JvmSynthetic
    public suspend fun resolveEventToContext(event: Event, listenerSize: Int): C
    
    /**
     * 向提供的上下文 [C] 的 [EventProcessingContext.results] 中追加一个 [EventResult].
     *
     * [CoreListenerManager] 会对所有得到的结果进行尝试推送，包括 [EventResult.Invalid],
     * 但是建议不会真正的添加 [EventResult.Invalid].
     *
     *
     * ### Reactive API
     * 在核心模块的默认实现下，[appendResultIntoContext] 中支持对 `reactive API` 的相关支持。
     *
     *
     * 详情请参考 [kotlinx-coroutine-reactive](https://github.com/Kotlin/kotlinx.coroutines/blob/master/reactive/README.md) 。
     *
     * @see EventResult.content
     */
    @JvmSynthetic
    public suspend fun appendResultIntoContext(context: C, result: EventResult): ListenerInvokeType
}

/**
 * 监听函数执行状态。由 [EventProcessingContextResolver.appendResultIntoContext] 进行返回。
 */
public enum class ListenerInvokeType {
    /**
     * 继续后续监听函数的执行。
     */
    CONTINUE,
    
    /**
     * 截断后续执行，即在当前点终止。
     */
    TRUNCATED,
    
}


@OptIn(ExperimentalSimbotApi::class)
internal class CoreEventProcessingContext(
    override val event: Event,
    override val messagesSerializersModule: SerializersModule,
    private val globalScopeContext: GlobalScopeContext,
    private val continuousSessionContext: SimpleContinuousSessionContext,
    private val instantScopeContextInitializer: () -> InstantScopeContext,
    private val attributeMap: AttributeMap,
    resultInitSize: Int,
) : EventProcessingContext {
    
    
    @Suppress("PropertyName")
    @JvmSynthetic
    val _results = ArrayList<EventResult>(resultInitSize)
    
    override val results: List<EventResult> = _results.view()
    
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
                    instantScopeContextInitializer().also {
                        instantScope0 = it
                    }
                }
            }
        }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getAttribute(attribute: Attribute<T>): T? {
        return when (attribute) {
            @Suppress("DEPRECATION")
            EventProcessingContext.Scope.Instant,
            -> this as T
            EventProcessingContext.Scope.Global -> globalScopeContext as T
            EventProcessingContext.Scope.ContinuousSession -> continuousSessionContext as T
            else -> attributeMap[attribute]
        }
    }
    
    override fun <T : Any> get(attribute: Attribute<T>): T? {
        return getAttribute(attribute)
    }
    
    override fun <T : Any> contains(attribute: Attribute<T>): Boolean {
        return when(attribute) {
            @Suppress("DEPRECATION")
            EventProcessingContext.Scope.Instant -> true
            EventProcessingContext.Scope.Global -> true
            EventProcessingContext.Scope.ContinuousSession -> true
            else -> attribute in attributeMap
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


private data class CoreEventProcessingResult(override val results: List<EventResult>) : EventProcessingResult


/**
 * 向当前的 [EventProcessingContext] 提供一个监听函数 [listener], 使其成为 [EventListenerProcessingContext].
 *
 * @param listener 监听函数
 * @receiver 事件处理上下文
 * @return 监听函数处理上下文
 */
internal fun EventProcessingContext.withListener(listener: EventListener): EventListenerProcessingContext =
    CoreEventListenerProcessingContext(this, listener)


private class CoreEventListenerProcessingContext(
    processingContext: EventProcessingContext,
    override val listener: EventListener,
) : EventListenerProcessingContext, EventProcessingContext by processingContext {
    override var textContent: String? = with(processingContext.event) {
        if (this is MessageEvent) messageContent.plainText else null
    }
}