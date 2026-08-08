package com.example.firefighterterminal

import com.example.firefighterterminal.domain.model.Position
import com.example.firefighterterminal.data.repository.PathFinder
import org.junit.Assert.*
import org.junit.Test

/**
 * A* 寻路算法测试
 *
 * 验证与 ESP32 端 findPath() 行为一致
 */
class PathFinderTest {

    // 简化地图: 5×5, 边缘为墙, 中间通道
    private fun simpleWalls(): Set<Position> {
        val walls = mutableSetOf<Position>()
        for (x in 0..4) { walls.add(Position(x, 0)); walls.add(Position(x, 4)) }
        for (y in 1..3) { walls.add(Position(0, y)); walls.add(Position(4, y)) }
        return walls
    }

    @Test
    fun `findPath returns valid path between two walkable cells`() {
        val walls = simpleWalls()
        val fires = emptySet<Position>()
        val finder = PathFinder(5, 5, walls, fires)

        val path = finder.findPath(Position(1, 1), Position(3, 3))

        assertNotNull(path)
        assertTrue(path!!.size >= 2)
        assertEquals(Position(1, 1), path.first())
        assertEquals(Position(3, 3), path.last())
    }

    @Test
    fun `findPath returns null when start is wall`() {
        val walls = simpleWalls()
        val finder = PathFinder(5, 5, walls, emptySet())

        val path = finder.findPath(Position(0, 0), Position(3, 3))
        assertNull(path)
    }

    @Test
    fun `findPath returns null when end is wall`() {
        val walls = simpleWalls()
        val finder = PathFinder(5, 5, walls, emptySet())

        val path = finder.findPath(Position(1, 1), Position(0, 0))
        assertNull(path)
    }

    @Test
    fun `findPath returns null when start is on fire`() {
        val walls = simpleWalls()
        val fires = setOf(Position(1, 1))
        val finder = PathFinder(5, 5, walls, fires)

        val path = finder.findPath(Position(1, 1), Position(3, 3))
        assertNull(path)
    }

    @Test
    fun `findPath avoids fire cells`() {
        val walls = simpleWalls()
        val fires = setOf(Position(2, 2))  // 挡住直接路径
        val finder = PathFinder(5, 5, walls, fires)

        val path = finder.findPath(Position(1, 1), Position(3, 3))

        assertNotNull(path)
        // 路径不能经过火点
        path!!.forEach { pos -> assertFalse(fires.contains(pos)) }
    }

    @Test
    fun `findPath returns null when fire blocks all routes`() {
        val walls = simpleWalls()
        // 火点堵死唯一通道
        val fires = setOf(Position(1, 2), Position(2, 1), Position(2, 2), Position(2, 3), Position(3, 2))
        val finder = PathFinder(5, 5, walls, fires)

        val path = finder.findPath(Position(1, 1), Position(3, 3))
        assertNull(path)
    }

    @Test
    fun `start equals end returns single-point path`() {
        val finder = PathFinder(5, 5, emptySet(), emptySet())

        val path = finder.findPath(Position(2, 2), Position(2, 2))

        assertNotNull(path)
        assertEquals(1, path!!.size)
        assertEquals(Position(2, 2), path[0])
    }

    @Test
    fun `path is shortest possible`() {
        val walls = simpleWalls()
        val finder = PathFinder(5, 5, walls, emptySet())

        val path = finder.findPath(Position(1, 1), Position(3, 1))

        assertNotNull(path)
        // 最短路径应为 (1,1)→(2,1)→(3,1)，长度 3
        assertEquals(3, path!!.size)
    }

    @Test
    fun `adjacent cells find direct path`() {
        val finder = PathFinder(5, 5, emptySet(), emptySet())

        val path = finder.findPath(Position(1, 1), Position(2, 1))

        assertNotNull(path)
        assertEquals(2, path!!.size)
        assertEquals(Position(1, 1), path[0])
        assertEquals(Position(2, 1), path[1])
    }

    @Test
    fun `path steps are adjacent (no jumping)`() {
        val finder = PathFinder(10, 5, emptySet(), emptySet())

        val path = finder.findPath(Position(1, 1), Position(8, 3))

        assertNotNull(path)
        for (i in 1 until path!!.size) {
            val dx = kotlin.math.abs(path[i].x - path[i-1].x)
            val dy = kotlin.math.abs(path[i].y - path[i-1].y)
            assertTrue("Step $i: ($dx,$dy) is not adjacent", dx + dy == 1)
        }
    }

    // ==================== 基于真实地图的测试 ====================

    @Test
    fun `path on real 10x5 map finds route through corridor`() {
        val walls = setOf(
            Position(0,0),Position(1,0),Position(2,0),Position(3,0),Position(4,0),
            Position(6,0),Position(7,0),Position(8,0),Position(9,0),
            Position(0,1),Position(9,1),
            Position(0,2),Position(1,2),Position(3,2),Position(4,2),Position(6,2),Position(8,2),Position(9,2),
            Position(0,4),Position(1,4),Position(2,4),Position(3,4),Position(5,4),Position(6,4),Position(7,4),Position(8,4),Position(9,4)
        )
        val finder = PathFinder(10, 5, walls, emptySet())

        // 从 L1(0,3) 到出口 E3(9,3)
        val path = finder.findPath(Position(0, 3), Position(9, 3))

        assertNotNull(path)
        assertTrue(path!!.size > 2)
        assertEquals(Position(0, 3), path.first())
        assertEquals(Position(9, 3), path.last())
    }

    @Test
    fun `path blocked when fire at corridor bottleneck`() {
        val walls = setOf(
            Position(0,0),Position(1,0),Position(2,0),Position(3,0),Position(4,0),
            Position(6,0),Position(7,0),Position(8,0),Position(9,0),
            Position(0,1),Position(9,1),
            Position(0,2),Position(1,2),Position(3,2),Position(4,2),Position(6,2),Position(8,2),Position(9,2),
            Position(0,4),Position(1,4),Position(2,4),Position(3,4),Position(5,4),Position(6,4),Position(7,4),Position(8,4),Position(9,4)
        )
        // 火点在主疏散通道关键位置：堵住瓶颈 (2,2) 和 (7,2) 切断上下联通
        val fires = setOf(Position(2, 2), Position(5, 3), Position(7, 2))
        val finder = PathFinder(10, 5, walls, fires)

        val path = finder.findPath(Position(0, 3), Position(9, 3))
        assertNull(path)  // 上/下通道都被堵死，无法到达右侧
    }
}
