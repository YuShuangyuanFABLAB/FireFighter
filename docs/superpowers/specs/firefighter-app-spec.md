# 消防员终端APP — 设计规范 v1.0

> 项目：火灾智能动态疏散灯牌系统 · 宋庆龄少年儿童发明奖 2026 国赛升级
> 日期：2026-08-09
> 状态：已实现

---

## 一、项目目标

为消防员开发 Android BLE 终端 APP，通过蓝牙连接 ESP32 主控，实时获取火场态势：

1. **火灾点位置** — 哪里着火了
2. **被困区域识别** — 哪些区域需要第一时间救援（黄闪灯牌）
3. **最佳进攻路线** — 从消防员入口到达火点的最短安全路径 (A\*)
4. **火势发展趋势** — 传感器触发时序推断蔓延方向
5. **语音播报同步** — APP 端显示疏散/救援指引文字

---

## 二、系统架构

```
ESP32 主控 (v2.1 BLE 集成)
  ├── 原系统: 传感器扫描 / A* 寻路 / LED 驱动 / 语音控制
  └── BLE Server: 实时推送 JSON + 接收命令
         ↕ BLE (静态配置 + 动态状态)
Android APP (消防员终端)
  ├── Page 1: 设备扫描 — BLE 发现 FIRE_CTRL / 连接管理
  ├── Page 2: 火场地图 — Canvas 5层渲染 + 手势缩放
  └── Page 3: 分析面板 — 优先级/时间线/语音/趋势/控制
```

---

## 三、BLE 通信协议

### UUID

| 用途 | UUID |
|------|------|
| Service | `0000fff0-0000-1000-8000-00805f9b34fb` |
| Notify (推送) | `0000fff1-0000-1000-8000-00805f9b34fb` |
| Write (命令) | `0000fff2-0000-1000-8000-00805f9b34fb` |

### 消息类型 (ESP32 → APP, JSON)

| type | 内容 | 推送时机 |
|------|------|---------|
| `MAP_CONFIG` | 10×5 网格、colWidths/rowHeights、墙壁/出口坐标 | 连接后一次性 |
| `LIGHT_CONFIG` | 13个灯牌: id/x/y/type | 连接后一次性 |
| `FIRE_UPDATE` | `fires: [[x,y], ...]` | 火点增删时 |
| `DIRECTION_UPDATE` | `directions: {id: dir, ...}` | 路径重算后 |
| `VOICE_MODE` | mode/modeName/hasTrapped | 模式切换时 |
| `SENSOR_STATE` | `states: [bool×13]` | 每 500ms |
| `HEARTBEAT` | uptime/fireCount | 每 5s |

### 命令 (APP → ESP32)

| 命令 | 格式 |
|------|------|
| 手动设灯牌 | `SET_LIGHT:{id}:{direction}` |
| 添加火点 | `ADD_FIRE:{x},{y}` |
| 移除火点 | `REMOVE_FIRE:{x},{y}` 或 `REMOVE_FIRE:ALL` |
| 系统复位 | `SYSTEM_RESET` |

---

## 四、APP UI 架构

### 三页架构 (ViewPager2)

| 页面 | 内容 |
|------|------|
| Page 1: 设备 | BLE 扫描列表、连接状态、信号强度 |
| Page 2: 火场地图 | Canvas 5层渲染 + 手势缩放/平移/双击复位 |
| Page 3: 分析面板 | 救援优先级(RecyclerView)、时间线、语音播报、出口状态、火势趋势、快捷控制 |

### Canvas 5 层渲染管线

```
Layer 0: GridRenderer     — 墙壁(砖纹理) / 通道 / 出口(绿色辉光) / 网格线
Layer 1: WarningRenderer  — 被困区域黄色径向波纹扩散
Layer 2: PathRenderer     — 进攻路线蓝色虚线 + 起终点标记
Layer 3: FireRenderer     — 火点红色径向辉光脉冲
Layer 4: LightRenderer    — 灯牌底座 + 方向箭头(绿/黄/灰) + 编号标签
```

### 非均匀网格

每列/行独立宽高，通过 `device_config.json` 配置：

```json
{
  "mapLayout": {
    "cols": 10, "rows": 5,
    "colWidths": [60, 80, 60, 80, 60, 80, 60, 80, 60, 80],
    "rowHeights": [80, 60, 80, 60, 80],
    "wallColor": "#3d3d5c",
    "floorColor": "#252540",
    "gridLineColor": "#2a2a45",
    "padding": 12
  }
}
```

### 灯牌箭头方向映射

| 方向值 | 横向灯 | 纵向灯 | 颜色 |
|--------|--------|--------|------|
| 1 (PRIMARY) | ← 左 | ↑ 上 | 绿色 (火情下闪烁) |
| 2 (SECONDARY) | → 右 | ↓ 下 | 绿色 (火情下闪烁) |
| 3 (AT_EXIT) | 双箭头 | 双箭头 | 双绿 (火情下闪烁) |
| 4 (NO_PATH) | ⚠ 警告 | ⚠ 警告 | 双黄闪烁 (被困) |
| 0 | 熄灭 | 熄灭 | — |

---

## 五、核心算法

### 救援优先级 (P0-P3)

```
P0 (score ≥ 100): 🔴 立即救援 — 黄闪灯牌(被困)
P1 (score ≥ 50):  🟠 高优先级 — 距火点 2 格以内
P2 (score ≥ 20):  🟡 注意监控 — 距火点 3-5 格
P3 (score < 20):  ⚪ 安全区域
```

### 危险预警 (WARNING/CRITICAL)

- 相邻 ≥2 火点 → WARNING
- 相邻 ≥3 火点或位于火点 → CRITICAL
- 被困灯牌 → CRITICAL

### A\* 寻路

- 启发式: 曼哈顿距离
- 代价: 每步 g=1
- 不可通行: 墙壁 + 火灾点
- 双实现: ESP32 (实时 LED) + Kotlin (APP 进攻路线)

---

## 六、技术栈

| 技术 | 用途 |
|------|------|
| Kotlin | 开发语言 |
| MVVM + LiveData | 架构模式 |
| ViewPager2 | 三页导航 |
| Canvas (自定义View) | 火场地图渲染 |
| Android BLE API | 蓝牙通信 |
| Kotlinx Serialization | JSON 解析 |
| DataStore | 数据持久化 |
| Coroutines | 异步任务 |

---

## 七、非功能需求

| 需求 | 说明 |
|------|------|
| 最低 Android | API 24 (Android 7.0) |
| 目标 SDK | API 34 |
| 屏幕方向 | 竖屏 |
| BLE 延迟 | < 200ms |
| 深色主题 | 全局深色 (#1a1a2e) |
| 抗误触 | 控制按钮需二次确认 |

---

*文档版本: v1.0 | 日期: 2026-08-09*
