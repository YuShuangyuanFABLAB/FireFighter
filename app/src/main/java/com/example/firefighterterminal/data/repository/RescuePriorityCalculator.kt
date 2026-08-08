package com.example.firefighterterminal.data.repository

import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.domain.model.PriorityLevel
import com.example.firefighterterminal.domain.model.RescuePriority

/**
 * 救援优先级计算器
 *
 * 根据火场状态计算每个灯牌区域的救援优先级。
 *
 * 算法（Spec §7.1）：
 * 1. 黄闪灯牌(direction=4) → base score 100 → P0
 * 2. 离最近火点的距离越近，score 越高（衰减 max(0, 50 - dist*10)）
 * 3. 结果按 score 降序返回
 */
class RescuePriorityCalculator {

    /** 灯牌状态（用于计算优先级） */
    data class LightState(
        val id: Int,
        val position: Position,
        val direction: Int,  // 0=熄灯, 1=PRIMARY, 2=SECONDARY, 3=AT_EXIT, 4=NO_PATH
    )

    companion object {
        private const val SCORE_TRAPPED = 100      // 被困基础分
        private const val SCORE_PROXIMITY_MAX = 50  // 邻近火点最大分
        private const val SCORE_PROXIMITY_DECAY = 10 // 每格衰减
    }

    /**
     * 计算所有灯牌的救援优先级
     *
     * @param lights 灯牌列表
     * @param fires 当前火灾点列表
     * @param walls 墙壁坐标列表
     * @param predictedSpread 预测蔓延位置列表（可选）
     * @return 按 score 降序排列的优先级列表
     */
    fun compute(
        lights: List<LightState>,
        fires: List<Position>,
        walls: List<Position>,
        predictedSpread: List<Position> = emptyList()
    ): List<RescuePriority> {
        if (lights.isEmpty()) return emptyList()

        val priorities = lights.map { light ->
            var score = 0

            // 1. 被困区域 → 最高优先级
            if (light.direction == 4) {
                score += SCORE_TRAPPED
            }

            // 2. 离最近火点距离
            if (fires.isNotEmpty()) {
                val minDistToFire = fires.minOf { light.position.manhattanDistance(it) }
                score += (SCORE_PROXIMITY_MAX - minDistToFire * SCORE_PROXIMITY_DECAY)
                    .coerceAtLeast(0)
            }

            // 3. 确定等级
            val level = when {
                score >= 100 -> PriorityLevel.P0
                score >= 50 -> PriorityLevel.P1
                score >= 20 -> PriorityLevel.P2
                else -> PriorityLevel.P3
            }

            // 4. 生成原因描述
            val reason = buildReason(light, fires, score)

            RescuePriority(
                lightId = light.id,
                position = light.position,
                level = level,
                score = score,
                reason = reason
            )
        }

        // 按 score 降序
        return priorities.sortedByDescending { it.score }
    }

    private fun buildReason(
        light: LightState,
        fires: List<Position>,
        score: Int
    ): String {
        val parts = mutableListOf<String>()

        if (light.direction == 4) {
            parts.add("灯牌显示黄闪(被困)")
        }

        if (fires.isNotEmpty()) {
            val minDist = fires.minOf { light.position.manhattanDistance(it) }
            if (minDist == 0) {
                parts.add("位于火灾点")
            } else if (minDist <= 2) {
                parts.add("距离火点${minDist}格")
            } else {
                parts.add("距离最近火点${minDist}格")
            }
        }

        if (fires.isEmpty()) {
            parts.add("当前无火情")
        }

        return parts.joinToString("，")
    }
}
