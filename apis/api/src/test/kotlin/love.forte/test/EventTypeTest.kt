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

package love.forte.test

import love.forte.simbot.event.Event
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.event.RequestEvent
import love.forte.simbot.event.isSubFrom
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.test.Test

/**
 *
 * @author ForteScarlet
 */
class EventTypeTest {

    @Test
    fun test() {
        val type = RequestEvent::class.type()!!
        println(type)
        println(type.id)
        println(type isSubFrom Event)
        println(type isSubFrom MessageEvent)

    }

    fun KClass<out Event>.type(): Event.Key<*>? {
        return this.nestedClasses.firstOrNull { it.isCompanion }?.takeIf {
            it.isSubclassOf(Event.Key::class)
        }?.objectInstance as Event.Key<*>

    }
}