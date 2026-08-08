package com.example.firefighterterminal.presentation.ui.map.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import android.view.animation.LinearInterpolator
import com.example.firefighterterminal.data.ble.FireMessage
import com.example.firefighterterminal.data.repository.FireDataSnapshot
import com.example.firefighterterminal.domain.model.Position
import kotlin.math.*

class FireMapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 数据
    private var mapConfig: FireMessage.MapConfig? = null
    private var lightPositions: List<LightRenderer.LightPos> = emptyList()
    private var directionMap: Map<Int, Int> = emptyMap()
    private var fires: List<Position> = emptyList()
    private var attackPath: List<Position> = emptyList()
    private var trappedPositions: List<Position> = emptyList()
    private var priorities: List<com.example.firefighterterminal.domain.model.RescuePriority> = emptyList()
    private var mapper: GridCoordinateMapper? = null

    // 渲染器
    private val gridRenderer = GridRenderer()
    private val priorityRenderer = PriorityOverlayRenderer()
    private val warningRenderer = WarningRenderer()
    private val pathRenderer = PathRenderer()
    private val fireRenderer = FireRenderer()
    private val lightRenderer = LightRenderer()

    // 动画
    private var animPhase: Float = 0f
    private var animator: ValueAnimator? = null
    private var isAnimating: Boolean = false

    // 变换：旋转90度 + 缩放 + 平移
    private val transform = Matrix()
    private var mapScale: Float = 1f
    private var mapTransX: Float = 0f
    private var mapTransY: Float = 0f
    private var needsRecalc = true

    // 手势
    private val scaleDetector = ScaleGestureDetector(context, ScaleListener())
    private val gestureDetector = GestureDetector(context, GestureListener())

    var onLightSelected: ((Int) -> Unit)? = null
    private val bgPaint = Paint().apply { color = Color.parseColor("#1a1a2e") }

    // ==================== 数据更新 ====================

    fun updateMapConfig(config: FireMessage.MapConfig) {
        this.mapConfig = config
        this.mapper = GridCoordinateMapper(config.colWidths, config.rowHeights, 12f)
        needsRecalc = true
        invalidate()
    }
    fun updateLightConfig(lights: List<LightRenderer.LightPos>) { this.lightPositions = lights; invalidate() }
    fun updateFireData(data: FireDataSnapshot?) {
        data ?: return
        this.fires = data.fires; this.directionMap = data.directions
        this.trappedPositions = data.lightConfigs
            ?.filter { (id, _) -> (data.directions[id] ?: 0) == 4 }
            ?.map { (_, info) -> Position(info.x, info.y) } ?: emptyList()
        if (data.fireCount > 0 && !isAnimating) startAnim()
        else if (data.fireCount == 0 && isAnimating) stopAnim()
        invalidate()
    }
    fun updatePriorities(list: List<com.example.firefighterterminal.domain.model.RescuePriority>) { priorities = list; invalidate() }
    fun showAttackPath(path: List<Position>) { attackPath = path; invalidate() }
    fun clearAttackPath() { attackPath = emptyList(); invalidate() }

    // ==================== 动画 ====================
    private fun startAnim() {
        if (isAnimating) return; isAnimating = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000; repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animPhase = it.animatedValue as Float; invalidate() }
            start()
        }
    }
    private fun stopAnim() { isAnimating = false; animator?.cancel(); animator = null }

    // ==================== 变换计算 ====================

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        needsRecalc = true
    }

    /** 构建 Canvas 变换矩阵: 平移(居中) · 旋转90° · 缩放 · 平移(地图中心) */
    private fun buildTransform() {
        val map = mapper ?: return
        if (width <= 0 || height <= 0) return

        val mw = map.totalWidth; val mh = map.totalHeight

        // 首次或双点复位时重新计算最佳缩放
        if (needsRecalc) {
            val sx = (width - 20f) / mh; val sy = (height - 20f) / mw
            mapScale = minOf(sx, sy)
            mapTransX = width / 2f; mapTransY = height / 2f
            needsRecalc = false
        }

        // M = T(viewC) · R(-90) · S(scale) · T(-mapC)
        transform.setTranslate(mapTransX, mapTransY)
        transform.preRotate(-90f)
        transform.preScale(mapScale, mapScale)
        transform.preTranslate(-mw / 2f, -mh / 2f)
    }

    // ==================== 手势 ====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private val SCALE_MIN = 0.3f; private val SCALE_MAX = 5f

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mapScale = (mapScale * detector.scaleFactor).coerceIn(SCALE_MIN, SCALE_MAX)
            invalidate()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            // 标准跟随手指移动: content moves with finger
            mapTransX -= dx; mapTransY -= dy
            invalidate()
            return true
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            needsRecalc = true; invalidate()
            return true
        }
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val map = mapper ?: return false
            val inv = Matrix(); transform.invert(inv)
            val pt = floatArrayOf(e.x, e.y); inv.mapPoints(pt)
            val hit = map.hitTest(pt[0], pt[1])
            if (hit != null) {
                lightPositions.find { it.x == hit.x && it.y == hit.y }?.let {
                    onLightSelected?.invoke(it.id)
                }
            }
            return true
        }
    }

    // ==================== 绘制 ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bgPaint.color)
        val config = mapConfig ?: return
        buildTransform()
        val map = mapper ?: return

        canvas.save()
        canvas.concat(transform)

        gridRenderer.draw(canvas, map, config, fires)

        // 优先级颜色叠加（BFS 扩散到相邻通道）
        if (priorities.isNotEmpty()) {
            priorityRenderer.setPhase(animPhase)
            priorityRenderer.draw(canvas, map, priorities, config.walls.toSet(), fires.toSet())
        }

        if (trappedPositions.isNotEmpty()) {
            warningRenderer.setPhase(animPhase)
            warningRenderer.draw(canvas, map, trappedPositions)
        }
        if (attackPath.isNotEmpty()) {
            pathRenderer.setDashPhase(animPhase * 20f)
            pathRenderer.draw(canvas, map, attackPath)
        }
        if (fires.isNotEmpty()) {
            fireRenderer.setPhase(animPhase)
            fireRenderer.draw(canvas, map, fires)
        }
        if (lightPositions.isNotEmpty()) {
            val hf = fires.isNotEmpty(); val fo = (animPhase < 0.5f) || !hf
            lightRenderer.draw(canvas, map, lightPositions, directionMap, hf, fo)
        }

        canvas.restore()
    }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); stopAnim() }
}
