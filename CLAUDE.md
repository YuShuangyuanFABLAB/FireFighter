# FireFighter — 消防员终端APP

> 火灾智能动态疏散灯牌系统 · 宋庆龄少年儿童发明奖 2026 国赛

## 项目路径

```
工作目录: D:\ProgramData\FirefighterApp\
JDK 17:   C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot\
SDK:      D:\ProgramData\AndroidSDK\
```

## 常用命令

```bash
# 设置环境
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.20.8-hotspot"
export PATH="$JAVA_HOME/bin:$PATH"

# 编译
cd D:/ProgramData/FirefighterApp
./gradlew :app:compileDebugKotlin --no-daemon

# 构建 APK
./gradlew :app:assembleDebug --no-daemon

# 运行测试 (103+ tests)
./gradlew :app:testDebugUnitTest --no-daemon

# Git 推送 (需要 GitHub 网络可用)
powershell.exe -Command "cd 'D:\ProgramData\FirefighterApp'; git push 2>&1"
```

## 项目结构

```
app/src/main/java/com/example/firefighterterminal/
├── data/ble/          BLE通信 + JSON解析
├── data/config/       配置文件加载
├── data/repository/   数据中心 + 算法
├── domain/model/      数据模型
├── presentation/ui/
│   ├── device/        设备扫描页
│   ├── map/           火场地图 (Canvas 5层渲染)
│   └── analysis/      分析面板
├── presentation/adapter/
└── presentation/ui/map/view/  渲染器 (Grid/Fire/Light/Priority/...)

esp32_firmware/fire_ctrl/fire_ctrl.ino  ESP32 集成固件 v2.1
docs/                                   技术文档
```

## 关键设计

- MVVM + LiveData + ViewPager2 三页
- BLE JSON 协议: 7种消息类型
- Canvas 旋转90度自适应竖屏
- 优先级颜色 BFS 扩散渲染
- ESP32 握手: delay 2s 后推送配置, 类型名缩短至2字符

## Git

- Remote: https://github.com/YuShuangyuanFABLAB/FireFighter
- Push 用 PowerShell (系统代理仅在 PowerShell 有效)
