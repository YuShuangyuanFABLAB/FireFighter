# 消防员终端APP — 实施计划 v1.0

> 基于 Spec v1.0 | 实施日期：2026-08-08 ~ 2026-08-09
> 开发方法：TDD (测试驱动开发)

---

## 总览

| Phase | 内容 | 任务数 | 状态 |
|-------|------|--------|:--:|
| Phase 1 | 项目搭建 + BLE 通信 | 8 | ✅ 完成 |
| Phase 2 | 地图渲染核心 | 10 | ✅ 完成 |
| Phase 3 | 动态动画系统 | 6 | ✅ 完成 |
| Phase 4 | 分析面板 | 8 | ✅ 完成 |
| Phase 5 | 高级功能 (寻路/预警/持久化) | 7 | ✅ 完成 |
| Phase 6 | ESP32 固件扩展 | 6 | ✅ 完成 |
| Phase 7 | 联调测试 | 5 | ⬜ 待做 |

---

## Phase 1: 项目搭建 + BLE 通信迁移

- [x] **1.1** 创建 Android 项目骨架 (Gradle/依赖/命名空间)
- [x] **1.2** 数据模型 (Position, MapConfig, LightConfig, FireData, DeviceConfig)
- [x] **1.3** 迁移 BLE 通信层 (BleManager, BleManagerRepository → 改 `FIRE_CTRL` 过滤)
- [x] **1.4** 重写 BleServiceManager → FireDataParser (7种 JSON 消息解析)
- [x] **1.5** 创建 FireDataRepository (全局状态中心 + LiveData)
- [x] **1.6** MainActivity + MainPagerAdapter (ViewPager2 三页壳)
- [x] **1.7** 创建 device_config.json (BLE配置 + mapLayout)
- [x] **1.8** 基础连接流程 (扫描→连接→静态配置→状态推送)

## Phase 2: 地图渲染核心

- [x] **2.1** FireMapView 骨架 (Canvas 自定义 View)
- [x] **2.2** GridCoordinateMapper (非均匀网格像素映射, 17 tests)
- [x] **2.3** GridRenderer (墙壁砖纹理/通道/出口辉光/网格线)
- [x] **2.4** FireRenderer (红色径向辉光 + 脉动动画)
- [x] **2.5** LightRenderer (底座 + 方向箭头 + 警告三角)
- [x] **2.6** FireMapFragment + FireMapViewModel
- [x] **2.7** 数据刷新管道 (FireDataRepository → LiveData → FireMapView)
- [x] **2.8** 灯牌点击交互 (hitTest + onLightSelected 回调)
- [x] **2.9** 地图缩放/平移 (ScaleGestureDetector + GestureDetector)
- [x] **2.10** 双击复位

## Phase 3: 动画系统

- [x] **3.1** 火点辉光脉冲 (ValueAnimator, 2000ms 周期)
- [x] **3.2** 被困区域波纹扩散 (WarningRenderer + RadialGradient)
- [x] **3.3** 灯牌箭头闪烁 (APP 端 500ms 周期: `animPhase < 0.5f`)
- [x] **3.4** 出口呼吸灯 (GridRenderer 绿色虚线边框)
- [x] **3.5** 进攻路线虚线流动 (DashPathEffect + dashPhase)
- [x] **3.6** 动画仅在火情时运行 (省资源)

## Phase 4: 分析面板

- [x] **4.1** AnalysisFragment 骨架 + ViewBinding
- [x] **4.2** AnalysisViewModel (数据绑定)
- [x] **4.3** 救援优先级算法 (RescuePriorityCalculator, 9 tests)
- [x] **4.4** RescuePriorityAdapter (RecyclerView + 颜色编码)
- [x] **4.5** 火灾时间线 (追加式 + 变化检测)
- [x] **4.6** 语音播报文字 (IDLE/ESCAPE/RESCUE 三模式)
- [x] **4.7** 火势趋势 (火点质心方向推断)
- [x] **4.8** 快捷控制 (系统复位/清除火点, 二次确认)

## Phase 5: 高级功能

- [x] **5.1** PathFinder A\* 寻路 (Kotlin 实现, 12 tests)
- [x] **5.2** PathRenderer 进攻路线渲染 (蓝色虚线)
- [x] **5.3** DangerWarningCalculator 危险预警 (10 tests)
- [x] **5.4** ArrowCalculator 箭头方向映射 (15 tests)
- [x] **5.5** 出口状态计算 + 显示
- [x] **5.6** DataStoreManager 数据持久化 (设备地址/时间线/地图配置)
- [x] **5.7** 深色消防主题 (colors.xml / themes.xml)

## Phase 6: ESP32 固件扩展

- [x] **6.1** BLE Server 初始化 (Service + Notify + Write 特征值)
- [x] **6.2** JSON 推送函数 (7种消息类型, 字符串拼接)
- [x] **6.3** 连接时推送静态配置 (MAP_CONFIG + LIGHT_CONFIG + 当前状态)
- [x] **6.4** 状态变化推送 (FIRE_UPDATE + DIRECTION_UPDATE + VOICE_MODE)
- [x] **6.5** 命令接收处理 (SET_LIGHT/ADD_FIRE/REMOVE_FIRE/SYSTEM_RESET)
- [x] **6.6** 完整集成固件 `fire_ctrl.ino` (v2.1)

## Phase 7: 联调测试

- [ ] **7.1** 端到端功能测试 (无火/单火/多火/灭火/命令)
- [ ] **7.2** 地图精确校准 (colWidths/rowHeights 物理对齐)
- [ ] **7.3** BLE 性能测试 (延迟/稳定性/断连重连)
- [ ] **7.4** 边界情况测试 (压力/MTU/横竖屏)
- [ ] **7.5** 代码清理与文档

---

## 依赖关系

```
Phase 1 (搭建) → Phase 2 (渲染) → Phase 3 (动画)
                  ↓                    ↓
Phase 4 (分析) ← (并行)           Phase 5 (高级)
                                       ↓
Phase 6 (ESP32) ← (并行)           Phase 7 (联调)
```

---

## 技术风险与缓解

| 风险 | 概率 | 缓解措施 | 实际结果 |
|------|------|---------|---------|
| Kotlin-Java 互操作构造函数 Bug | 低 | - | **已发生**: PointF/RectF 改 setter |
| 中文路径 Gradle 不兼容 | 中 | 纯 ASCII 路径 | **已发生**: 移至 D:\ProgramData |
| BLE JSON 超 MTU | 中 | 分包 + 换行分隔 | 待联调验证 |
| Canvas 动画性能不足 | 低 | 仅在变化时 invalidate | 未测试 |

---

*计划版本: v1.0 | 日期: 2026-08-09*
