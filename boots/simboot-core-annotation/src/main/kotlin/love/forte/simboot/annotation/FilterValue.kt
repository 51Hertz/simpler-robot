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
 *
 */

package love.forte.simboot.annotation

/**
 * 指定一个参数，此参数为通过 [love.forte.simboot.filter.Keyword]
 * 解析而得到的动态参数提取器中的内容。
 *
 * @property value 所需动态参数的key。
 * @property required 对于参数绑定器来讲其是否为必须的。
 *  如果不是必须的，则在无法获取参数后传递null作为结果，否则将会抛出异常并交由后续绑定器处理。
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class FilterValue(val value: String, val required: Boolean = true)
