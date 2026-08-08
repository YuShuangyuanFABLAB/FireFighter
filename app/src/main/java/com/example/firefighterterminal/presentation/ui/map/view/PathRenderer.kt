package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*
import com.example.firefighterterminal.domain.model.Position

/**
 * 进攻路线渲染器 (Layer 2)
 */
class PathRenderer {
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_PATH_BLUE; strokeWidth = 3f
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
    }
    private val startPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_PATH_BLUE; style = Paint.Style.FILL
    }
    private val endPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED; style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private var dashPhase: Float = 0f

    fun setDashPhase(p: Float) { dashPhase = p }

    fun draw(canvas: Canvas, mapper: GridCoordinateMapper, path: List<Position>) {
        if (path.size < 2) return

        // 虚线路径
        pathPaint.pathEffect = DashPathEffect(floatArrayOf(10f, 8f), dashPhase)
        val pixelPath = Path()
        val start = mapper.cellCenter(path[0].x, path[0].y)
        pixelPath.moveTo(start.x, start.y)
        for (i in 1 until path.size) {
            val pt = mapper.cellCenter(path[i].x, path[i].y)
            pixelPath.lineTo(pt.x, pt.y)
        }
        canvas.drawPath(pixelPath, pathPaint)

        // 起点标记
        canvas.drawCircle(start.x, start.y, 6f, startPaint)

        // 终点标记
        val end = mapper.cellCenter(path.last().x, path.last().y)
        canvas.drawCircle(end.x, end.y, 8f, endPaint)
        canvas.drawCircle(end.x, end.y, 4f, endPaint)
    }
}
