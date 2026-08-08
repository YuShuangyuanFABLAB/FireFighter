package com.example.firefighterterminal.presentation.ui.map.view

import android.graphics.PointF
import android.graphics.RectF
import com.example.firefighterterminal.domain.model.Position

/**
 * 非均匀网格像素坐标映射器
 *
 * 将逻辑坐标 (col, row) 映射到画布像素坐标。
 * 每个格子的宽度和高度可独立配置，支持非均匀网格布局。
 *
 * @param colWidths 每列宽度（dp），长度 = 列数
 * @param rowHeights 每行高度（dp），长度 = 行数
 * @param padding 地图四周留白（dp）
 */
class GridCoordinateMapper(
    private val colWidths: List<Float>,
    private val rowHeights: List<Float>,
    private val padding: Float
) {
    val cols: Int get() = colWidths.size
    val rows: Int get() = rowHeights.size

    val totalWidth: Float get() = colWidths.sum() + padding * 2
    val totalHeight: Float get() = rowHeights.sum() + padding * 2

    // 缓存的累加偏移（避免每次计算时重复累加）
    private val colOffsets: FloatArray = FloatArray(colWidths.size + 1)
    private val rowOffsets: FloatArray = FloatArray(rowHeights.size + 1)

    init {
        for (i in colOffsets.indices) {
            colOffsets[i] = if (i == 0) padding else colOffsets[i - 1] + colWidths[i - 1]
        }
        for (i in rowOffsets.indices) {
            rowOffsets[i] = if (i == 0) padding else rowOffsets[i - 1] + rowHeights[i - 1]
        }
    }

    /** 格子左边 x 坐标 */
    fun cellLeft(col: Int): Float = colOffsets[col]

    /** 格子上边 y 坐标 */
    fun cellTop(row: Int): Float = rowOffsets[row]

    /** 格子宽度 */
    fun cellWidth(col: Int): Float = colWidths[col]

    /** 格子高度 */
    fun cellHeight(row: Int): Float = rowHeights[row]

    /** 格子中心 x */
    fun cellCenterX(col: Int): Float = cellLeft(col) + cellWidth(col) / 2f

    /** 格子中心 y */
    fun cellCenterY(row: Int): Float = cellTop(row) + cellHeight(row) / 2f

    /** 格子的像素矩形 */
    fun cellRect(col: Int, row: Int): RectF {
        val left = cellLeft(col)
        val top = cellTop(row)
        val r = RectF()
        r.left = left
        r.top = top
        r.right = left + cellWidth(col)
        r.bottom = top + cellHeight(row)
        return r
    }

    /** 格子中心点 */
    fun cellCenter(col: Int, row: Int): PointF {
        val cx = cellCenterX(col)
        val cy = cellCenterY(row)
        val p = PointF()
        p.x = cx
        p.y = cy
        return p
    }

    /**
     * 根据像素坐标反查逻辑格子位置
     *
     * @param px 像素 x
     * @param py 像素 y
     * @return 对应的网格坐标，超出范围返回 null
     */
    fun hitTest(px: Float, py: Float): Position? {
        // 检查是否在 padding 区域内
        if (px < padding || py < padding ||
            px >= totalWidth - padding || py >= totalHeight - padding) {
            return null
        }

        // 查找包含 px 的列
        var colIndex = -1
        for (i in 0 until cols) {
            val left = colOffsets[i]
            val right = colOffsets[i + 1]
            if (px >= left && px < right) {
                colIndex = i
                break
            }
        }

        // 查找包含 py 的行
        var rowIndex = -1
        for (i in 0 until rows) {
            val top = rowOffsets[i]
            val bottom = rowOffsets[i + 1]
            if (py >= top && py < bottom) {
                rowIndex = i
                break
            }
        }

        if (colIndex < 0 || rowIndex < 0) return null
        return Position(colIndex, rowIndex)
    }
}
