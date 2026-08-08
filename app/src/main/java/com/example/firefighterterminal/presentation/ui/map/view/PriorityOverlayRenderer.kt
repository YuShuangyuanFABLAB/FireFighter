package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*
import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.domain.model.PriorityLevel
import com.example.firefighterterminal.domain.model.RescuePriority

/**
 * 救援优先级颜色叠加 (Layer 0.5)
 * P0=红色 P1=橙色 P2=黄色 P3=不渲染
 * 从灯牌位置沿通道向四周 BFS 扩散
 */
class PriorityOverlayRenderer {

    private val p0Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 255, 60, 60); style = Paint.Style.FILL
    }
    private val p0Border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 30, 30); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val p1Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 150, 30); style = Paint.Style.FILL
    }
    private val p2Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(50, 255, 210, 30); style = Paint.Style.FILL
    }

    private var dashPhase: Float = 0f
    fun setPhase(p: Float) { dashPhase = p }

    fun draw(
        canvas: Canvas, mapper: GridCoordinateMapper,
        priorities: List<RescuePriority>,
        walls: Set<Position>, fires: Set<Position>
    ) {
        // 已渲染的格子，避免重复（保留最高优先级）
        val rendered = mutableSetOf<Position>()

        for (p in priorities.sortedBy { it.score }) { // 高分后渲染（覆盖）
            if (p.level == PriorityLevel.P3) continue
            val radius = when (p.level) { PriorityLevel.P0 -> 2; PriorityLevel.P1 -> 1; else -> 0 }
            val cells = expandByBfs(p.position, radius, walls, fires)
            val rects = cells.map { mapper.cellRect(it.x, it.y) }

            when (p.level) {
                PriorityLevel.P0 -> {
                    p0Border.pathEffect = DashPathEffect(floatArrayOf(8f, 4f), dashPhase * 10f)
                    for (r in rects) {
                        canvas.drawRoundRect(r, 6f, 6f, p0Paint)
                        canvas.drawRoundRect(r, 6f, 6f, p0Border)
                    }
                }
                PriorityLevel.P1 -> {
                    for (r in rects) canvas.drawRoundRect(r, 4f, 4f, p1Paint)
                }
                PriorityLevel.P2 -> {
                    for (r in rects) canvas.drawRoundRect(r, 4f, 4f, p2Paint)
                }
                else -> {}
            }
        }
    }

    /** BFS 从灯牌位置向四周扩散 radius 格，仅走通道 */
    private fun expandByBfs(start: Position, radius: Int, walls: Set<Position>, fires: Set<Position>): Set<Position> {
        if (radius <= 0) return setOf(start)
        val result = mutableSetOf(start)
        val visited = mutableSetOf(start)
        val queue = ArrayDeque<Pair<Position, Int>>()
        queue.add(start to 0)
        val dirs = arrayOf(Position(0,-1), Position(1,0), Position(0,1), Position(-1,0))

        while (queue.isNotEmpty()) {
            val (cur, dist) = queue.removeFirst()
            if (dist >= radius) continue
            for (d in dirs) {
                val nx = cur.x + d.x; val ny = cur.y + d.y
                val np = Position(nx, ny)
                if (nx in 0..9 && ny in 0..4 && np !in visited && np !in walls && np !in fires) {
                    visited.add(np); result.add(np)
                    queue.add(np to dist + 1)
                }
            }
        }
        return result
    }
}
