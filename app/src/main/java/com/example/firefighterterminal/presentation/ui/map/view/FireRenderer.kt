package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*
import com.example.firefighterterminal.domain.model.Position

/**
 * 火点渲染器 (Layer 3) — 径向辉光 + 脉动动画
 */
class FireRenderer {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f; textAlign = Paint.Align.CENTER
    }
    private val resolver = RenderColorResolver()
    private var phase: Float = 0f

    fun setPhase(p: Float) { phase = p }

    fun draw(canvas: Canvas, mapper: GridCoordinateMapper, fires: List<Position>) {
        val colors = resolver.getFireGlowColors()
        for (fire in fires) {
            val center = mapper.cellCenter(fire.x, fire.y)
            val maxR = minOf(mapper.cellWidth(fire.x), mapper.cellHeight(fire.y)) * 0.45f
            val pulse = 1.0f + 0.12f * kotlin.math.sin(phase * 2 * Math.PI).toFloat()
            val radius = maxR * pulse

            val stops = floatArrayOf(0f, 0.35f, 0.65f, 0.85f, 0.95f, 1f)
            val gradient = RadialGradient(center.x, center.y, radius, colors, stops, Shader.TileMode.CLAMP)
            glowPaint.shader = gradient
            canvas.drawCircle(center.x, center.y, radius, glowPaint)

            canvas.drawText("🔥", center.x, center.y + 10f, iconPaint)
        }
        glowPaint.shader = null
    }
}
