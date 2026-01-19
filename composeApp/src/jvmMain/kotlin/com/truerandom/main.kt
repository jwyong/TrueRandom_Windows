package com.truerandom

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.truerandom.di.appModule
import com.truerandom.di.platformModule
import org.koin.compose.KoinApplication

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "TrueRandom") {
        // 1. Initialize Koin for the Compose Context
        KoinApplication(application = {
            modules(appModule, platformModule) // Pass all your modules here
        }) {
            // 2. Now call your App() - it can now safely use koinViewModel()
            App()
        }
    }
}