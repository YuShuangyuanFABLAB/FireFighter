/**
 * 火灾疏散系统 — BLE 服务器扩展
 *
 * 在原 20260808.ino 基础上追加 BLE 通信模块。
 * 用法：将以下代码块按标记插入 20260808.ino 对应位置。
 *
 * ==================== 插入位置说明 ====================
 * [BLOCK 1] → 文件开头，在 #include <Arduino.h> 之后
 * [BLOCK 2] → 全局变量区域，在语音模块变量之后
 * [BLOCK 3] → 函数声明区域
 * [BLOCK 4] → setup() 末尾，Serial.println 之后
 * [BLOCK 5] → loop() 函数内，delay(10) 之前
 * [BLOCK 6] → 文件末尾
 */

// ==================== [BLOCK 1] 头文件 ====================
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define BLE_SERVICE_UUID        "0000fff0-0000-1000-8000-00805f9b34fb"
#define BLE_CHAR_NOTIFY_UUID    "0000fff1-0000-1000-8000-00805f9b34fb"
#define BLE_CHAR_WRITE_UUID     "0000fff2-0000-1000-8000-00805f9b34fb"
#define BLE_DEVICE_NAME         "FIRE_CTRL"
#define BLE_HEARTBEAT_INTERVAL  5000   // 心跳间隔 ms
#define BLE_SENSOR_INTERVAL     500    // 传感器状态推送间隔 ms

// ==================== [BLOCK 2] BLE 全局变量 ====================
BLEServer* bleServer = nullptr;
BLECharacteristic* bleNotifyChar = nullptr;
BLECharacteristic* bleWriteChar = nullptr;
bool bleDeviceConnected = false;
unsigned long lastBleHeartbeat = 0;
unsigned long lastBleSensorPush = 0;

// BLE Server 回调类
class FireServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
        bleDeviceConnected = true;
        Serial.println("[BLE] 设备已连接");
        // 连接后推送静态配置
        sendMapConfig();
        delay(100);
        sendLightConfig();
        delay(100);
        sendCurrentState();
    }

    void onDisconnect(BLEServer* pServer) {
        bleDeviceConnected = false;
        Serial.println("[BLE] 设备已断开，重新广播");
        delay(500);
        pServer->getAdvertising()->start();
    }
};

// BLE 写入回调类
class FireWriteCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {
        std::string value = pCharacteristic->getValue();
        if (value.length() > 0) {
            String cmd = String(value.c_str());
            Serial.print("[BLE] 收到命令: "); Serial.println(cmd);
            processBleCommand(cmd);
        }
    }
};

// ==================== [BLOCK 3] BLE 函数声明 ====================
void initBLE();
void sendJson(const String& json);
void sendMapConfig();
void sendLightConfig();
void sendCurrentState();
void sendFireUpdate();
void sendDirectionUpdate();
void sendVoiceMode(int mode, bool hasTrapped);
void sendSensorState();
void sendHeartbeat();
void processBleCommand(const String& cmd);

// ==================== [BLOCK 4] BLE 初始化 ====================
void initBLE() {
    BLEDevice::init(BLE_DEVICE_NAME);
    bleServer = BLEDevice::createServer();
    bleServer->setCallbacks(new FireServerCallbacks());

    BLEService* pService = bleServer->createService(BLE_SERVICE_UUID);

    // 通知特征值
    bleNotifyChar = pService->createCharacteristic(
        BLE_CHAR_NOTIFY_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
    );
    bleNotifyChar->addDescriptor(new BLE2902());

    // 写入特征值
    bleWriteChar = pService->createCharacteristic(
        BLE_CHAR_WRITE_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
    );
    bleWriteChar->setCallbacks(new FireWriteCallbacks());

    pService->start();

    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] 服务已启动，广播名称: " BLE_DEVICE_NAME);
}

