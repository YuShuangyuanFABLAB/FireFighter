package com.example.firefighterterminal.data.ble

import com.example.firefighterterminal.domain.model.Position
import kotlinx.serialization.json.*

/**
 * 火场数据 JSON 解析器
 *
 * 解析 ESP32 通过 BLE Notify 推送的 JSON 消息。
 * 所有消息以 {"type": "..."} 字段区分类型。
 */
class FireDataParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 解析 JSON 字符串为 FireMessage
     *
     * @param raw 原始 JSON 字符串
     * @return 对应的 FireMessage 子类，解析失败返回 FireMessage.Unknown
     */
    fun parse(raw: String): FireMessage {
        return try {
            val element = json.parseToJsonElement(raw.trim())
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return FireMessage.Unknown(raw)

            when (type) {
                "MAP_CONFIG" -> parseMapConfig(obj)
                "LIGHT_CONFIG" -> parseLightConfig(obj)
                "FIRE_UPDATE" -> parseFireUpdate(obj)
                "DIRECTION_UPDATE" -> parseDirectionUpdate(obj)
                "VOICE_MODE" -> parseVoiceMode(obj)
                "SENSOR_STATE" -> parseSensorState(obj)
                "HEARTBEAT" -> parseHeartbeat(obj)
                else -> FireMessage.Unknown(raw)
            }
        } catch (e: Exception) {
            FireMessage.Unknown(raw)
        }
    }

    private fun parseMapConfig(obj: JsonObject): FireMessage.MapConfig {
        val width = obj["width"]?.jsonPrimitive?.int ?: 0
        val height = obj["height"]?.jsonPrimitive?.int ?: 0

        val colWidths = obj["colWidths"]?.jsonArray?.map {
            it.jsonPrimitive.float
        } ?: emptyList()

        val rowHeights = obj["rowHeights"]?.jsonArray?.map {
            it.jsonPrimitive.float
        } ?: emptyList()

        val walls = obj["walls"]?.jsonArray?.map { wall ->
            val arr = wall.jsonArray
            Position(arr[0].jsonPrimitive.int, arr[1].jsonPrimitive.int)
        } ?: emptyList()

        val exits = obj["exits"]?.jsonArray?.map { exit ->
            val arr = exit.jsonArray
            Position(arr[0].jsonPrimitive.int, arr[1].jsonPrimitive.int)
        } ?: emptyList()

        return FireMessage.MapConfig(width, height, colWidths, rowHeights, walls, exits)
    }

    private fun parseLightConfig(obj: JsonObject): FireMessage.LightConfig {
        val lights = obj["lights"]?.jsonArray?.map { light ->
            val lightObj = light.jsonObject
            FireMessage.LightInfo(
                id = lightObj["id"]?.jsonPrimitive?.int ?: 0,
                x = lightObj["x"]?.jsonPrimitive?.int ?: 0,
                y = lightObj["y"]?.jsonPrimitive?.int ?: 0,
                type = lightObj["type"]?.jsonPrimitive?.content ?: ""
            )
        } ?: emptyList()

        return FireMessage.LightConfig(lights)
    }

    private fun parseFireUpdate(obj: JsonObject): FireMessage.FireUpdate {
        val fires = obj["fires"]?.jsonArray?.map { fire ->
            val arr = fire.jsonArray
            Position(arr[0].jsonPrimitive.int, arr[1].jsonPrimitive.int)
        } ?: emptyList()

        return FireMessage.FireUpdate(fires)
    }

    private fun parseDirectionUpdate(obj: JsonObject): FireMessage.DirectionUpdate {
        val directionsObj = obj["directions"]?.jsonObject ?: JsonObject(emptyMap())
        val directions = directionsObj.entries.associate { (key, value) ->
            key.toInt() to value.jsonPrimitive.int
        }

        return FireMessage.DirectionUpdate(directions)
    }

    private fun parseVoiceMode(obj: JsonObject): FireMessage.VoiceMode {
        return FireMessage.VoiceMode(
            mode = obj["mode"]?.jsonPrimitive?.int ?: 0,
            modeName = obj["modeName"]?.jsonPrimitive?.content ?: "IDLE",
            hasTrapped = obj["hasTrapped"]?.jsonPrimitive?.boolean ?: false
        )
    }

    private fun parseSensorState(obj: JsonObject): FireMessage.SensorState {
        val states = obj["states"]?.jsonArray?.map {
            it.jsonPrimitive.boolean
        } ?: emptyList()

        return FireMessage.SensorState(states)
    }

    private fun parseHeartbeat(obj: JsonObject): FireMessage.Heartbeat {
        return FireMessage.Heartbeat(
            uptime = obj["uptime"]?.jsonPrimitive?.long ?: 0,
            fireCount = obj["fireCount"]?.jsonPrimitive?.int ?: 0
        )
    }
}
