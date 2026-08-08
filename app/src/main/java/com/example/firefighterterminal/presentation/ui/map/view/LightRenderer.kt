package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*

/**
 * 灯牌箭头渲染器 (Layer 4) — 底座 + 方向箭头 + 警告标记
 */
class LightRenderer {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 45, 45, 76); style = Paint.Style.FILL
    }
    private val arrowGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_LIGHT_GREEN; strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val arrowYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_LIGHT_YELLOW; strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val arrowDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255); strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
    }
    private val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = RenderColorResolver.COLOR_LIGHT_YELLOW; style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 16f; textAlign = Paint.Align.CENTER
    }
    private val calc = ArrowCalculator()

    data class LightPos(val id: Int, val x: Int, val y: Int, val type: String)

    fun draw(
        canvas: Canvas, mapper: GridCoordinateMapper,
        lights: List<LightPos>, directions: Map<Int, Int>,
        hasFire: Boolean, flashOn: Boolean
    ) {
        // 统一箭头大小
        val baseSize = minOf(mapper.cellWidth(0), mapper.cellHeight(0)) * 0.45f

        for (light in lights) {
            val dir = directions[light.id] ?: 0
            val cellCenter = mapper.cellCenter(light.x, light.y)
            val isExit = (dir == 3)
            // 出口灯牌箭头上移避开 EXIT
            val center = if (isExit) PointF(cellCenter.x, cellCenter.y - baseSize * 0.5f) else cellCenter
            val arrow = calc.compute(light.type, dir)
            val size = baseSize

            canvas.drawCircle(center.x, center.y, size * 0.5f, basePaint)

            when (arrow.primary) {
                ArrowDirection.OFF -> {}
                ArrowDirection.WARNING -> drawWarning(canvas, center, size)
                else -> {
                    val paint = when {
                        arrow.isYellowWarning -> arrowYellow
                        hasFire && !flashOn -> arrowDim
                        else -> arrowGreen
                    }
                    drawArrow(canvas, center, size, arrowToAngle(arrow.primary), paint)
                    arrow.secondary?.let { drawArrow(canvas, center, size * 0.6f, arrowToAngle(it), paint) }
                }
            }
            // 编号放格子右下角，不遮挡箭头
            val rect = mapper.cellRect(light.x, light.y)
            val labelPaintCorner = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 14f }
            canvas.drawText("${light.id}", rect.right - 3f, rect.bottom - 3f, labelPaintCorner)
        }
    }

    private fun arrowToAngle(dir: ArrowDirection): Float = when (dir) {
        ArrowDirection.LEFT -> 180f; ArrowDirection.RIGHT -> 0f
        ArrowDirection.UP -> 270f; ArrowDirection.DOWN -> 90f; else -> 0f
    }

    private fun drawArrow(canvas: Canvas, c: PointF, size: Float, angle: Float, paint: Paint) {
        val rad = Math.toRadians(angle.toDouble())
        val cosA = kotlin.math.cos(rad).toFloat()
        val sinA = kotlin.math.sin(rad).toFloat()
        val perpX = kotlin.math.cos(rad + Math.PI / 2).toFloat()
        val perpY = kotlin.math.sin(rad + Math.PI / 2).toFloat()

        // 三角箭头以 c 为质心：尖头=2/3·size, 底座=1/3·size  (T=2B 保证质心在c)
        val tipX = c.x + size * (2f / 3f) * cosA
        val tipY = c.y + size * (2f / 3f) * sinA
        val hw = size * 0.4f
        val baseCx = c.x - size * (1f / 3f) * cosA
        val baseCy = c.y - size * (1f / 3f) * sinA

        val path = Path().apply {
            moveTo(tipX, tipY)
            lineTo(baseCx + perpX * hw, baseCy + perpY * hw)
            lineTo(baseCx - perpX * hw, baseCy - perpY * hw)
            close()
        }
        canvas.drawPath(path, Paint(paint).apply { style = Paint.Style.FILL; isAntiAlias = true })
    }

    private fun drawWarning(canvas: Canvas, c: PointF, size: Float) {
        val h = size * 0.6f; val w = size * 0.45f
        val path = Path().apply {
            moveTo(c.x, c.y - h); lineTo(c.x + w, c.y + h * 0.5f); lineTo(c.x - w, c.y + h * 0.5f); close()
        }
        canvas.drawPath(path, warnPaint)
        canvas.drawText("!", c.x, c.y + h * 0.15f, Paint(labelPaint).apply { textSize = 18f; color = Color.BLACK })
    }
}
