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

package love.forte.simbot

/**
 * 标记为供Java使用的阻塞API，不建议Kotlin使用。对于Kotlin应存在其他更优的代替方法。
 */
@RequiresOptIn("Blocking API marked for Java use, not recommended for Kotlin.", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class Api4J

/**
 * 标记为可能存在同含义的非阻塞API的阻塞API。应优先考虑使用同含义的非阻塞的API。
 */
@RequiresOptIn("Blocking APIs marked as possibly having non-blocking APIs with the same meaning should be given preference over non-blocking APIs with the same meaning.", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class BlockingApi


/**
 * 标标记为实验性的API，不保证稳定性且可能会随时发生无提示的变更或被删除。
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
@RequiresOptIn("APIs marked as experimental are not guaranteed to be stable and may be subject to unprompted change at any time.", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class ExperimentalSimbotApi


/**
 * 标记一个作为内部API所使用的相关内容。如无必要则不应该使用内部API。一个内部API可能会在没有任何通知的情况下发生变更、删除。
 */
@RequiresOptIn(
    message = "Marked as an internal API, its availability is not guaranteed and there will be no notification of changes.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
public annotation class InternalSimbotApi


/**
 * 标记为可能存在使用限制、严格要求或者存在特殊规则的API，需要仔细阅读说明且谨慎使用。
 */
@RequiresOptIn(
    message = "APIs marked as having possible usage restrictions, strict requirements or special rules need to be read carefully and used with caution.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
public annotation class DiscreetSimbotApi


/**
 * 标记为十分脆弱的、存在性能瓶颈、限制或有更好替代品的API，应阅读相应的文档并选择更优方案，同时尽量避免使用相关API。
 *
 * 被标记的相关内容可能会在未来进行优化、变更或删除。
 */
@RequiresOptIn(
    message = "APIs that are marked as very vulnerable, have performance bottlenecks, limitations, or have better alternatives should be read in the appropriate documentation and chosen as the better solution, while avoiding the use of the API in question as much as possible.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
public annotation class FragileSimbotApi


/**
 * 标记那些能够监听，但是不建议监听的事件类型，
 * 常见于一些携带泛型的事件类型。
 *
 * @see love.forte.simbot.event.Event
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS
)
@MustBeDocumented
public annotation class NotSuggestedEvent // todo?


@RequiresOptIn(
    message = "You have found a bonus! But you're better off using something normal.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
public annotation class Bonus