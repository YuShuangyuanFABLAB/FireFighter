package com.example.firefighterterminal

import com.example.firefighterterminal.domain.model.Position
import org.junit.Assert.*
import org.junit.Test

/**
 * Position 模型单元测试
 * 坐标值对象：不可变，支持相等比较
 */
class PositionTest {

    @Test
    fun `constructor stores x and y coordinates`() {
        val pos = Position(3, 7)
        assertEquals(3, pos.x)
        assertEquals(7, pos.y)
    }

    @Test
    fun `equal positions with same x and y`() {
        val a = Position(3, 7)
        val b = Position(3, 7)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `not equal positions with different x`() {
        val a = Position(3, 7)
        val b = Position(5, 7)
        assertNotEquals(a, b)
    }

    @Test
    fun `not equal positions with different y`() {
        val a = Position(3, 7)
        val b = Position(3, 9)
        assertNotEquals(a, b)
    }

    @Test
    fun `isValid returns true for positions within map bounds`() {
        val pos = Position(5, 2)
        assertTrue(pos.isValid(10, 5))
    }

    @Test
    fun `isValid returns false for negative x`() {
        val pos = Position(-1, 2)
        assertFalse(pos.isValid(10, 5))
    }

    @Test
    fun `isValid returns false for x beyond width`() {
        val pos = Position(10, 2)
        assertFalse(pos.isValid(10, 5))
    }

    @Test
    fun `isValid returns false for negative y`() {
        val pos = Position(5, -1)
        assertFalse(pos.isValid(10, 5))
    }

    @Test
    fun `isValid returns false for y beyond height`() {
        val pos = Position(5, 5)
        assertFalse(pos.isValid(10, 5))
    }

    @Test
    fun `isValid with default bounds 10x5`() {
        assertTrue(Position(0, 0).isValid())
        assertTrue(Position(9, 4).isValid())
        assertFalse(Position(10, 4).isValid())
        assertFalse(Position(0, 5).isValid())
    }

    @Test
    fun `manhattan distance between two positions`() {
        val a = Position(1, 2)
        val b = Position(4, 6)
        assertEquals(7, a.manhattanDistance(b))
    }

    @Test
    fun `manhattan distance between same position is zero`() {
        val a = Position(3, 5)
        assertEquals(0, a.manhattanDistance(a))
    }

    @Test
    fun `copy creates new instance with same values`() {
        val original = Position(3, 7)
        val copied = original.copy()
        assertEquals(original, copied)
        assertNotSame(original, copied)
    }
}
