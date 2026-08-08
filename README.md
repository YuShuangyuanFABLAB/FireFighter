# 消防员终端APP — FireFighter

[![Version](https://img.shields.io/badge/version-v1.0-blue.svg)](https://github.com/YuShuangyuanFABLAB/FireFighter)
[![Tests](https://img.shields.io/badge/tests-103%2B-green.svg)]()

> 火灾智能动态疏散灯牌系统 · Android BLE 消防员态势感知终端
> 宋庆龄少年儿童发明奖 2026 国赛

---

## 系统概述

消防员通过 Android APP 蓝牙连接 ESP32 主控，实时获取火场态势信息：

- **火场地图可视化** — Canvas 5层渲染，旋转自适应竖屏
- **火灾点标记** — 红色径向辉光脉冲动画
- **灯牌箭头实时显示** — 疏散方向动态更新+闪烁
- **被困区域高亮** — 黄色波纹扩散 + 优先级颜色叠加
- **救援优先级** — P0(红)/P1(橙)/P2(黄) BFS 扩散到通道
- **出口状态** — 可用/火情威胁判断
- **A\* 路径规划** — 消防员进攻路线蓝色虚线
- **语音播报同步** — ESP32 语音模块状态同步显示
- **火灾时间线** — 传感器触发时序记录

## 项目结构

```
FireFighter/
├── app/                          # Android 应用 (Kotlin)
│   ├── src/main/java/.../
│   │   ├── data/ble/             # BLE 通信层
│   │   ├── data/repository/      # 数据中心 + 算法
│   │   ├── domain/model/         # 数据模型
│   │   └── presentation/ui/      # MVVM 三页面
│   ├── src/test/                 # 103+ 单元测试
│   └── src/main/assets/          # JSON 配置
├── esp32_firmware/               # ESP32 固件
│   ├── fire_ctrl/                # 完整集成固件 v2.1
│   └── fire_ble_extension/       # BLE 扩展参考
├── docs/                         # 技术文档
│   ├── DEVELOPMENT_LOG.md        # 完整开发日志
│   └── superpowers/              # Spec + Plan
└── gradle/                       # 构建系统
```

## 技术栈

| 层 | 技术 |
|----|------|
| Android | Kotlin + MVVM + LiveData + ViewPager2 |
| 地图渲染 | Canvas 自定义 View + 5层渲染管线 |
| BLE 通信 | Android BLE API + JSON 协议 |
| ESP32 | Arduino + BLE Server + 74HC595 LED + 语音模块 |
| 算法 | A\* 寻路 + BFS + 救援优先级 + 危险预警 |
| 测试 | JUnit + TDD (103+ tests) |

## 快速开始

### 环境要求

- Android Studio Hedgehog+
- Android SDK 34
- JDK 17
- ESP32 开发板 (NodeMCU-32S)

### 编译 APP

```bash
export JAVA_HOME="/path/to/jdk-17"
cd FireFighter
./gradlew :app:assembleDebug
# APK 在 app/build/outputs/apk/debug/
```

### 运行测试

```bash
./gradlew :app:testDebugUnitTest
# 103+ tests, 0 failures
```

### 烧录 ESP32 固件

1. Arduino IDE 打开 `esp32_firmware/fire_ctrl/fire_ctrl.ino`
2. 安装库: BLEDevice, BLEUtils, BLE2902
3. 选择 ESP32 Dev Module，烧录

### 使用

1. ESP32 上电，广播名 `FIRE_CTRL`
2. APP 授予蓝牙权限 → 扫描设备 → 连接
3. 地图页显示实时火场态势
4. 分析页查看出口/时间线/语音/趋势

## BLE 通信协议

| 消息类型 | 方向 | 内容 |
|---------|------|------|
| MAP_CONFIG | ESP→APP | 10×5 网格+墙壁+出口 |
| LIGHT_CONFIG | ESP→APP | 13个灯牌坐标+类型 |
| FIRE_UPDATE | ESP→APP | 火灾点坐标 |
| DIRECTION_UPDATE | ESP→APP | 灯牌方向(0-4) |
| VOICE_MODE | ESP→APP | 语音模式(怠速/疏散/救援) |
| SENSOR_STATE | ESP→APP | 13路火焰传感器状态 |
| HEARTBEAT | ESP→APP | 系统心跳 |
| 命令 | APP→ESP | SET_LIGHT / ADD_FIRE / REMOVE_FIRE / SYSTEM_RESET |

## 版本

| 版本 | 日期 | 内容 |
|------|------|------|
| v1.0 | 2026-08-09 | 首次发布: BLE通信+Canvas地图+优先级+时间线+ESP32固件 |

## 许可证

MIT License

## 作者

William Yu · FABLAB 法贝实验室
