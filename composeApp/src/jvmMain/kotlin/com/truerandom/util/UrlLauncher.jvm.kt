package com.truerandom.util

import java.awt.Desktop
import java.net.URI

actual fun openUrlInBrowser(url: String) {
    if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(URI(url))
    }
}