// ==================== [BLOCK 5] 主循环中添加 BLE 推送 ====================
// 在 loop() 的 delay(10) 之前添加:
//   unsigned long now = millis();
//   if (bleDeviceConnected) {
//     if (now - lastBleSensorPush >= BLE_SENSOR_INTERVAL) {
//       lastBleSensorPush = now;
//       sendSensorState();
//     }
//     if (now - lastBleHeartbeat >= BLE_HEARTBEAT_INTERVAL) {
//       lastBleHeartbeat = now;
//       sendHeartbeat();
//     }
//   }

// ==================== [BLOCK 6] BLE 函数实现 ====================

/** 发送 JSON 字符串（分包，确保不超过 512 字节 MTU） */
void sendJson(const String& json) {
    if (!bleDeviceConnected || bleNotifyChar == nullptr) return;

    const int MTU_LIMIT = 480;  // 略小于 512 确保安全
    if (json.length() <= MTU_LIMIT) {
        bleNotifyChar->setValue(json.c_str());
        bleNotifyChar->notify();
    } else {
        // 分包发送（JSON 对象无法简单分割，使用换行分隔符）
        int offset = 0;
        while (offset < json.length()) {
            int len = min(MTU_LIMIT, json.length() - offset);
            String chunk = json.substring(offset, offset + len);
            bleNotifyChar->setValue(chunk.c_str());
            bleNotifyChar->notify();
            offset += len;
            delay(20);  // 给客户端时间处理
        }
    }
    Serial.print("[BLE] 发送("); Serial.print(json.length()); Serial.print("B): ");
    Serial.println(json.substring(0, min(80, json.length())));
}

/** 发送地图静态配置 */
void sendMapConfig() {
    String json = "{\"type\":\"MAP_CONFIG\",\"width\":10,\"height\":5,";
    json += "\"colWidths\":[60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0],";
    json += "\"rowHeights\":[80.0,60.0,80.0,60.0,80.0],";
    json += "\"walls\":[";
    // 从 DISPLAY_GRID 中提取墙壁坐标
    bool first = true;
    for (int y = 0; y < MAP_HEIGHT; y++) {
        for (int x = 0; x < MAP_WIDTH; x++) {
            if (ORIGINAL_GRID[y][x] == 1) {
                if (!first) json += ",";
                first = false;
                json += "["; json += x; json += ","; json += y; json += "]";
            }
        }
    }
    json += "],\"exits\":[";
    for (int i = 0; i < exitCount; i++) {
        if (i > 0) json += ",";
        json += "["; json += exits[i].x; json += ","; json += exits[i].y; json += "]";
    }
    json += "]}";
    sendJson(json);
}

/** 发送灯牌静态配置 */
void sendLightConfig() {
    String json = "{\"type\":\"LIGHT_CONFIG\",\"lights\":[";
    for (int i = 0; i < lightCount; i++) {
        if (i > 0) json += ",";
        json += "{\"id\":"; json += lights[i].index;
        json += ",\"x\":"; json += lights[i].pos.x;
        json += ",\"y\":"; json += lights[i].pos.y;
        json += ",\"type\":\"";
        switch (lights[i].type) {
            case HORIZONTAL_UP: json += "HORIZONTAL_UP"; break;
            case HORIZONTAL_DOWN: json += "HORIZONTAL_DOWN"; break;
            case VERTICAL_LEFT: json += "VERTICAL_LEFT"; break;
            case VERTICAL_RIGHT: json += "VERTICAL_RIGHT"; break;
        }
        json += "\"}";
    }
    json += "]}";
    sendJson(json);
}

/** 发送当前状态（连接后立即同步） */
void sendCurrentState() {
    sendFireUpdate();
    sendDirectionUpdate();
    int mode = (fireCount == 0) ? 0 : (voiceMode == 2 ? 2 : 1);
    sendVoiceMode(mode, voiceMode == 2);
}

/** 发送火灾点更新 */
void sendFireUpdate() {
    String json = "{\"type\":\"FIRE_UPDATE\",\"fires\":[";
    for (int i = 0; i < fireCount; i++) {
        if (i > 0) json += ",";
        json += "["; json += fires[i].x; json += ","; json += fires[i].y; json += "]";
    }
    json += "]}";
    sendJson(json);
}

