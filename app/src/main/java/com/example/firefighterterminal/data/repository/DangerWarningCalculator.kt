package com.example.firefighterterminal.data.repository

import com.example.firefighterterminal.domain.model.Position

enum class WarningLevel(val label: String) { SAFE("安全"), WARNING("黄色预警"), CRITICAL("红色预警") }

data class DangerWarning(val lightId: Int, val position: Position, val level: WarningLevel, val reason: String)

class DangerWarningCalculator {

    data class LightInfo(val id: Int, val position: Position, val direction: Int)

    fun compute(
        lights: List<LightInfo>,
        fires: Set<Position>,
        predictedSpread: Set<Position>,
        blockedExits: Set<Position>,
        exits: Set<Position>
    ): List<DangerWarning> {
        return lights.map { light ->
            val adjacentFireCount = countAdjacentFires(light.position, fires)
            val onFire = fires.contains(light.position)
            val onSpreadPath = predictedSpread.contains(light.position)
            val isTrapped = light.direction == 4

            val (level, reason) = when {
                isTrapped -> WarningLevel.CRITICAL to "灯牌显示黄闪(被困区域)"
                onFire -> WarningLevel.CRITICAL to "灯牌位于火灾点"
                adjacentFireCount >= 3 -> WarningLevel.CRITICAL to "周围$adjacentFireCount 处着火"
                adjacentFireCount >= 2 -> WarningLevel.WARNING to "周围$adjacentFireCount 处着火"
                onSpreadPath -> WarningLevel.WARNING to "位于火势蔓延路径上"
                adjacentFireCount == 1 -> {
                    // 检查是否有出口被堵
                    val exitBlocked = exits.any { e -> fires.any { f -> adjacent(e, f) } }
                    if (exitBlocked) WarningLevel.WARNING to "附近火点+出口被阻断"
                    else WarningLevel.SAFE to "附近1处火点"
                }
                else -> WarningLevel.SAFE to "无直接威胁"
            }

            DangerWarning(light.id, light.position, level, reason)
        }
    }

    private fun countAdjacentFires(pos: Position, fires: Set<Position>): Int {
        val dirs = arrayOf(Position(0, -1), Position(1, 0), Position(0, 1), Position(-1, 0))
        return dirs.count { d -> fires.contains(Position(pos.x + d.x, pos.y + d.y)) }
    }

    private fun adjacent(a: Position, b: Position): Boolean =
        kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y) == 1
}
