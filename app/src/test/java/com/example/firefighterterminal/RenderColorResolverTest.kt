package com.example.firefighterterminal

import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.presentation.ui.map.view.RenderColorResolver
import com.example.firefighterterminal.presentation.ui.map.view.CellType
import org.junit.Assert.*
import org.junit.Test

/**
 * 渲染颜色解析器测试
 *
 * 地图格子和灯牌的颜色由当前状态决定
 */
class RenderColorResolverTest {

    private val resolver = RenderColorResolver()

    // ==================== 格子类型判断 ====================

    @Test
    fun `exit cell with no fire nearby is AVAILABLE`() {
        val walls = setOf(Position(0, 0))
        val exits = setOf(Position(5, 0))
        val fires = emptySet<Position>()

        val cellType = resolver.resolveCellType(5, 0, walls, exits, fires)
        assertEquals(CellType.EXIT_AVAILABLE, cellType)
    }

    @Test
    fun `exit cell with adjacent fire is BLOCKED`() {
        val walls = setOf<Position>()
        val exits = setOf(Position(5, 0))
        val fires = setOf(Position(5, 1))  // 出口下方着火

        val cellType = resolver.resolveCellType(5, 0, walls, exits, fires)
        assertEquals(CellType.EXIT_BLOCKED, cellType)
    }

    @Test
    fun `wall cell is WALL`() {
        val walls = setOf(Position(0, 0))
        val exits = emptySet<Position>()
        val fires = emptySet<Position>()

        val cellType = resolver.resolveCellType(0, 0, walls, exits, fires)
        assertEquals(CellType.WALL, cellType)
    }

    @Test
    fun `floor cell is FLOOR`() {
        val walls = emptySet<Position>()
        val exits = emptySet<Position>()
        val fires = emptySet<Position>()

        val cellType = resolver.resolveCellType(3, 2, walls, exits, fires)
        assertEquals(CellType.FLOOR, cellType)
    }

    @Test
    fun `floor cell on fire is FIRE`() {
        val walls = emptySet<Position>()
        val exits = emptySet<Position>()
        val fires = setOf(Position(3, 2))

        val cellType = resolver.resolveCellType(3, 2, walls, exits, fires)
        assertEquals(CellType.FIRE, cellType)
    }

    // ==================== 灯牌颜色解析 ====================

    @Test
    fun `light with direction 1 and hasFire returns flash green if flashOn`() {
        val color = resolver.resolveLightColor(direction = 1, hasFire = true, flashOn = true)
        assertEquals(0xFF00FF88.toInt(), color)
    }

    @Test
    fun `light with direction 1 and hasFire returns off if flashOff`() {
        val color = resolver.resolveLightColor(direction = 1, hasFire = true, flashOn = false)
        assertEquals(0x00000000, color)
    }

    @Test
    fun `light with direction 1 and noFire returns solid green`() {
        val color = resolver.resolveLightColor(direction = 1, hasFire = false, flashOn = true)
        assertEquals(0xFF00FF88.toInt(), color)
    }

    @Test
    fun `light with direction 4 trapped returns yellow if flashOn`() {
        val color = resolver.resolveLightColor(direction = 4, hasFire = true, flashOn = true)
        assertEquals(0xFFFFD700.toInt(), color)
    }

    @Test
    fun `light with direction 4 trapped returns off if flashOff`() {
        val color = resolver.resolveLightColor(direction = 4, hasFire = true, flashOn = false)
        assertEquals(0x00000000, color)
    }

    @Test
    fun `light with direction 3 exit returns double green if flashOn`() {
        val color = resolver.resolveLightColor(direction = 3, hasFire = true, flashOn = true)
        assertEquals(0xFF00FF88.toInt(), color)
    }

    @Test
    fun `light with direction 0 off always returns transparent`() {
        assertEquals(0x00000000, resolver.resolveLightColor(0, true, true))
        assertEquals(0x00000000, resolver.resolveLightColor(0, false, false))
    }

    // ==================== 火焰渐变 ====================

    @Test
    fun `fire gradient colors are red-orange range`() {
        val colors = resolver.getFireGlowColors()
        assertTrue(colors.isNotEmpty())
        // 中心应为亮红/橙
        val centerColor = colors.first()
        val red = (centerColor shr 16) and 0xFF
        val green = (centerColor shr 8) and 0xFF
        assertTrue(red > 200)
        assertTrue(green < 150)
    }

    @Test
    fun `fire gradient has transparent end`() {
        val colors = resolver.getFireGlowColors()
        val lastColor = colors.last()
        val alpha = (lastColor shr 24) and 0xFF
        assertEquals(0, alpha)
    }

    // ==================== 被困区域 ====================

    @Test
    fun `trapped wave color uses yellow-gold`() {
        val colors = resolver.getTrappedWaveColors()
        assertTrue(colors.isNotEmpty())
        val centerColor = colors.first()
        val alpha = (centerColor shr 24) and 0xFF
        assertTrue(alpha > 100)
    }
}
