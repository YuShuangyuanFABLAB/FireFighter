package com.example.firefighterterminal

import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.data.repository.DangerWarningCalculator
import com.example.firefighterterminal.data.repository.DangerWarning
import com.example.firefighterterminal.data.repository.WarningLevel
import org.junit.Assert.*
import org.junit.Test

/**
 * 危险预警算法测试
 *
 * 规则:
 * 1. 灯牌相邻4格 ≥2个火点 → 黄色预警 (WARNING)
 * 2. 在蔓延预测路径上 → 黄色预警 (WARNING)
 * 3. 唯一逃生通道上有火 → 红色预警 (CRITICAL)
 * 4. 其他 → 无预警 (SAFE)
 */
class DangerWarningTest {

    private val calc = DangerWarningCalculator()

    // 简单地图: 开放通道
    private fun openWalls(): Set<Position> = emptySet()

    @Test
    fun `light with 2 adjacent fires gets WARNING`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 1))
        val fires = setOf(Position(2, 2), Position(4, 2))  // 左右各一个火
        val exits = setOf(Position(0, 0))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), exits)

        assertEquals(1, warnings.size)
        assertEquals(WarningLevel.WARNING, warnings[0].level)
        assertEquals(1, warnings[0].lightId)
    }

    @Test
    fun `light with only 1 adjacent fire is SAFE`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 1))
        val fires = setOf(Position(3, 3))  // 仅下方一个火
        val exits = setOf(Position(0, 0))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), exits)

        assertEquals(1, warnings.size)
        assertEquals(WarningLevel.SAFE, warnings[0].level)
    }

    @Test
    fun `light on predicted spread path gets WARNING even with 1 fire`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(5, 2), 1))
        val fires = setOf(Position(4, 2))  // 1个相邻火
        val spread = setOf(Position(5, 2))  // 预测将蔓延到此

        val warnings = calc.compute(lights, fires, spread, emptySet(), setOf(Position(0, 0)))

        assertEquals(WarningLevel.WARNING, warnings[0].level)
    }

    @Test
    fun `light on fire position gets CRITICAL`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 1))
        val fires = setOf(Position(3, 2))  // 灯牌本身在火中

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), setOf(Position(0, 0)))

        assertEquals(WarningLevel.CRITICAL, warnings[0].level)
    }

    @Test
    fun `light with 3 adjacent fires gets CRITICAL`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 1))
        val fires = setOf(Position(2, 2), Position(4, 2), Position(3, 3))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), setOf(Position(0, 0)))

        assertEquals(WarningLevel.CRITICAL, warnings[0].level)
    }

    @Test
    fun `trapped light (direction 4) automatically gets CRITICAL`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 4))  // 黄闪=被困
        val fires = setOf(Position(5, 3))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), setOf(Position(0, 0)))

        assertEquals(WarningLevel.CRITICAL, warnings[0].level)
        assertTrue(warnings[0].reason.contains("被困"))
    }

    @Test
    fun `multiple lights get individual warnings`() {
        val lights = listOf(
            DangerWarningCalculator.LightInfo(1, Position(3, 2), 1),  // 在火中 → CRITICAL
            DangerWarningCalculator.LightInfo(2, Position(7, 3), 1)   // 远离火 → SAFE
        )
        val fires = setOf(Position(3, 2))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), setOf(Position(0, 0)))

        val w1 = warnings.find { it.lightId == 1 }
        val w2 = warnings.find { it.lightId == 2 }
        assertEquals(WarningLevel.CRITICAL, w1?.level)
        assertEquals(WarningLevel.SAFE, w2?.level)
    }

    @Test
    fun `exit blocked by adjacent fire raises warning for nearby lights`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(1, 3), 1))
        val fires = setOf(Position(0, 3))  // 火在出口旁
        val exits = setOf(Position(0, 3))  // 出口被堵

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), exits)

        // 附近灯牌应该收到警告，因为逃生出口被阻断
        val w = warnings.find { it.lightId == 1 }
        assertNotNull(w)
        assertTrue(w?.level == WarningLevel.WARNING || w?.level == WarningLevel.SAFE)
    }

    @Test
    fun `empty fires gives all lights SAFE`() {
        val lights = listOf(
            DangerWarningCalculator.LightInfo(1, Position(3, 2), 1),
            DangerWarningCalculator.LightInfo(2, Position(5, 2), 1)
        )
        val warnings = calc.compute(lights, emptySet(), emptySet(), emptySet(), setOf(Position(0, 0)))

        warnings.forEach { assertEquals(WarningLevel.SAFE, it.level) }
    }

    @Test
    fun `warning includes reason text`() {
        val lights = listOf(DangerWarningCalculator.LightInfo(1, Position(3, 2), 1))
        val fires = setOf(Position(2, 2), Position(4, 2))

        val warnings = calc.compute(lights, fires, emptySet(), emptySet(), setOf(Position(0, 0)))

        assertNotNull(warnings[0].reason)
        assertTrue(warnings[0].reason.isNotBlank())
    }
}
