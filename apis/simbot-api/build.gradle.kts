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
    `java-library`
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("kotlin-kapt")
}

dependencies {
    api(V.Kotlin.Reflect.notation)
    api(V.Kotlinx.Coroutines.Core.Jvm.notation)
    api(V.Kotlinx.Coroutines.J8.notation)
    api(V.Kotlinx.Serialization.Core.notation)
    api(V.Slf4j.Api.notation)
    compileOnly(V.Jetbrains.Annotations.notation)
    
    compileOnly(V.Kotlinx.Serialization.Json.notation)
    compileOnly(V.Kotlinx.Serialization.Properties.notation)
    compileOnly(V.Kotlinx.Serialization.Yaml.notation)
    compileOnly(V.Kotlinx.Serialization.Protobuf.notation)
    
    testImplementation(V.Kotlinx.Serialization.Json.notation)
    testImplementation(V.Kotlinx.Serialization.Yaml.notation)
    testImplementation(V.Kotlinx.Serialization.Properties.notation)
    testImplementation(V.Kotlinx.Serialization.Protobuf.notation)
    
    testImplementation(V.Kotlin.Test.Testng.notation)
    
    testImplementation("org.openjdk.jmh:jmh-core:1.33")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.35")
    kaptTest("org.openjdk.jmh:jmh-generator-annprocess:1.35")
}



tasks.getByName<Test>("test") {
    useTestNG()
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            skipEmptyPackages.set(true)
            includes.from("Module.md")
            displayName.set("api")
            perPackageOption {
                // this.
            }
        }
    }
}



kotlin {
        explicitApi()
    
    
    sourceSets.all {
        languageSettings {
            optIn("kotlin.RequiresOptIn")
        }
    }
}
