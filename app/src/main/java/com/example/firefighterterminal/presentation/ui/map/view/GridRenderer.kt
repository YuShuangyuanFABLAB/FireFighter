package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*
import com.example.firefighterterminal.data.ble.FireMessage
import com.example.firefighterterminal.domain.model.Position

/**
 * 网格渲染器 (Layer 0) — 墙壁/通道/出口/网格线
 */
class GridRenderer {

    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_WALL; style = Paint.Style.FILL
    }
    private val wallLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 255, 255, 255); strokeWidth = 1f; style = Paint.Style.STROKE
    }
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_FLOOR; style = Paint.Style.FILL
    }
    private val exitBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 0, 255, 136); style = Paint.Style.FILL
    }
    private val exitGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_EXIT_AVAILABLE; style = Paint.Style.STROKE
        strokeWidth = 2.5f; pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }
    private val exitBlockedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_EXIT_BLOCKED; style = Paint.Style.STROKE; strokeWidth = 2.5f
    }
    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 255, 255, 255); strokeWidth = 0.5f; style = Paint.Style.STROKE
    }
    private val exitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_EXIT_AVAILABLE; textSize = 13f
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val exitBlockedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_EXIT_BLOCKED; textSize = 13f
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }
    private val resolver = RenderColorResolver()

    fun draw(canvas: Canvas, mapper: GridCoordinateMapper, config: FireMessage.MapConfig, fires: List<Position>) {
        val wallSet = config.walls.toSet(); val exitSet = config.exits.toSet(); val fireSet = fires.toSet()
        for (row in 0 until config.height) {
            for (col in 0 until config.width) {
                val rect = mapper.cellRect(col, row)
                when (resolver.resolveCellType(col, row, wallSet, exitSet, fireSet)) {
                    CellType.WALL -> { canvas.drawRect(rect, wallPaint); drawBrickLines(canvas, rect) }
                    CellType.FLOOR -> canvas.drawRect(rect, floorPaint)
                    CellType.EXIT_AVAILABLE -> drawExit(canvas, rect, false)
                    CellType.EXIT_BLOCKED -> drawExit(canvas, rect, true)
                    CellType.FIRE -> canvas.drawRect(rect, floorPaint)
                }
                canvas.drawRect(rect, gridLinePaint)
            }
        }
    }

    private fun drawBrickLines(canvas: Canvas, rect: RectF) {
        val ls = rect.height() / 3f
        for (i in 1..2) { val y = rect.top + ls * i; canvas.drawLine(rect.left, y, rect.right, y, wallLinePaint) }
    }

    private fun drawExit(canvas: Canvas, rect: RectF, blocked: Boolean) {
        canvas.drawRect(rect, exitBgPaint)
        val p = if (blocked) exitBlockedPaint else exitGlowPaint
        canvas.drawRoundRect(RectF(rect.left + 2, rect.top + 2, rect.right - 2, rect.bottom - 2), 4f, 4f, p)
        val tp = if (blocked) exitBlockedTextPaint else exitTextPaint
        canvas.drawText("EXIT", rect.centerX(), rect.bottom - 4f, tp)
    }
}
