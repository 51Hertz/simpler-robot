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

plugins {
    id("simbot.simple-module-conventions")
    id("simbot.maven-publish")
    kotlin("plugin.serialization")
}


dependencies {
    // simbot-core 使用 logger
    api(project(":simbot-apis:simbot-logger"))
    api(project(":simbot-apis:simbot-api"))
    api(libs.slf4j.api)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.jdk8)
    api(libs.kotlinx.serialization.core)

    compileOnly(libs.kotlinx.coroutines.reactive)
    compileOnly(libs.kotlinx.coroutines.reactor)
    compileOnly(libs.kotlinx.coroutines.rx2)
    compileOnly(libs.kotlinx.coroutines.rx3)

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.serialization.properties)
    testImplementation(libs.kotlinx.serialization.protobuf)

}
