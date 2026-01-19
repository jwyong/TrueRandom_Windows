package com.truerandom.util

import kotlinx.serialization.json.Json

object JsonUtil {
    val jsonObj by lazy { Json { ignoreUnknownKeys = true } }
}