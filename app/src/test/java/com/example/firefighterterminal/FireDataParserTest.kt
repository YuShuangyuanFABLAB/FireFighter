package com.example.firefighterterminal

import com.example.firefighterterminal.data.ble.FireDataParser
import com.example.firefighterterminal.data.ble.FireMessage
import org.junit.Assert.*
import org.junit.Test

/**
 * FireDataParser JSON 消息解析测试
 *
 * 验证 ESP32 → APP 所有 7 种消息类型的解析
 */
class FireDataParserTest {

    private val parser = FireDataParser()

    // ==================== MAP_CONFIG ====================

    @Test
    fun `parse MAP_CONFIG message with walls and exits`() {
        val json = """{"type":"MAP_CONFIG","width":10,"height":5,"colWidths":[60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0],"rowHeights":[80.0,60.0,80.0,60.0,80.0],"walls":[[0,0],[1,0],[2,0]],"exits":[[5,0],[0,3],[9,3],[3,4]]}"""

        val msg = parser.parse(json)

        assertTrue(msg is FireMessage.MapConfig)
        val config = msg as FireMessage.MapConfig
        assertEquals(10, config.width)
        assertEquals(5, config.height)
        assertEquals(10, config.colWidths.size)
        assertEquals(60.0f, config.colWidths[0])
        assertEquals(80.0f, config.colWidths[1])
        assertEquals(5, config.rowHeights.size)
        assertEquals(3, config.walls.size)
        assertEquals(4, config.exits.size)
    }

    @Test
    fun `parse MAP_CONFIG walls include specific coordinates`() {
        val json = """{"type":"MAP_CONFIG","width":10,"height":5,"colWidths":[60.0],"rowHeights":[80.0],"walls":[[0,0],[9,4]],"exits":[[5,0]]}"""

        val msg = parser.parse(json) as FireMessage.MapConfig
        assertEquals(0, msg.walls[0].x)
        assertEquals(0, msg.walls[0].y)
        assertEquals(9, msg.walls[1].x)
        assertEquals(4, msg.walls[1].y)
    }

    // ==================== LIGHT_CONFIG ====================

    @Test
    fun `parse LIGHT_CONFIG message with all 13 lights`() {
        val json = """{"type":"LIGHT_CONFIG","lights":[{"id":1,"x":0,"y":3,"type":"HORIZONTAL_UP"},{"id":2,"x":2,"y":3,"type":"HORIZONTAL_UP"},{"id":13,"x":2,"y":1,"type":"HORIZONTAL_DOWN"}]}"""

        val msg = parser.parse(json)

        assertTrue(msg is FireMessage.LightConfig)
        val config = msg as FireMessage.LightConfig
        assertEquals(3, config.lights.size)
        assertEquals(1, config.lights[0].id)
        assertEquals("HORIZONTAL_UP", config.lights[0].type)
        assertEquals(13, config.lights[2].id)
        assertEquals("HORIZONTAL_DOWN", config.lights[2].type)
    }

    // ==================== FIRE_UPDATE ====================

    @Test
    fun `parse FIRE_UPDATE with two fire points`() {
        val json = """{"type":"FIRE_UPDATE","fires":[[3,2],[5,3]]}"""

        val msg = parser.parse(json)

        assertTrue(msg is FireMessage.FireUpdate)
        val update = msg as FireMessage.FireUpdate
        assertEquals(2, update.fires.size)
        assertEquals(3, update.fires[0].x)
        assertEquals(2, update.fires[0].y)
        assertEquals(5, update.fires[1].x)
        assertEquals(3, update.fires[1].y)
    }

    @Test
    fun `parse FIRE_UPDATE with empty fires array`() {
        val json = """{"type":"FIRE_UPDATE","fires":[]}"""

        val msg = parser.parse(json) as FireMessage.FireUpdate
        assertEquals(0, msg.fires.size)
    }

    // ==================== DIRECTION_UPDATE ====================

    @Test
    fun `parse DIRECTION_UPDATE with all direction values`() {
        val json = """{"type":"DIRECTION_UPDATE","directions":{"1":1,"2":2,"3":4,"4":1,"5":2,"6":3,"7":1,"8":1,"9":2,"10":1,"11":2,"12":1,"13":2}}"""

        val msg = parser.parse(json)

        assertTrue(msg is FireMessage.DirectionUpdate)
        val update = msg as FireMessage.DirectionUpdate
        assertEquals(13, update.directions.size)
        assertEquals(1, update.directions[1])
        assertEquals(2, update.directions[2])
        assertEquals(4, update.directions[3])  // DIR_NO_PATH — 被困!
        assertEquals(3, update.directions[6])  // DIR_AT_EXIT
    }

    // ==================== VOICE_MODE ====================

    @Test
    fun `parse VOICE_MODE idle`() {
        val json = """{"type":"VOICE_MODE","mode":0,"modeName":"IDLE","hasTrapped":false}"""
        val msg = parser.parse(json) as FireMessage.VoiceMode

        assertEquals(0, msg.mode)
        assertEquals("IDLE", msg.modeName)
        assertFalse(msg.hasTrapped)
    }

    @Test
    fun `parse VOICE_MODE escape only`() {
        val json = """{"type":"VOICE_MODE","mode":1,"modeName":"ESCAPE","hasTrapped":false}"""
        val msg = parser.parse(json) as FireMessage.VoiceMode

        assertEquals(1, msg.mode)
        assertEquals("ESCAPE", msg.modeName)
        assertFalse(msg.hasTrapped)
    }

    @Test
    fun `parse VOICE_MODE rescue with trapped areas`() {
        val json = """{"type":"VOICE_MODE","mode":2,"modeName":"RESCUE","hasTrapped":true}"""
        val msg = parser.parse(json) as FireMessage.VoiceMode

        assertEquals(2, msg.mode)
        assertEquals("RESCUE", msg.modeName)
        assertTrue(msg.hasTrapped)
    }

    // ==================== SENSOR_STATE ====================

    @Test
    fun `parse SENSOR_STATE with all 13 sensors`() {
        val json = """{"type":"SENSOR_STATE","states":[false,false,true,false,true,false,false,false,false,false,false,false,false]}"""

        val msg = parser.parse(json) as FireMessage.SensorState
        assertEquals(13, msg.states.size)
        assertFalse(msg.states[0])
        assertTrue(msg.states[2])
        assertTrue(msg.states[4])
    }

    // ==================== HEARTBEAT ====================

    @Test
    fun `parse HEARTBEAT message`() {
        val json = """{"type":"HEARTBEAT","uptime":123456,"fireCount":2}"""

        val msg = parser.parse(json) as FireMessage.Heartbeat
        assertEquals(123456L, msg.uptime)
        assertEquals(2, msg.fireCount)
    }

    // ==================== Error handling ====================

    @Test
    fun `parse returns Unknown for invalid JSON`() {
        val msg = parser.parse("not json at all")
        assertTrue(msg is FireMessage.Unknown)
    }

    @Test
    fun `parse returns Unknown for missing type field`() {
        val json = """{"someField":"value"}"""
        val msg = parser.parse(json)
        assertTrue(msg is FireMessage.Unknown)
    }

    @Test
    fun `parse returns Unknown for unknown message type`() {
        val json = """{"type":"UNKNOWN_TYPE","data":"test"}"""
        val msg = parser.parse(json)
        assertTrue(msg is FireMessage.Unknown)
    }
}
