package com.example.firefighterterminal.data.ble

import com.example.firefighterterminal.domain.model.Position

/**
 * BLE 接收的 JSON 消息类型
 *
 * ESP32 推送的所有消息类型的密封类封装
 */
sealed class FireMessage {

    /** 地图静态配置 */
    data class MapConfig(
        val width: Int,
        val height: Int,
        val colWidths: List<Float>,
        val rowHeights: List<Float>,
        val walls: List<Position>,
        val exits: List<Position>
    ) : FireMessage()

    /** 灯牌静态配置 */
    data class LightConfig(
        val lights: List<LightInfo>
    ) : FireMessage()

    /** 灯牌信息 */
    data class LightInfo(
        val id: Int,
        val x: Int,
        val y: Int,
        val type: String  // HORIZONTAL_UP/DOWN, VERTICAL_LEFT/RIGHT
    )

    /** 火灾点更新 */
    data class FireUpdate(
        val fires: List<Position>
    ) : FireMessage()

    /** 灯牌方向更新 */
    data class DirectionUpdate(
        val directions: Map<Int, Int>  // 灯牌ID → 方向值(0-4)
    ) : FireMessage()

    /** 语音模式 */
    data class VoiceMode(
        val mode: Int,
        val modeName: String,
        val hasTrapped: Boolean
    ) : FireMessage()

    /** 传感器原始状态 */
    data class SensorState(
        val states: List<Boolean>
    ) : FireMessage()

    /** 系统心跳 */
    data class Heartbeat(
        val uptime: Long,
        val fireCount: Int
    ) : FireMessage()

    /** 无法解析的消息 */
    data class Unknown(
        val raw: String
    ) : FireMessage()
}
