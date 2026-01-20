package com.truerandom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.truerandom.di.appModule
import com.truerandom.di.platformModule
import com.truerandom.ui.main.MainViewModel
import org.koin.compose.koinInject
import org.koin.core.context.GlobalContext.startKoin
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.system.exitProcess

val wakeUpSignal = mutableStateOf(0)
fun main() {
    val port = 58293

    // 1. Try to start the "Server" (Instance #1)
    val serverSocket = try {
        ServerSocket(port, 0, InetAddress.getByName("127.0.0.1"))
    } catch (e: Exception) {
        // 2. If it fails, we are Instance #2. Send signal and EXIT.
        try {
            Socket("127.0.0.1", port).use { it.getOutputStream().write(1) }
        } catch (err: Exception) { /* Server might be closing */ }
        exitProcess(0)
    }

    // 3. If we got here, we are Instance #1. Listen for "Wake up" signals in a background thread.
    thread(isDaemon = true) {
        while (true) {
            try {
                serverSocket.accept().use {
                    // When a connection is received, increment the signal
                    wakeUpSignal.value++
                }
            } catch (e: Exception) { break }
        }
    }

    startKoin { modules(appModule, platformModule) }

    application {
        val mainViewModel = koinInject<MainViewModel>()
        var isVisible by remember { mutableStateOf(true) }

        // 4. Listen for the signal change to show the window
        LaunchedEffect(wakeUpSignal.value) {
            if (wakeUpSignal.value > 0) {
                isVisible = true
            }
        }

        if (isVisible) {
            Window(onCloseRequest = { isVisible = false }, title = "TrueRandom") {
                App(mainViewModel)
            }
        }

        Tray(
            icon = rememberVectorPainter(Icons.Default.MusicNote),
            tooltip = "TrueRandom",
            onAction = { isVisible = true },
            menu = {
                Item(
                    "Show TrueRandom",
                    onClick = { isVisible = true },
                )
                Separator()
                Item(
                    "Exit App",
                    onClick = { exitApplication() },
                )
            }
        )
    }
}