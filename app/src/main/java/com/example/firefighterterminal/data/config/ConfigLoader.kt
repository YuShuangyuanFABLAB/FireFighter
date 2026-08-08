package com.example.firefighterterminal.data.config

import android.content.Context
import com.example.firefighterterminal.domain.model.DeviceConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.IOException

class ConfigLoader(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadDeviceConfig(fileName: String = "device_config.json"): DeviceConfig {
        return context.assets.open("config/$fileName").use { inputStream ->
            json.decodeFromStream<DeviceConfig>(inputStream)
        }
    }

    fun loadDeviceConfigSafely(fileName: String = "device_config.json"): DeviceConfig? {
        return try {
            loadDeviceConfig(fileName)
        } catch (e: Exception) {
            null
        }
    }
}
