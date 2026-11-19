package com.truerandom.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.mp.KoinPlatform

actual fun openUrlInBrowser(url: String) {
    val context: Context = KoinPlatform.getKoin().get()
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
