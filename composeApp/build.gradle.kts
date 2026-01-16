import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization")
    alias(libs.plugins.androidx.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.preview)

            // REPLACING ROOM (KMP version)
            implementation(libs.androidx.room.runtime)
            implementation("androidx.sqlite:sqlite-bundled:2.5.0-alpha11")

            // Datastore
            implementation("androidx.datastore:datastore-preferences:1.1.1")
            implementation("androidx.datastore:datastore:1.1.1")

            // FOR SPOTIFY API (Replacing Android SDK)
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-cio:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

            // Loopback browser for spotify auth
            implementation("io.ktor:ktor-server-netty:2.3.12")

            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        val jvmMain by getting {
            // Add the KSP generated directory to the JVM source set
            kotlin.srcDir("build/generated/ksp/jvm/jvmMain/kotlin")
        }
    }
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
}

ksp {
    arg("room.generateKotlin", "true")
}

compose.desktop {
    application {
        mainClass = "com.truerandom.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.truerandom"
            packageVersion = "1.0.0"
        }
    }
}

composeCompiler {
    featureFlags.set(emptyList())
    // Disable source information to ensure a clean build on Kotlin 2.1.0
    includeSourceInformation.set(false)
}