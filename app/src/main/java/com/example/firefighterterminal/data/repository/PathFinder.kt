package com.example.firefighterterminal.data.repository

import com.example.firefighterterminal.domain.model.Position

/**
 * A* 寻路算法 (Kotlin 实现)
 *
 * 与 ESP32 端 findPath() 行为一致：
 * - 启发式：曼哈顿距离
 * - 代价：每步 g=1
 * - 不可通行：墙壁 + 火灾点
 * - 开放集线性扫描（地图小，O(50) 比堆 O(log50) 更简单）
 */
class PathFinder(
    private val width: Int,
    private val height: Int,
    private val walls: Set<Position>,
    private val fires: Set<Position>
) {
    companion object {
        private const val INF = 10000
    }

    private fun isWalkable(x: Int, y: Int): Boolean {
        if (x < 0 || x >= width || y < 0 || y >= height) return false
        val pos = Position(x, y)
        return !walls.contains(pos) && !fires.contains(pos)
    }

    /**
     * 查找从 start 到 end 的最短路径
     *
     * @return 路径坐标数组（包含起点和终点），不可达返回 null
     */
    fun findPath(start: Position, end: Position): List<Position>? {
        if (!isWalkable(start.x, start.y) || !isWalkable(end.x, end.y)) return null
        if (start == end) return listOf(start)

        val gScore = Array(height) { IntArray(width) { INF } }
        val fScore = Array(height) { IntArray(width) { INF } }
        val parentX = Array(height) { IntArray(width) { -1 } }
        val parentY = Array(height) { IntArray(width) { -1 } }
        val closed = Array(height) { BooleanArray(width) { false } }

        gScore[start.y][start.x] = 0
        fScore[start.y][start.x] = manhattan(start, end)

        val dirs = arrayOf(intArrayOf(0, -1), intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 0))

        while (true) {
            // 从开放集找最小 f 值
            var bestF = INF; var cx = -1; var cy = -1
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (!closed[y][x] && gScore[y][x] < INF && fScore[y][x] < bestF) {
                        bestF = fScore[y][x]; cx = x; cy = y
                    }
                }
            }
            if (cx == -1) return null // 无可达路径

            if (cx == end.x && cy == end.y) {
                // 回溯路径
                val rev = mutableListOf<Position>()
                var px = end.x; var py = end.y
                while (px != start.x || py != start.y) {
                    rev.add(Position(px, py))
                    val nx = parentX[py][px]; val ny = parentY[py][px]
                    px = nx; py = ny
                }
                rev.add(start)
                return rev.reversed()
            }

            closed[cy][cx] = true

            for (d in dirs) {
                val nx = cx + d[0]; val ny = cy + d[1]
                if (!isWalkable(nx, ny) || closed[ny][nx]) continue
                val tg = gScore[cy][cx] + 1
                if (tg < gScore[ny][nx]) {
                    parentX[ny][nx] = cx; parentY[ny][nx] = cy
                    gScore[ny][nx] = tg
                    fScore[ny][nx] = tg + manhattan(Position(nx, ny), end)
                }
            }
        }
    }

    private fun manhattan(a: Position, b: Position): Int =
        kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y)
}
