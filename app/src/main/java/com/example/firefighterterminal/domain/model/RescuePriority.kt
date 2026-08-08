package com.example.firefighterterminal.domain.model

/**
 * 救援优先级结果
 */
data class RescuePriority(
    val lightId: Int,
    val position: Position,
    val level: PriorityLevel,
    val score: Int,
    val reason: String
)

/**
 * 救援优先级等级
 */
enum class PriorityLevel(val label: String, val emoji: String) {
    P0("立即救援", "🔴"),   // 🔴
    P1("高优先级", "🟠"),   // 🟠
    P2("注意监控", "🟡"),   // 🟡
    P3("安全区域", "⚪")          // ⚪
}
