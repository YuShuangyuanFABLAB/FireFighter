package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.*
import com.example.firefighterterminal.domain.model.Position

/**
 * 被困区域警告渲染器 (Layer 1)
 */
class WarningRenderer {
    private val resolver = RenderColorResolver()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var phase: Float = 0f

    fun setPhase(p: Float) { phase = p }

    fun draw(canvas: Canvas, mapper: GridCoordinateMapper, trappedLights: List<Position>) {
        val colors = resolver.getTrappedWaveColors()
        for (pos in trappedLights) {
            val center = mapper.cellCenter(pos.x, pos.y)
            val maxR = minOf(mapper.cellWidth(pos.x), mapper.cellHeight(pos.y)) * 0.55f
            val waveR = maxR * (0.5f + 0.5f * phase)  // 0.5→1.0 扩散

            val stops = floatArrayOf(0f, 0.3f, 0.6f, 0.85f, 1f)
            val gradient = RadialGradient(center.x, center.y, waveR, colors, stops, Shader.TileMode.CLAMP)
            wavePaint.shader = gradient
            canvas.drawCircle(center.x, center.y, waveR, wavePaint)
        }
        wavePaint.shader = null
    }
}