/** 发送灯牌方向更新 */
void sendDirectionUpdate() {
    String json = "{\"type\":\"DIRECTION_UPDATE\",\"directions\":{";
    bool first = true;
    for (int i = 0; i < lightCount; i++) {
        if (!first) json += ",";
        first = false;
        json += "\""; json += lights[i].index; json += "\":";
        json += lights[i].direction;
    }
    json += "}}";
    sendJson(json);
}

/** 发送语音模式 */
void sendVoiceMode(int mode, bool hasTrapped) {
    String modeName = (mode == 0) ? "IDLE" : (mode == 1 ? "ESCAPE" : "RESCUE");
    String json = "{\"type\":\"VOICE_MODE\",\"mode\":";
    json += mode;
    json += ",\"modeName\":\""; json += modeName; json += "\"";
    json += ",\"hasTrapped\":"; json += hasTrapped ? "true" : "false";
    json += "}";
    sendJson(json);
}

/** 发送传感器状态 */
void sendSensorState() {
    String json = "{\"type\":\"SENSOR_STATE\",\"states\":[";
    for (int i = 0; i < lightCount; i++) {
        if (i > 0) json += ",";
        json += sensorLastState[i] ? "true" : "false";
    }
    json += "]}";
    sendJson(json);
}

/** 发送心跳 */
void sendHeartbeat() {
    String json = "{\"type\":\"HEARTBEAT\",\"uptime\":";
    json += millis();
    json += ",\"fireCount\":";
    json += fireCount;
    json += "}";
    sendJson(json);
}

/** 处理 APP 下发的命令 */
void processBleCommand(const String& cmd) {
    // SET_LIGHT:{id}:{direction}
    if (cmd.startsWith("SET_LIGHT:")) {
        int firstColon = cmd.indexOf(':', 10);
        int id = cmd.substring(10, firstColon).toInt();
        int dir = cmd.substring(firstColon + 1).toInt();
        if (id >= 1 && id <= lightCount && dir >= 0 && dir <= 4) {
            lights[id - 1].setDirection(dir);
            updateHardware();
            Serial.print("[BLE] 手动设置 L"); Serial.print(id);
            Serial.print(" 方向="); Serial.println(dir);
            sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"SET_LIGHT\",\"status\":\"OK\"}");
        }
    }
    // ADD_FIRE:{x},{y}
    else if (cmd.startsWith("ADD_FIRE:")) {
        int comma = cmd.indexOf(',', 9);
        int x = cmd.substring(9, comma).toInt();
        int y = cmd.substring(comma + 1).toInt();
        addFire(Position(x, y));
        sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"ADD_FIRE\",\"status\":\"OK\"}");
    }
    // REMOVE_FIRE:{x},{y} or REMOVE_FIRE:ALL
    else if (cmd.startsWith("REMOVE_FIRE:")) {
        String param = cmd.substring(12);
        if (param == "ALL") {
            while (fireCount > 0) removeFire(fires[0]);
        } else {
            int comma = param.indexOf(',');
            int x = param.substring(0, comma).toInt();
            int y = param.substring(comma + 1).toInt();
            removeFire(Position(x, y));
        }
        sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"REMOVE_FIRE\",\"status\":\"OK\"}");
    }
    // SYSTEM_RESET
    else if (cmd == "SYSTEM_RESET") {
        while (fireCount > 0) removeFire(fires[0]);
        computeAllPaths();
        sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"SYSTEM_RESET\",\"status\":\"OK\"}");
    }
    else {
        Serial.print("[BLE] 未知命令: "); Serial.println(cmd);
    }
}

// ==================== 修改 computeAllPaths() ====================
// 在 computeAllPaths() 函数末尾（updateHardware() 之后）添加:
//   if (bleDeviceConnected) {
//     sendFireUpdate();
//     sendDirectionUpdate();
//   }

// ==================== 修改 updateVoiceModule() ====================
// 在 updateVoiceModule() 函数中，模式切换时（voiceMode 赋值后）添加:
//   if (bleDeviceConnected) {
//     sendVoiceMode(voiceMode, (voiceMode == 2));
//   }
