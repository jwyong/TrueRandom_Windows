package com.truerandom.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

object PlaybackManager {
    val playbackEndCheckScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    var playbackEndCheckJob: Job? = null
}