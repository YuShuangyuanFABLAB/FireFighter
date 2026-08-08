package com.example.firefighterterminal

import com.example.firefighterterminal.presentation.ui.map.view.GridCoordinateMapper
import org.junit.Assert.*
import org.junit.Test

/**
 * 像素坐标映射器测试
 *
 * 非均匀网格的像素坐标计算 — 地图渲染的基础数学
 */
class GridCoordinateMapperTest {

    private val colWidths = listOf(60f, 80f, 60f, 80f, 60f, 80f, 60f, 80f, 60f, 80f)
    private val rowHeights = listOf(80f, 60f, 80f, 60f, 80f)
    private val padding = 12f

    @Test
    fun `total width equals sum of col widths plus padding`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        val expectedWidth = colWidths.sum() + padding * 2
        assertEquals(expectedWidth, mapper.totalWidth, 0.01f)
    }

    @Test
    fun `total height equals sum of row heights plus padding`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        val expectedHeight = rowHeights.sum() + padding * 2
        assertEquals(expectedHeight, mapper.totalHeight, 0.01f)
    }

    @Test
    fun `cell x offset is padding plus sum of previous col widths`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        // col 0: offset = padding
        assertEquals(padding, mapper.cellLeft(0), 0.01f)
        // col 1: offset = padding + colWidths[0]
        assertEquals(padding + colWidths[0], mapper.cellLeft(1), 0.01f)
        // col 2: offset = padding + colWidths[0] + colWidths[1]
        assertEquals(padding + colWidths[0] + colWidths[1], mapper.cellLeft(2), 0.01f)
    }

    @Test
    fun `cell y offset is padding plus sum of previous row heights`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        assertEquals(padding, mapper.cellTop(0), 0.01f)
        assertEquals(padding + rowHeights[0], mapper.cellTop(1), 0.01f)
        assertEquals(padding + rowHeights[0] + rowHeights[1], mapper.cellTop(2), 0.01f)
    }

    @Test
    fun `cell width equals configured col width`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        assertEquals(colWidths[0], mapper.cellWidth(0), 0.01f)
        assertEquals(colWidths[5], mapper.cellWidth(5), 0.01f)
    }

    @Test
    fun `cell height equals configured row height`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        assertEquals(rowHeights[0], mapper.cellHeight(0), 0.01f)
        assertEquals(rowHeights[3], mapper.cellHeight(3), 0.01f)
    }

    @Test
    fun `cell center x is left plus half width`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        for (col in 0 until 10) {
            val expected = mapper.cellLeft(col) + mapper.cellWidth(col) / 2f
            assertEquals(expected, mapper.cellCenterX(col), 0.01f)
        }
    }

    @Test
    fun `cell center y is top plus half height`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        for (row in 0 until 5) {
            val expected = mapper.cellTop(row) + mapper.cellHeight(row) / 2f
            assertEquals(expected, mapper.cellCenterY(row), 0.01f)
        }
    }

    @Test
    fun `cellRect returns correct bounds for a cell`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        val rect = mapper.cellRect(3, 2)  // col=3, row=2

        assertEquals(mapper.cellLeft(3), rect.left, 0.01f)
        assertEquals(mapper.cellTop(2), rect.top, 0.01f)
        assertEquals(mapper.cellLeft(3) + mapper.cellWidth(3), rect.right, 0.01f)
        assertEquals(mapper.cellTop(2) + mapper.cellHeight(2), rect.bottom, 0.01f)
    }

    @Test
    fun `cellCenter returns correct point for a cell`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        val center = mapper.cellCenter(3, 2)

        assertEquals(mapper.cellCenterX(3), center.x, 0.01f)
        assertEquals(mapper.cellCenterY(2), center.y, 0.01f)
    }

    @Test
    fun `hit test identifies correct cell from pixel coordinates`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        // 点击 cell(3,2) 的中心应该返回 (3,2)
        val center = mapper.cellCenter(3, 2)
        val hit = mapper.hitTest(center.x, center.y)
        assertNotNull(hit)
        assertEquals(3, hit?.x)
        assertEquals(2, hit?.y)
    }

    @Test
    fun `hit test returns null for coordinates in padding area`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        // padding 区域内的点击
        val hit = mapper.hitTest(padding / 2f, padding / 2f)
        assertNull(hit)
    }

    @Test
    fun `hit test returns null for coordinates beyond total size`() {
        val mapper = GridCoordinateMapper(colWidths, rowHeights, padding)

        val hit = mapper.hitTest(mapper.totalWidth + 100f, mapper.totalHeight + 100f)
        assertNull(hit)
    }

    @Test
    fun `works with uniform grid sizes`() {
        val uniformCols = listOf(50f, 50f, 50f, 50f, 50f)
        val uniformRows = listOf(50f, 50f, 50f)
        val mapper = GridCoordinateMapper(uniformCols, uniformRows, 0f)

        assertEquals(250f, mapper.totalWidth, 0.01f)
        assertEquals(150f, mapper.totalHeight, 0.01f)
        assertEquals(0f, mapper.cellLeft(0), 0.01f)
        assertEquals(50f, mapper.cellLeft(1), 0.01f)
        assertEquals(100f, mapper.cellLeft(2), 0.01f)
    }

    @Test
    fun `grid with single cell works`() {
        val mapper = GridCoordinateMapper(listOf(100f), listOf(80f), 10f)

        assertEquals(100f + 20f, mapper.totalWidth, 0.01f)
        assertEquals(80f + 20f, mapper.totalHeight, 0.01f)
        assertEquals(0, mapper.hitTest(60f, 50f)?.x)
        assertEquals(0, mapper.hitTest(60f, 50f)?.y)
    }
}
