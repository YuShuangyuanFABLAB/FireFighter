package com.example.firefighterterminal

import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.domain.model.RescuePriority
import com.example.firefighterterminal.domain.model.PriorityLevel
import com.example.firefighterterminal.data.repository.RescuePriorityCalculator
import org.junit.Assert.*
import org.junit.Test

/**
 * 救援优先级算法测试
 *
 * 验证 Spec §7.1 的优先级排序逻辑：
 * - 黄闪灯牌(方向=4) → P0 立即救援
 * - 离火点近 → 高分
 * - 在蔓延路径上 → 加分
 */
class RescuePriorityCalculatorTest {

    private val calculator = RescuePriorityCalculator()

    // 标准 10x5 地图墙壁（简单场景：全通道，仅边缘为墙）
    private fun defaultWalls(): List<Position> {
        val walls = mutableListOf<Position>()
        for (x in 0 until 10) {
            walls.add(Position(x, 0))
            walls.add(Position(x, 4))
        }
        for (y in 1 until 4) {
            walls.add(Position(0, y))
            walls.add(Position(9, y))
        }
        return walls
    }

    @Test
    fun `trapped light (direction 4) gets highest priority P0`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(3, 2), 4),  // 黄闪
            RescuePriorityCalculator.LightState(2, Position(5, 2), 1),  // 正常
            RescuePriorityCalculator.LightState(3, Position(7, 2), 2)   // 正常
        )
        val fires = listOf(Position(3, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        val trappedLight = priorities.find { it.lightId == 1 }
        assertNotNull(trappedLight)
        assertEquals(PriorityLevel.P0, trappedLight?.level)
        assertTrue(trappedLight!!.score >= 100)
    }

    @Test
    fun `light far from fire gets lower priority`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(1, 1), 1),  // 离火远
            RescuePriorityCalculator.LightState(2, Position(5, 1), 1)   // 离火近
        )
        val fires = listOf(Position(5, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        val farLight = priorities.find { it.lightId == 1 }
        val nearLight = priorities.find { it.lightId == 2 }
        assertNotNull(farLight)
        assertNotNull(nearLight)
        assertTrue(nearLight!!.score > farLight!!.score)
    }

    @Test
    fun `multiple fires affect priority scoring`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(3, 2), 1)  // 附近 2 个火点
        )
        val fires = listOf(Position(3, 1), Position(3, 3))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        assertEquals(1, priorities.size)
        assertTrue(priorities[0].score > 0)
    }

    @Test
    fun `light on fire position scores higher than light far away`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(4, 2), 1),
            RescuePriorityCalculator.LightState(2, Position(8, 3), 1)
        )
        val fires = listOf(Position(4, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        assertTrue(priorities[0].score > priorities[1].score)
    }

    @Test
    fun `result is sorted by score descending`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(8, 3), 1),
            RescuePriorityCalculator.LightState(2, Position(1, 1), 1),
            RescuePriorityCalculator.LightState(3, Position(4, 2), 4)  // 黄闪
        )
        val fires = listOf(Position(4, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        // 排序验证：score[i] >= score[i+1]
        for (i in 0 until priorities.size - 1) {
            assertTrue(
                "priorities[$i].score(${priorities[i].score}) >= priorities[${i + 1}].score(${priorities[i + 1].score})",
                priorities[i].score >= priorities[i + 1].score
            )
        }
    }

    @Test
    fun `P0 is assigned only for score 100 or above`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(3, 2), 4)  // should be P0
        )
        val fires = listOf(Position(3, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        assertEquals(PriorityLevel.P0, priorities[0].level)
        assertTrue(priorities[0].score >= 100)
    }

    @Test
    fun `P3 assigned when score is below 20`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(8, 1), 1)  // far from fire
        )
        val fires = listOf(Position(1, 3))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        assertEquals(PriorityLevel.P3, priorities[0].level)
    }

    @Test
    fun `empty fires gives all lights low priority`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(3, 2), 1),
            RescuePriorityCalculator.LightState(2, Position(5, 2), 1)
        )
        val fires = emptyList<Position>()

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        priorities.forEach {
            assertTrue(it.score >= 0)
            assertEquals(PriorityLevel.P3, it.level)
        }
    }

    @Test
    fun `each priority has a reason description`() {
        val lights = listOf(
            RescuePriorityCalculator.LightState(1, Position(3, 2), 4),
            RescuePriorityCalculator.LightState(2, Position(5, 2), 1)
        )
        val fires = listOf(Position(3, 2))

        val priorities = calculator.compute(lights, fires, defaultWalls(), emptyList())

        priorities.forEach {
            assertNotNull(it.reason)
            assertTrue(it.reason.isNotBlank())
        }
    }
}
