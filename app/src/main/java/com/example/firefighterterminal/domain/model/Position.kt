package com.example.firefighterterminal.domain.model

/**
 * 地图坐标 — 不可变值对象
 *
 * 坐标系统：x 从左到右 (0 ~ width-1)，y 从上到下 (0 ~ height-1)
 */
data class Position(
    val x: Int,
    val y: Int
) {
    /**
     * 检查坐标是否在地图边界内
     * @param width 地图宽度 (列数)，默认 10
     * @param height 地图高度 (行数)，默认 5
     */
    fun isValid(width: Int = 10, height: Int = 5): Boolean {
        return x in 0 until width && y in 0 until height
    }

    /**
     * 计算与另一个坐标的曼哈顿距离
     */
    fun manhattanDistance(other: Position): Int {
        return kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
    }
}
