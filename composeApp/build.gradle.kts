import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization")
    alias(libs.plugins.androidx.room)
    id("com.codingfeline.buildkonfig")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

buildkonfig {
    packageName = "com.truerandom.build" // This is where the generated class will live
    objectName = "AppConfig" // The name of the generated object (default is BuildKonfig)

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "SPOTIFY_CLIENT_ID",
            localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "SPOTIFY_CLIENT_SECRET",
            localProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: ""
        )
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    jvm()
    
    sourceSets {
        val commonMain by getting {
            // This helps the IDE "see" AppConfig in your common code
            kotlin.srcDir("build/generated/source/buildKonfig/commonMain/kotlin")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.preview)

            // ROOM
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

            // Base ViewModel and Coroutines integration
            implementation(libs.androidx.lifecycle.viewmodel)
            // Allows using 'viewModel()' or 'koinViewModel()' in Composables
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            // Allows observing state flows safely in the UI
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Core DI functionality
            implementation(libs.koin.core)
            // Essential for using koinViewModel() in App.kt
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation("org.jetbrains.compose.components:components-resources:1.6.11") // check for latest version
            implementation("org.jetbrains.compose.material:material-icons-extended:1.6.11")
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
            modules("jdk.unsupported", "jdk.unsupported.desktop")
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