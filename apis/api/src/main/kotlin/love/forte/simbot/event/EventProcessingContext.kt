/*
 *  Copyright (c) 2021-2021 ForteScarlet <https://github.com/ForteScarlet>
 *
 *  根据 Apache License 2.0 获得许可；
 *  除非遵守许可，否则您不得使用此文件。
 *  您可以在以下网址获取许可证副本：
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   有关许可证下的权限和限制的具体语言，请参见许可证。
 */

package love.forte.simbot.event

import love.forte.simbot.Attribute
import love.forte.simbot.AttributeContainer
import love.forte.simbot.MutableAttributeMap
import love.forte.simbot.attribute
import org.jetbrains.annotations.UnmodifiableView
import kotlin.coroutines.CoroutineContext


/**
 *
 * 整个事件流程中进行传递的上下文。
 *
 * 此流程上下文由事件被触发开始，从头至尾完成参与完成流程下各个节点的信息传递。
 *
 * 事件流程中进行流转的上下文也是一个协程上下文.
 * @author ForteScarlet
 */
public interface EventProcessingContext : CoroutineContext.Element, AttributeContainer {
    public companion object Key : CoroutineContext.Key<EventProcessingContext>
    override val key: CoroutineContext.Key<*> get() = Key


    /**
     * 事件流程上下文的部分作用域。 [Scope] 中的所有作用域应该按照约定由 [EventProcessingContext] 的产生者进行实现与提供。
     *
     * 通过 [getAttribute] 获取对应作用域结果。
     *
     */
    @Suppress("NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING")
    public object Scope {
        /**
         * 全局作用域。 一个 [ScopeContext], 此作用域下的内容应当保持.
         *
         */
        @JvmStatic
        public val Global = attribute<ScopeContext>("context.scope.global")

        /**
         * 瞬时作用域，每一次的事件处理流程都是一个新的 [ScopeContext].
         */
        @JvmStatic
        public val Instant = attribute<ScopeContext>("context.scope.instant")


        // 持续会话作用域


    }


    /**
     * 本次监听流程中的事件主题。
     */
    public val event: Event

    /**
     * 已经执行过的所有监听函数的结果。
     *
     * 此列表仅由事件处理器内部操作，是一个对外不可变视图。
     */
    public val results: @UnmodifiableView List<EventResult>

    /**
     * 根据一个 [Attribute] 得到一个属性。
     *
     */
    override fun <T : Any> getAttribute(attribute: Attribute<T>): T?

    // 其他参数

    // 事件 processor

}


/**
 * 作用域上下文，提供部分贯穿事件的作用域参数信息。
 */
public interface ScopeContext : MutableAttributeMap