package com.example.firefighterterminal.presentation.ui.map.view

import androidx.annotation.ColorInt
import com.example.firefighterterminal.domain.model.Position

/**
 * 格子类型
 */
enum class CellType {
    WALL,            // 墙壁
    FLOOR,           // 通道
    EXIT_AVAILABLE,  // 可用出口
    EXIT_BLOCKED,    // 被火阻断的出口
    FIRE             // 火灾点
}

/**
 * 渲染颜色解析器
 *
 * 根据火场状态确定地图各元素的绘制颜色。
 */
class RenderColorResolver {

    companion object {
        // 基础颜色
        @ColorInt const val COLOR_WALL = 0xFF3d3d5c.toInt()
        @ColorInt const val COLOR_FLOOR = 0xFF252540.toInt()
        @ColorInt const val COLOR_EXIT_AVAILABLE = 0xFF00FF88.toInt()
        @ColorInt const val COLOR_EXIT_BLOCKED = 0xFFF44336.toInt()
        @ColorInt const val COLOR_FIRE_GLOW_CENTER = 0xFFFF4500.toInt()
        @ColorInt const val COLOR_TRAPPED_WAVE = 0xFFFFD700.toInt()
        @ColorInt const val COLOR_LIGHT_GREEN = 0xFF00FF88.toInt()
        @ColorInt const val COLOR_LIGHT_YELLOW = 0xFFFFD700.toInt()
        @ColorInt const val COLOR_OFF = 0x00000000
        @ColorInt const val COLOR_PATH_BLUE = 0xFF4ecdc4.toInt()
    }

    /**
     * 判断格子类型
     */
    fun resolveCellType(
        x: Int, y: Int,
        walls: Set<Position>,
        exits: Set<Position>,
        fires: Set<Position>
    ): CellType {
        val pos = Position(x, y)

        if (fires.contains(pos)) return CellType.FIRE

        if (exits.contains(pos)) {
            val hasAdjacentFire = fires.any { f ->
                kotlin.math.abs(f.x - x) + kotlin.math.abs(f.y - y) == 1
            }
            return if (hasAdjacentFire) CellType.EXIT_BLOCKED else CellType.EXIT_AVAILABLE
        }

        if (walls.contains(pos)) return CellType.WALL

        return CellType.FLOOR
    }

    /**
     * 解析灯牌颜色（闪烁逻辑）
     */
    @ColorInt
    fun resolveLightColor(direction: Int, hasFire: Boolean, flashOn: Boolean): Int {
        if (direction == 0) return COLOR_OFF

        // 黄闪 — 被困
        if (direction == 4) {
            return if (flashOn) COLOR_LIGHT_YELLOW else COLOR_OFF
        }

        // 双绿 — 到达出口
        if (direction == 3) {
            return if (hasFire && !flashOn) COLOR_OFF else COLOR_LIGHT_GREEN
        }

        // 方向灯 (1 或 2)
        if (hasFire) {
            return if (flashOn) COLOR_LIGHT_GREEN else COLOR_OFF
        }
        return COLOR_LIGHT_GREEN
    }

    /**
     * 火灾辉光渐变颜色（中心→边缘）
     */
    fun getFireGlowColors(): IntArray = intArrayOf(
        COLOR_FIRE_GLOW_CENTER,
        0xDDFF4500.toInt(),
        0xAAFF4500.toInt(),
        0x66FF4500.toInt(),
        0x22FF4500.toInt(),
        0x00FF0000
    )

    /**
     * 被困区域波纹颜色（中心→边缘）
     */
    fun getTrappedWaveColors(): IntArray = intArrayOf(
        0xBBFFD700.toInt(),
        0x88FFD700.toInt(),
        0x44FFD700.toInt(),
        0x11FFD700.toInt(),
        0x00000000
    )
}
