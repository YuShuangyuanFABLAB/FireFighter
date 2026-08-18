# 消防员终端APP — 开发日志

> 项目：火灾智能动态疏散灯牌系统 · 宋庆龄少年儿童发明奖 2026 国赛升级
> 开发日期：2026-08-08 ~ 2026-08-09
> 开发方法：TDD (测试驱动开发)
> 总测试数：103+ (全部通过)

---

## 一、项目背景

在原有 ESP32 火灾疏散灯牌系统基础上，开发消防员 Android 终端 APP。消防员通过蓝牙连接 ESP32 主控，实时获取火场态势：
- 火灾点位置、灯牌方向、被困区域
- 救援优先级排序
- 最佳进攻路线 (A* 寻路)
- 语音播报同步、火灾时间线

基于开源模板 [esp32BluetoothAndroidTemplate](https://github.com/YuShuangyuanFABLAB/esp32BluetoothAndroidTemplate) 搭建。

---

## 二、开发环境搭建

### 2.1 初始状态

- 操作系统：Windows 11 Home China
- 无 JDK、无 Gradle、Android SDK 已在 `D:\ProgramData\AndroidSDK`
- 工作目录含中文字符 → Gradle 测试运行器 ClassNotFoundException

### 2.2 环境配置

| 步骤 | 操作 | 结果 |
|------|------|------|
| 1 | `winget install EclipseAdoptium.Temurin.17.JDK` | JDK 17 安装到 `C:\Program Files\Eclipse Adoptium\` |
| 2 | 设置 `JAVA_HOME` (需 Unix 风格路径 `/c/Program Files/...`) | `java -version` 成功 |
| 3 | 设置 `ANDROID_HOME=D:/ProgramData/AndroidSDK` | SDK 34 可用 |
| 4 | 创建 `local.properties` → `sdk.dir=D:/ProgramData/AndroidSDK` | AGP 识别 |
| 5 | `gradle.properties` 添加 `android.overridePathCheck=true` | 中文路径警告消除 |
| 6 | 项目移至 `D:\ProgramData\FirefighterApp\` (纯 ASCII 路径) | 测试可运行 |

### 2.3 遇到的问题

| 问题 | 根因 | 解决 |
|------|------|------|
| `ClassNotFoundException` (4个测试类) | 项目路径含中文，Gradle test runner 路径编码失败 | 移至 `D:\ProgramData\FirefighterApp\` |
| `PointF(Float,Float)` 返回值 x=0 | Kotlin-Java 互操作构造函数解析问题 | 改用 `PointF()` + setter |
| `RectF(Float,Float,Float,Float)` 同样问题 | 同上 | 改用 `RectF()` + setter |
| BLE BLE `BluetoothGattDescriptor` deprecation | 模板代码使用旧 API | 保留 (API 34 仍可用) |
| `BleSimulator` 引用不存在 | 模板代码依赖，我们不需要 | 从 `BleManagerRepository` 中删除 |

---

## 三、TDD 开发记录

### 3.1 完整测试清单

#### Position 坐标模型 (13 tests)
- `constructor stores x and y coordinates`
- `equal positions with same x and y`
- `not equal positions with different x/y`
- `isValid returns true/false for various bounds`
- `isValid with default bounds 10x5`
- `manhattan distance between two positions`
- `manhattan distance between same position is zero`
- `copy creates new instance with same values`

#### FireDataParser JSON 解析 (16 tests)
- `parse MAP_CONFIG message with walls and exits`
- `parse MAP_CONFIG walls include specific coordinates`
- `parse LIGHT_CONFIG message with all 13 lights`
- `parse FIRE_UPDATE with two fire points`
- `parse FIRE_UPDATE with empty fires array`
- `parse DIRECTION_UPDATE with all direction values`
- `parse VOICE_MODE idle / escape / rescue`
- `parse SENSOR_STATE with all 13 sensors`
- `parse HEARTBEAT message`
- `parse returns Unknown for invalid JSON / missing type / unknown type`

#### RescuePriorityCalculator 救援优先级 (9 tests)
- `trapped light (direction 4) gets highest priority P0`
- `light far from fire gets lower priority`
- `multiple fires affect priority scoring`
- `light on fire position scores higher than light far away`
- `result is sorted by score descending`
- `P0 is assigned only for score 100 or above`
- `P3 assigned when score is below 20`
- `empty fires gives all lights low priority`
- `each priority has a reason description`

#### GridCoordinateMapper 网格坐标映射 (17 tests)
- `total width/height equals sum + padding`
- `cell x/y offset is padding plus sum of previous widths/heights`
- `cell width/height equals configured col/row`
- `cell center x/y is left/top plus half`
- `cellRect returns correct bounds for a cell` ← **发现 RectF 构造 Bug**
- `cellCenter returns correct point for a cell` ← **发现 PointF 构造 Bug**
- `hit test identifies correct cell / returns null for padding / beyond bounds`
- `works with uniform grid sizes`
- `grid with single cell works`

#### ArrowCalculator 箭头方向 (15 tests)
- `horizontal light DIR_PRIMARY/DIR_SECONDARY → LEFT/RIGHT`
- `horizontal light DIR_AT_EXIT → double green arrows`
- `horizontal light DIR_NO_PATH → yellow warning`
- `vertical light DIR_PRIMARY/DIR_SECONDARY → UP/DOWN`
- `vertical light DIR_AT_EXIT → double green arrows up-down`
- `HORIZONTAL_DOWN behaves same as HORIZONTAL_UP`
- `VERTICAL_RIGHT behaves same as VERTICAL_LEFT`
- `unknown light type falls back to horizontal behavior`
- `unknown direction value falls back to OFF`

#### RenderColorResolver 颜色解析 (13 tests)
- `exit cell with/without adjacent fire → AVAILABLE/BLOCKED`
- `wall/floor/fire cell type resolution`
- `light color: direction 1 hasFire flashOn/Off → green/off`
- `light color: direction 4 trapped → yellow/off`
- `light color: direction 3 exit → double green`
- `light color: direction 0 off → always transparent`
- `fire gradient colors are red-orange range`
- `fire gradient has transparent end`
- `trapped wave color uses yellow-gold`

#### PathFinder A* 寻路 (12 tests)
- `findPath returns valid path between two walkable cells`
- `findPath returns null when start/end is wall`
- `findPath returns null when start is on fire`
- `findPath avoids fire cells` — 验证绕行
- `findPath returns null when fire blocks all routes`
- `start equals end returns single-point path`
- `path is shortest possible`
- `adjacent cells find direct path`
- `path steps are adjacent (no jumping)`
- `path on real 10x5 map finds route through corridor`
- `path blocked when fire at corridor bottleneck` ← **测试 Bug：断言过严，A\* 正确绕行**

#### DangerWarningCalculator 危险预警 (10 tests)
- `light with 2 adjacent fires gets WARNING`
- `light with only 1 adjacent fire is SAFE`
- `light on predicted spread path gets WARNING`
- `light on fire position / 3 adjacent fires → CRITICAL`
- `trapped light (direction 4) automatically gets CRITICAL`
- `multiple lights get individual warnings`
- `exit blocked raises warning for nearby lights`
- `empty fires gives all lights SAFE`
- `warning includes reason text`

### 3.2 TDD Bug 发现清单

| 类型 | 数量 | 详情 |
|------|------|------|
| 实现 Bug | 2 | `PointF`/`RectF` Kotlin-Java 构造器互操作 |
| 实现 Bug | 1 | `hitTest` `indexOfFirst` 边界计算 |
| 测试 Bug | 1 | 真实地图 A\* 路径预期过严 (算法正确绕行) |
| 测试 Bug | 1 | 墙壁集合含出口坐标 |

---

## 四、架构决策记录 (ADR)

### ADR-1: Canvas 自定义 View vs Compose

**决策**: 使用 Canvas 自定义 View (`FireMapView`)

**理由**:
- 模板技术栈为基础 (无 Compose 依赖)
- 非均匀格子需要精确像素控制
- 5 层渲染管线(网格→预警→路线→火点→灯牌)与 Canvas.save/restore 天然匹配
- 手势处理更灵活 (`ScaleGestureDetector` + `GestureDetector`)

### ADR-2: 非均匀网格配置化

**决策**: `colWidths[10]` 和 `rowHeights[5]` 通过 `device_config.json` 配置

**理由**:
- 物理沙盘格子实际尺寸不一致
- 不需要改代码即可调整视觉比例
- `GridCoordinateMapper` 使用累加偏移缓存计算

### ADR-3: JSON 消息协议 vs 二进制

**决策**: ESP32 → APP 使用换行分隔的 JSON

**理由**:
- ESP32 端手写字符串拼接，无需 ArduinoJson 库
- APP 端使用 `kotlinx.serialization.json` 解析
- 人类可读，调试方便 (串口可见)
- MTU 512 字节足够 (地图配置 ~800 字节需分包)

### ADR-4: 闪烁状态由 APP 端控制

**决策**: 灯光闪烁 (`flashOn`) 由 APP 端 `ValueAnimator` 驱动，而非 ESP32 推送

**理由**:
- BLE 推送频率 50-100ms 难以精确控制 500ms 闪烁周期
- APP 端 `animPhase < 0.5f` 精确控制闪烁
- 减少 BLE 带宽占用

### ADR-5: A\* 在 ESP32 和 Kotlin 双实现

**决策**: 两套独立的 A\* 实现

**理由**:
- ESP32: 驱动 LED 灯牌方向 (实时控制)
- Kotlin: 消防员进攻路线计算 (APP 端显示，无需占用 BLE 带宽)
- 不耦合：关闭 APP 不影响 ESP32 正常工作

---

## 五、最终项目结构

```
D:\ProgramData\FirefighterApp\
│
├── app/src/main/java/com/example/firefighterterminal/
│   ├── MainActivity.kt                     # ViewPager2 三页壳
│   ├── MainPagerAdapter.kt                 # 页面适配器
│   ├── DataStoreManager.kt                 # 火场数据持久化
│   │
│   ├── data/
│   │   ├── ble/
│   │   │   ├── BleManager.kt               # BLE 扫描/连接/读写
│   │   │   ├── BleManagerRepository.kt     # 单例仓库
│   │   │   ├── FireMessage.kt              # 7种消息类型 (sealed class)
│   │   │   └── FireDataParser.kt           # JSON 解析器
│   │   ├── config/
│   │   │   └── ConfigLoader.kt             # JSON 配置加载
│   │   └── repository/
│   │       ├── FireDataRepository.kt        # 火场数据状态中心
│   │       ├── FireDataSnapshot.kt          # 数据快照
│   │       ├── PathFinder.kt               # A* 寻路 (Kotlin)
│   │       ├── RescuePriorityCalculator.kt  # 救援优先级 (P0-P3)
│   │       └── DangerWarningCalculator.kt   # 危险预警 (SAFE/WARNING/CRITICAL)
│   │
│   ├── domain/model/
│   │   ├── Position.kt                     # 坐标值对象 (不可变)
│   │   ├── IotDevice.kt                    # BLE 设备
│   │   ├── DeviceConfig.kt                 # 设备配置模型
│   │   └── RescuePriority.kt               # 救援优先级 + PriorityLevel
│   │
│   ├── presentation/
│   │   ├── ui/
│   │   │   ├── device/
│   │   │   │   ├── DeviceListFragment.kt   # BLE 扫描页
│   │   │   │   └── DeviceViewModel.kt
│   │   │   ├── map/
│   │   │   │   ├── FireMapFragment.kt      # 火场地图页
│   │   │   │   ├── FireMapViewModel.kt
│   │   │   │   └── view/
│   │   │   │       ├── FireMapView.kt      # Canvas 5层 + 手势
│   │   │   │       ├── GridCoordinateMapper.kt  # 非均匀网格映射
│   │   │   │       ├── GridRenderer.kt     # Layer 0: 墙壁/出口
│   │   │   │       ├── WarningRenderer.kt  # Layer 1: 被困波纹
│   │   │   │       ├── PathRenderer.kt     # Layer 2: 进攻路线
│   │   │   │       ├── FireRenderer.kt     # Layer 3: 火点辉光
│   │   │   │       ├── LightRenderer.kt    # Layer 4: 灯牌箭头
│   │   │   │       ├── ArrowCalculator.kt  # 箭头方向计算
│   │   │   │       └── RenderColorResolver.kt # 颜色/闪烁/渐变
│   │   │   └── analysis/
│   │   │       ├── AnalysisFragment.kt     # 分析面板页
│   │   │       └── AnalysisViewModel.kt    # 时间线/语音/趋势/出口
│   │   └── adapter/
│   │       ├── DeviceListAdapter.kt        # BLE 设备列表
│   │       └── RescuePriorityAdapter.kt    # 救援优先级列表
│   │
│   ├── res/
│   │   ├── layout/ (4个)
│   │   ├── values/ (colors.xml, strings.xml, themes.xml)
│   │   └── mipmap/ (ic_launcher)
│   └── assets/config/device_config.json
│
├── app/src/test/ (8个测试类, 103+ tests)
│
├── esp32_firmware/
│   ├── fire_ctrl/fire_ctrl.ino             # ★ 完整固件 v2.1 (BLE集成)
│   └── fire_ble_extension/fire_ble_extension.ino  # BLE 扩展参考
│
├── docs/
│   ├── superpowers/specs/firefighter-app-spec.md
│   ├── superpowers/plans/firefighter-app-plan.md
│   └── DEVELOPMENT_LOG.md                  # 本文档
│
└── gradle/ (build system)
```

---

## 六、重要设定 (Key Design Decisions)

### 6.1 Android 蓝牙权限 (API 31+ 分叉)

Android 12 (API 31) 起蓝牙权限模型变更，必须在运行时区分处理：

```kotlin
// API 31+: 需要 BLUETOOTH_SCAN + BLUETOOTH_CONNECT
// API 30-: 需要 BLUETOOTH + BLUETOOTH_ADMIN
// 所有版本: 都需要 ACCESS_FINE_LOCATION (BLE 扫描依赖)
```

**设定**: `MainActivity.requestBluetoothPermissions()` 根据 `Build.VERSION.SDK_INT` 动态选择权限列表，使用 `ActivityResultContracts.RequestMultiplePermissions()` 统一处理回调。

### 6.2 BLE JSON 协议设计

| 设定 | 值 | 原因 |
|------|-----|------|
| 分隔符 | 换行 `\n` | ESP32 `println()` 自动追加，解析端按行切分 |
| MTU | 512 字节 | ESP32 BLE 默认 MTU，单条消息必须 < 512B |
| 消息标识 | `"type"` 字段 | 7 种类型通过 `when(type)` 分发 |
| 灯牌类型名 | 保留全名 `HORIZONTAL_UP` 等 | 单条消息不超 MTU，暂未缩短 |
| JSON 库 | APP: kotlinx.serialization, ESP32: 手写字符串拼接 | ESP32 内存受限，避免引入 ArduinoJson |

**关键细节**: ESP32 连接后延时 2 秒才推送静态配置，确保 APP 端 BLE 特征值已就绪：

```
连接事件 → delay 2s → MAP_CONFIG → delay 500ms → LIGHT_CONFIG
→ delay 500ms → FIRE_UPDATE + DIRECTION_UPDATE
→ 之后 SENSOR_STATE 每500ms, HEARTBEAT 每5s
```

### 6.3 Canvas 旋转 90° 适配竖屏

火场沙盘是 10×5 横向布局，但 APP 竖屏显示。**设定**: Canvas 渲染前旋转 90° (`Matrix.postRotate(90, cx, cy)`)，让横向地图填满竖屏宽度。所有渲染器在逻辑坐标系 (x=0..9, y=0..4) 工作，`GridCoordinateMapper` 负责逻辑坐标 → 像素映射。

```kotlin
// FireMapView.onDraw():
canvas.save()
canvas.concat(transform)  // transform 包含 90°旋转 + scale + translate
// 5层渲染...
canvas.restore()
```

### 6.4 动画系统设计

| 动画 | 实现 | 周期 | 触发条件 |
|------|------|------|---------|
| 火点辉光脉冲 | `ValueAnimator` + `RadialGradient` alpha 变化 | 2000ms | fireCount > 0 |
| 灯牌箭头闪烁 | `animPhase < 0.5f` 判断 | 500ms (等效) | fireCount > 0, 方向=1/2/3 |
| 被困波纹扩散 | `WarningRenderer` 多层 `RadialGradient` | 1500ms | 存在 direction=4 的灯牌 |
| 出口呼吸灯 | `GridRenderer` 绿色虚线 + alpha 变化 | 3000ms | 始终运行 |
| 路线虚线流动 | `DashPathEffect` + `dashPhase` 递增 | 连续 | 有进攻路线时 |

**关键设定**: 动画仅在 fireCount > 0 时运行，无火情自动停止以节省资源。

### 6.5 DataStore 持久化策略

```
firefighter_data (Preferences DataStore)
├── last_device_address  → 最后连接的 BLE 设备 MAC
├── fire_timeline        → 火灾时间线 (每条带时间戳，最新在前，上限 10000 字符)
└── map_config           → 地图配置 JSON (备份)
```

**设定**: DataStore 而非 Room/SQLite — 数据结构简单 (键值对)，无需关系查询，DataStore 的 Flow API 与 MVVM 天然集成。

### 6.6 配置文件加载架构

`device_config.json` 从 `app/src/main/assets/config/` 加载，使用 `kotlinx.serialization.json.decodeFromStream()` 直接反序列化为 `DeviceConfig` 数据类。

```json
{
  "appConfig":    { scanDuration, autoReconnect, reconnectInterval, maxReconnectAttempts },
  "bleConfig":    { services[].uuid + characteristics[].uuid/type/dataFormat, deviceFilters },
  "mapLayout":    { cols, rows, colWidths[], rowHeights[], 颜色, padding }
}
```

**设定**: `ConfigLoader.loadDeviceConfigSafely()` 提供安全版本，解析失败返回 null 而不崩溃。

### 6.7 命令系统二次确认

所有破坏性操作 (SYSTEM_RESET, REMOVE_FIRE:ALL) 在分析面板触发前弹出确认对话框，防止误触。

---

## 七、经验教训

### 7.1 技术

1. **Kotlin-Java 互操作陷阱**: `PointF(Float, Float)` 和 `RectF(Float, Float, Float, Float)` 在 Kotlin 中构造函数解析有问题，返回值全为默认值。**解决方案**: 使用无参构造 + setter。**影响范围**: `GridCoordinateMapper.cellRect()` 和 `cellCenter()` 两个方法。

2. **Gradle + 中文路径不兼容**: Android Gradle Plugin 的测试运行器无法处理非 ASCII 路径。现象是 `ClassNotFoundException`（4 个测试类同时找不到）。**解决方案**: 项目从 `E:\FABLAB 法贝实验室\...` 移至 `D:\ProgramData\FirefighterApp\`。**教训**: 所有开发路径必须纯 ASCII。

3. **配置驱动的价值**: 从模板继承的 `device_config.json` 模式让地图布局 (`colWidths`/`rowHeights`)、BLE UUID、颜色方案全部可配置，物理沙盘调整时无需改代码。

4. **Canvas 渲染管道分层**: 5 层独立渲染器 (Grid→Warning→Path→Fire→Light) + 1 层 PriorityOverlay 设计让每层可独立开发、测试、启用/禁用，复杂度可控。每层渲染器是无状态的纯函数，接收数据参数，不持有可变状态。

5. **Kotlin sealed class + when 穷举**: `FireMessage` 密封类 7 个子类，`FireDataParser` 的 `when(type)` 在编译期保证所有消息类型被处理。新增消息类型时编译器会报错提醒。

6. **LiveData 而非 StateFlow**: 项目选择 LiveData 而非 Kotlin StateFlow — 模板代码已使用 LiveData，且 LiveData 自动感知生命周期，在 Fragment 销毁时自动取消订阅，避免内存泄漏。

7. **ViewPager2 + offscreenPageLimit=2**: 三页全部保持活跃，地图页在后台也能接收 BLE 数据更新，切回时无需重新渲染。

### 7.2 流程

8. **TDD 发现了 4 个真实 Bug**: `PointF`/`RectF` 互操作、`hitTest` 边界、地图墙壁数据含出口坐标、A\* 测试断言过严。如果没有先写测试，这些问题可能在硬件联调时才暴露。

9. **Spec 先行减少了返工**: 在头脑风暴阶段确认了功能清单和设计决策 (5 个 ADR)，避免了开发中途的需求变更。**关键**: ADR 在写代码前记录，而非事后补写。

10. **环境搭建占 30% 时间**: JDK 安装、SDK 路径查找、中文路径问题、资源文件迁移等环境问题消耗了大量时间。建议新项目一开始就用纯 ASCII 路径。

11. **模板复用效率**: 基于 `esp32BluetoothAndroidTemplate` 搭建，BLE 扫描/连接/读写层直接迁移，只需修改设备名过滤 (`FIRE_CTRL`) 和 JSON 解析逻辑。**经验**: 维护可复用的模板项目对后续开发至关重要。

### 7.3 跨平台 (Kotlin ↔ ESP32 C++)

12. **双 A\* 实现注意事项**: ESP32 端 `findPath()` 返回完整路径用于 LED 驱动；Kotlin 端 `PathFinder.findPath()` 返回消防员进攻路线。两套实现必须使用相同的启发式 (曼哈顿距离) 和代价函数 (g=1/步)，确保结果一致。**验证方法**: 对同一地图数据分别运行两端 A\*，比较路径。

13. **JSON 字段命名一致性**: ESP32 `sendJson()` 手写拼接的字段名必须与 Kotlin `FireDataParser` 解析的字段名严格一致。**踩坑**: 早期版本 ESP32 用 `"direction"` 单数，Kotlin 期望 `"directions"` 复数 → 解析失败。

14. **BLE 重新广播机制**: ESP32 断连后 `delay(500)` 再 `startAdvertising()`。500ms 延迟是必要的 — 立即重启广播可能导致 Android BLE 栈无法识别设备。

---

## 八、踩坑总结 (Pitfalls)

### 8.1 环境类

| 序号 | 问题 | 症状 | 根因 | 解决 |
|------|------|------|------|------|
| P1 | `ClassNotFoundException` (4 测试类) | `gradlew test` 报找不到测试类 | 项目路径含中文字符 | 移至纯 ASCII 路径 `D:\ProgramData\FirefighterApp\` |
| P2 | `java` 命令找不到 | `bash: java: command not found` | `JAVA_HOME` 未设置，或用了反斜杠路径 | `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.8-hotspot"` (Unix 风格正斜杠) |
| P3 | AGP 找不到 SDK | `SDK location not found` | `local.properties` 未创建 | 创建文件写入 `sdk.dir=D:/ProgramData/AndroidSDK` |
| P4 | Gradle 中文路径警告 | 编译警告但不影响构建 | `android.overridePathCheck` 未设置 | `gradle.properties` 添加 `android.overridePathCheck=true` |

### 8.2 Kotlin-Java 互操作类

| 序号 | 问题 | 症状 | 根因 | 解决 |
|------|------|------|------|------|
| P5 | `PointF(Float,Float)` x=0, y=0 | 点击测试返回错误坐标 | Kotlin 调用 Java 构造函数 `PointF(float,float)` 时参数未正确传递 | 改用 `PointF().apply { x=...; y=... }` |
| P6 | `RectF(Float,Float,Float,Float)` 全为 0 | `cellRect` 始终返回 (0,0,0,0) | 同 P5 — Kotlin-Java float 构造函数互操作 Bug | 改用 `RectF().apply { left=...; top=...; right=...; bottom=... }` |

### 8.3 数据与算法类

| 序号 | 问题 | 症状 | 根因 | 解决 |
|------|------|------|------|------|
| P7 | `hitTest` 越界 | 点击地图边缘 cell 返回错误结果 | `indexOfFirst` 边界索引偏移 1 | 修正边界计算逻辑 |
| P8 | 地图墙壁数据含出口坐标 | A\* 路径测试随机失败 | `device_config.json` 中墙壁列表包含了出口位置 | 从 walls 中移除出口坐标 |
| P9 | A\* 测试「path blocked at corridor bottleneck」断言过严 | 测试失败但算法正常输出绕行路径 | 测试预期路径被火堵死，但 A\* 找到了另一条绕行路线 | 修改测试预期，改为验证路径存在且避开火点 |

### 8.4 BLE 通信类

| 序号 | 问题 | 症状 | 根因 | 解决 |
|------|------|------|------|------|
| P10 | `BleSimulator` 引用不存在 | `BleManagerRepository.kt` 编译失败 | 模板代码包含模拟器引用，项目不需要 | 从仓库中删除 BleSimulator 相关代码 |
| P11 | `BluetoothGattDescriptor` deprecation | IDE 黄色警告 | 模板代码使用 API 33 已弃用的方法 | 保留 — API 34 仍可用，且替代 API 需要 minSdk 33 |

### 8.5 Git 与工具类

| 序号 | 问题 | 症状 | 根因 | 解决 |
|------|------|------|------|------|
| P12 | `git push` 在 Git Bash 失败 | 连接超时 / 443 错误 | Windows 系统代理仅在 PowerShell 生效，Git Bash 不走代理 | 使用 `powershell.exe -Command "cd 'D:\ProgramData\FirefighterApp'; git push"` |
| P13 | `.gitignore` 未排除构建产物 | `git status` 显示大量 build/ 文件 | 模板 .gitignore 不完整 | 添加 `*.apk`, `*.aab`, `build/`, `.gradle/`, `local.properties`, `.idea/` |

---

## 九、Git Push 特别说明

Windows 环境下 Git Bash (MSYS2) 不走系统代理，导致 `git push` 超时。解决方法：

```bash
# ❌ Git Bash 直连 — 超时
git push

# ✅ 通过 PowerShell 推送 (系统代理生效)
powershell.exe -Command "cd 'D:\ProgramData\FirefighterApp'; git push 2>&1"

# ✅ 或配置 Git 使用系统代理
git config --global http.proxy http://127.0.0.1:<port>
git config --global https.proxy http://127.0.0.1:<port>
```

---

## 十、待办事项 (Phase 7 联调测试)

- [ ] **7.1** 端到端功能测试 (无火/单火/多火/灭火/命令)
- [ ] **7.2** 地图精确校准 (colWidths/rowHeights 物理对齐)
- [ ] **7.3** BLE 性能测试 (延迟/稳定性/断连重连)
- [ ] **7.4** 边界情况测试 (压力/MTU/横竖屏)
- [ ] **7.5** 代码清理与文档

---

## 十一、版本历史

| 版本 | 日期 | 内容 |
|------|------|------|
| v0.1 | 2026-08-08 晚 | 项目骨架搭建、JDK/SDK 环境配置 |
| v0.2 | 2026-08-08 深夜 | TDD 核心算法 (Position, Parser, Priority, Mapper) |
| v0.3 | 2026-08-09 凌晨 | 渲染管线 (5层 Canvas) + 箭头/颜色逻辑 |
| v0.4 | 2026-08-09 上午 | A\* 寻路 + 危险预警 + 手势交互 |
| v0.5 | 2026-08-09 下午 | 分析面板完善 + DataStore + Adapters + ESP32 固件 |
| **v1.0.0** | **2026-08-18** | **★ 关键版本: 发布 — vivo OriginOS 6 真机验证运行正常** (BLE连接/三页架构可用, git tag `v1.0.0`) |
| doc v2.0 | 2026-08-09 | 文档升级: 补充重要设定(6项)、经验教训(14条)、踩坑总结(13项 P1-P13) |

---

*文档版本: v2.0 | 创建日期: 2026-08-09 | 最后更新: 2026-08-18 (v1.0.0 关键版本发布: vivo OriginOS 6 真机验证)*
