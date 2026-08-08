package com.example.firefighterterminal.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceConfig(
    val appConfig: AppConfig = AppConfig(),
    val bleConfig: BleConfig = BleConfig(),
    val mapLayout: MapLayoutConfig = MapLayoutConfig()
)

@Serializable
data class AppConfig(
    val appName: String = "消防员终端",
    val scanDuration: Long = 10000,
    val autoReconnect: Boolean = true,
    val reconnectInterval: Long = 5000,
    val maxReconnectAttempts: Int = 3
)

@Serializable
data class BleConfig(
    val services: List<BleServiceConfig> = emptyList(),
    val deviceFilters: List<DeviceFilter> = emptyList()
)

@Serializable
data class BleServiceConfig(
    val uuid: String,
    val characteristics: List<BleCharacteristicConfig> = emptyList()
)

@Serializable
data class BleCharacteristicConfig(
    val uuid: String,
    val type: String,
    val dataFormat: String = "CUSTOM"
)

@Serializable
data class DeviceFilter(
    val namePattern: String,
    val required: Boolean = true
)

@Serializable
data class MapLayoutConfig(
    val cols: Int = 10,
    val rows: Int = 5,
    val colWidths: List<Float> = listOf(60f, 80f, 60f, 80f, 60f, 80f, 60f, 80f, 60f, 80f),
    val rowHeights: List<Float> = listOf(80f, 60f, 80f, 60f, 80f),
    val wallColor: String = "#3d3d5c",
    val floorColor: String = "#252540",
    val gridLineColor: String = "#2a2a45",
    val padding: Float = 12f
)
