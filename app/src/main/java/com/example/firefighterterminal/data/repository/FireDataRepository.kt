package com.example.firefighterterminal.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.firefighterterminal.data.ble.BleManagerRepository
import com.example.firefighterterminal.data.ble.FireDataParser
import com.example.firefighterterminal.data.ble.FireMessage
import com.example.firefighterterminal.domain.model.Position

data class FireDataSnapshot(
    val fires: List<Position> = emptyList(),
    val directions: Map<Int, Int> = emptyMap(),
    val sensorStates: List<Boolean> = emptyList(),
    val voiceMode: VoiceModeState? = null,
    val fireCount: Int = 0,
    val uptime: Long = 0,
    val lightConfigs: Map<Int, LightConfigInfo>? = null,
    val mapConfig: FireMessage.MapConfig? = null
)

data class VoiceModeState(val mode: Int, val modeName: String, val hasTrapped: Boolean)
data class LightConfigInfo(val x: Int, val y: Int, val type: String)

/**
 * 火场数据中心 (单例)
 *
 * 桥接 BLE 原始数据 → JSON 解析 → 全局状态
 */
object FireDataRepository {

    private val _mapConfig = MutableLiveData<FireMessage.MapConfig?>()
    val mapConfig: LiveData<FireMessage.MapConfig?> = _mapConfig

    private val _fireData = MutableLiveData<FireDataSnapshot?>()
    val fireData: LiveData<FireDataSnapshot?> = _fireData

    private var currentSnapshot = FireDataSnapshot()
    private var lightConfigData: MutableMap<Int, LightConfigInfo> = mutableMapOf()
    private val parser = FireDataParser()
    private var initialized = false

    /** 注册 BLE 数据回调，桥接 原始数据 → 解析 → 状态更新 */
    fun initialize() {
        if (initialized) return
        initialized = true

        BleManagerRepository.registerDataCallback { rawJson ->
            val msg = parser.parse(rawJson)
            android.util.Log.d("FireRepo", "收到: ${rawJson.take(80)} → ${msg::class.simpleName}")
            processMessage(msg)
        }
    }

    fun processMessage(msg: FireMessage) {
        when (msg) {
            is FireMessage.MapConfig -> {
                _mapConfig.postValue(msg)
                updateSnapshot { copy(mapConfig = msg) }
            }
            is FireMessage.LightConfig -> {
                lightConfigData.clear()
                msg.lights.forEach { light ->
                    lightConfigData[light.id] = LightConfigInfo(light.x, light.y, light.type)
                }
                updateSnapshot { copy(lightConfigs = lightConfigData.toMap()) }
            }
            is FireMessage.FireUpdate -> {
                updateSnapshot { copy(fires = msg.fires, fireCount = msg.fires.size) }
            }
            is FireMessage.DirectionUpdate -> {
                updateSnapshot { copy(directions = msg.directions) }
            }
            is FireMessage.VoiceMode -> {
                updateSnapshot { copy(voiceMode = VoiceModeState(msg.mode, msg.modeName, msg.hasTrapped)) }
            }
            is FireMessage.SensorState -> {
                updateSnapshot { copy(sensorStates = msg.states) }
            }
            is FireMessage.Heartbeat -> {
                updateSnapshot { copy(uptime = msg.uptime, fireCount = msg.fireCount) }
            }
            is FireMessage.Unknown -> { /* ignore */ }
        }
    }

    private fun updateSnapshot(transform: FireDataSnapshot.() -> FireDataSnapshot) {
        currentSnapshot = currentSnapshot.transform()
        _fireData.postValue(currentSnapshot)
    }

    fun cleanup() {
        currentSnapshot = FireDataSnapshot()
        initialized = false
    }
}
