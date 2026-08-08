package com.example.firefighterterminal

import com.example.firefighterterminal.presentation.ui.map.view.ArrowDirection
import com.example.firefighterterminal.presentation.ui.map.view.ArrowCalculator
import org.junit.Assert.*
import org.junit.Test

/**
 * 箭头方向计算器测试
 *
 * 根据灯牌类型和方向值，确定箭头的视觉朝向和颜色
 *
 * 方向映射规格:
 *   横向灯 (HORIZONTAL_UP/DOWN):
 *     DIR_PRIMARY(1)   → 左箭头, LED1 亮
 *     DIR_SECONDARY(2) → 右箭头, LED2 亮
 *     DIR_AT_EXIT(3)   → 双箭头(左右), 双绿
 *     DIR_NO_PATH(4)   → 警告标记, 双黄
 *
 *   纵向灯 (VERTICAL_LEFT/RIGHT):
 *     DIR_PRIMARY(1)   → 上箭头, LED2 亮
 *     DIR_SECONDARY(2) → 下箭头, LED1 亮
 *     DIR_AT_EXIT(3)   → 双箭头(上下), 双绿
 *     DIR_NO_PATH(4)   → 警告标记, 双黄
 */
class ArrowCalculatorTest {

    private val calc = ArrowCalculator()

    // ==================== 横向灯 ====================

    @Test
    fun `horizontal light DIR_PRIMARY points left`() {
        val result = calc.compute("HORIZONTAL_UP", 1)
        assertEquals(ArrowDirection.LEFT, result.primary)
        assertNull(result.secondary)
    }

    @Test
    fun `horizontal light DIR_SECONDARY points right`() {
        val result = calc.compute("HORIZONTAL_UP", 2)
        assertEquals(ArrowDirection.RIGHT, result.primary)
        assertNull(result.secondary)
    }

    @Test
    fun `horizontal light DIR_AT_EXIT shows double green arrows`() {
        val result = calc.compute("HORIZONTAL_UP", 3)
        assertEquals(ArrowDirection.LEFT, result.primary)
        assertEquals(ArrowDirection.RIGHT, result.secondary)
        assertTrue(result.isDoubleGreen)
    }

    @Test
    fun `horizontal light DIR_NO_PATH shows yellow warning`() {
        val result = calc.compute("HORIZONTAL_UP", 4)
        assertEquals(ArrowDirection.WARNING, result.primary)
        assertTrue(result.isYellowWarning)
    }

    @Test
    fun `horizontal light direction 0 is off`() {
        val result = calc.compute("HORIZONTAL_UP", 0)
        assertEquals(ArrowDirection.OFF, result.primary)
        assertNull(result.secondary)
    }

    // ==================== 纵向灯 ====================

    @Test
    fun `vertical light DIR_PRIMARY points up`() {
        val result = calc.compute("VERTICAL_LEFT", 1)
        assertEquals(ArrowDirection.UP, result.primary)
        assertNull(result.secondary)
    }

    @Test
    fun `vertical light DIR_SECONDARY points down`() {
        val result = calc.compute("VERTICAL_LEFT", 2)
        assertEquals(ArrowDirection.DOWN, result.primary)
        assertNull(result.secondary)
    }

    @Test
    fun `vertical light DIR_AT_EXIT shows double green arrows up-down`() {
        val result = calc.compute("VERTICAL_LEFT", 3)
        assertEquals(ArrowDirection.UP, result.primary)
        assertEquals(ArrowDirection.DOWN, result.secondary)
        assertTrue(result.isDoubleGreen)
    }

    @Test
    fun `vertical light DIR_NO_PATH shows yellow warning`() {
        val result = calc.compute("VERTICAL_LEFT", 4)
        assertEquals(ArrowDirection.WARNING, result.primary)
        assertTrue(result.isYellowWarning)
    }

    @Test
    fun `vertical light direction 0 is off`() {
        val result = calc.compute("VERTICAL_LEFT", 0)
        assertEquals(ArrowDirection.OFF, result.primary)
        assertNull(result.secondary)
    }

    // ==================== HORIZONTAL_DOWN 类型 ====================

    @Test
    fun `HORIZONTAL_DOWN behaves same as HORIZONTAL_UP for direction mapping`() {
        // 两种横向类型仅在物理LED排列上有区别，视觉箭头方向一致
        val result = calc.compute("HORIZONTAL_DOWN", 1)
        assertEquals(ArrowDirection.LEFT, result.primary)
    }

    // ==================== VERTICAL_RIGHT 类型 ====================

    @Test
    fun `VERTICAL_RIGHT behaves same as VERTICAL_LEFT for direction mapping`() {
        val result = calc.compute("VERTICAL_RIGHT", 2)
        assertEquals(ArrowDirection.DOWN, result.primary)
    }

    // ==================== 未知类型回退 ====================

    @Test
    fun `unknown light type falls back to horizontal behavior`() {
        val result = calc.compute("UNKNOWN", 1)
        assertEquals(ArrowDirection.LEFT, result.primary)
    }

    @Test
    fun `unknown direction value falls back to OFF`() {
        val result = calc.compute("HORIZONTAL_UP", 99)
        assertEquals(ArrowDirection.OFF, result.primary)
    }
}
