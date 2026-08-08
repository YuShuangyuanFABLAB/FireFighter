package com.example.firefighterterminal.presentation.ui.map.view

/**
 * 箭头方向枚举
 */
enum class ArrowDirection {
    LEFT, RIGHT, UP, DOWN,   // 单方向箭头
    WARNING,                  // 黄色警告标记 (DIR_NO_PATH)
    OFF                       // 熄灭
}

/**
 * 箭头计算结果
 *
 * @param primary 主箭头方向（一定有值）
 * @param secondary 副箭头方向（DIR_AT_EXIT 时有值，双箭头）
 * @param isDoubleGreen 是否为双绿（到达出口）
 * @param isYellowWarning 是否为黄色警告（被困）
 */
data class ArrowResult(
    val primary: ArrowDirection,
    val secondary: ArrowDirection? = null,
    val isDoubleGreen: Boolean = false,
    val isYellowWarning: Boolean = false
)

/**
 * 箭头方向计算器
 *
 * 根据灯牌类型和方向值，确定画布上应绘制的箭头朝向和颜色。
 * 纯逻辑，不依赖 Android Canvas。
 */
class ArrowCalculator {

    companion object {
        private const val DIR_OFF = 0
        private const val DIR_PRIMARY = 1
        private const val DIR_SECONDARY = 2
        private const val DIR_AT_EXIT = 3
        private const val DIR_NO_PATH = 4
    }

    /**
     * 计算箭头方向和颜色
     *
     * @param lightType 灯牌类型字符串 (HORIZONTAL_UP, VERTICAL_LEFT 等)
     * @param direction 方向值 (0-4)
     */
    fun compute(lightType: String, direction: Int): ArrowResult {
        // 兼容新旧格式: H* → 横向; V* → 纵向; 其他 → 默认横向
        val firstChar = lightType.firstOrNull() ?: 'H'
        val isHorizontal = (firstChar != 'V')

        return when (direction) {
            DIR_OFF -> ArrowResult(ArrowDirection.OFF)

            DIR_PRIMARY -> {
                val dir = if (isHorizontal) ArrowDirection.LEFT else ArrowDirection.UP
                ArrowResult(dir)
            }

            DIR_SECONDARY -> {
                val dir = if (isHorizontal) ArrowDirection.RIGHT else ArrowDirection.DOWN
                ArrowResult(dir)
            }

            DIR_AT_EXIT -> {
                if (isHorizontal) {
                    ArrowResult(ArrowDirection.LEFT, ArrowDirection.RIGHT, isDoubleGreen = true)
                } else {
                    ArrowResult(ArrowDirection.UP, ArrowDirection.DOWN, isDoubleGreen = true)
                }
            }

            DIR_NO_PATH -> {
                ArrowResult(ArrowDirection.WARNING, isYellowWarning = true)
            }

            else -> ArrowResult(ArrowDirection.OFF)
        }
    }
}
