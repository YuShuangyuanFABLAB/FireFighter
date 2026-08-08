/**
 * 火灾智能动态疏散灯牌系统 — 国赛升级 v2.1
 *
 * v2.0 基础上集成 BLE 服务器，为消防员终端 APP 提供实时火场数据。
 * ESP32 Arduino 框架
 */

#include <Arduino.h>

// ==================== BLE 头文件 ====================
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// ==================== 地图配置 ====================
const int MAP_WIDTH = 10;
const int MAP_HEIGHT = 5;

const int DISPLAY_GRID[MAP_HEIGHT][MAP_WIDTH] = {
  {1, 1, 1, 1, 1, 2, 1, 1, 1, 1},
  {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
  {1, 0, 1, 0, 1, 1, 0, 1, 0, 1},
  {2, 0, 0, 0, 0, 0, 0, 0, 0, 2},
  {1, 1, 1, 2, 1, 1, 1, 1, 1, 1}
};

int ORIGINAL_GRID[MAP_HEIGHT][MAP_WIDTH];

void initGridFlipped() {
  for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
      ORIGINAL_GRID[y][x] = DISPLAY_GRID[y][x];
    }
  }
}

// ==================== 灯牌位置配置 ====================
const int LIGHT_COORDS[13][2] = {
  {3, 0}, {3, 2}, {4, 3}, {3, 5}, {3, 7}, {3, 9},
  {2, 1}, {2, 3}, {2, 6}, {2, 8}, {1, 7}, {0, 5}, {1, 2}
};

enum LightType {
  HORIZONTAL_UP, HORIZONTAL_DOWN, VERTICAL_LEFT, VERTICAL_RIGHT
};

bool isHorizontal(LightType t);

const LightType LIGHT_TYPE[13] = {
  HORIZONTAL_UP, HORIZONTAL_UP, VERTICAL_LEFT, HORIZONTAL_UP,
  HORIZONTAL_DOWN, HORIZONTAL_DOWN, VERTICAL_RIGHT, VERTICAL_LEFT,
  VERTICAL_LEFT, VERTICAL_LEFT, HORIZONTAL_DOWN, VERTICAL_LEFT, HORIZONTAL_DOWN
};

// ==================== 硬件引脚 ====================
#define PIN_DATA   2
#define PIN_CLOCK  15
#define PIN_LATCH  4
#define PIN_VOICE_ESCAPE 16
#define PIN_VOICE_RESCUE 17

const int SENSOR_PINS[13] = {
  32, 33, 25, 26, 27, 14, 12, 13, 23, 22, 21, 19, 18
};

// ==================== 常量定义 ====================
const int LED_OFF    = 0;
const int LED_GREEN  = 1;
const int LED_RED    = 2;
const int LED_YELLOW = 3;

const int DIR_PRIMARY   = 1;
const int DIR_SECONDARY = 2;
const int DIR_AT_EXIT   = 3;
const int DIR_NO_PATH   = 4;

const int INF_COST = 10000;

bool isDirectionalLight(int dir) {
  return dir >= DIR_PRIMARY && dir <= DIR_AT_EXIT;
}

// ==================== BLE 常量 ====================
#define BLE_SERVICE_UUID        "0000fff0-0000-1000-8000-00805f9b34fb"
#define BLE_CHAR_NOTIFY_UUID    "0000fff1-0000-1000-8000-00805f9b34fb"
#define BLE_CHAR_WRITE_UUID     "0000fff2-0000-1000-8000-00805f9b34fb"
#define BLE_DEVICE_NAME         "FIRE_CTRL"
#define BLE_HEARTBEAT_INTERVAL  5000
#define BLE_SENSOR_INTERVAL     500

// ==================== Position 结构体 ====================
struct Position {
  int x, y;
  Position() : x(-1), y(-1) {}
  Position(int x, int y) : x(x), y(y) {}
  bool operator==(const Position& other) const { return x == other.x && y == other.y; }
  bool isValid() const { return x >= 0 && x < MAP_WIDTH && y >= 0 && y < MAP_HEIGHT; }
  void print() const { Serial.print("("); Serial.print(x); Serial.print(","); Serial.print(y); Serial.print(")"); }
};

// ==================== LightSign 结构体 ====================
struct LightSign {
  Position pos;
  int index;
  int direction;
  bool isLit;
  LightType type;

  LightSign() : index(-1), direction(0), isLit(false), type(HORIZONTAL_UP) {}
  LightSign(int idx, int x, int y, LightType t) : index(idx), pos(x, y), direction(0), isLit(false), type(t) {}

  void setDirection(int dir) { direction = dir; isLit = (dir != 0); }

  int led1StaticColor() const {
    if (!isHorizontal(type))
      return (direction == DIR_SECONDARY) ? LED_GREEN : LED_OFF;
    else
      return (direction == DIR_PRIMARY) ? LED_GREEN : LED_OFF;
  }

  int led2StaticColor() const {
    if (!isHorizontal(type))
      return (direction == DIR_PRIMARY) ? LED_GREEN : LED_OFF;
    else
      return (direction == DIR_SECONDARY) ? LED_GREEN : LED_OFF;
  }

  int getLed1Color(int fireCount, bool flashOn) const {
    return resolveLedColor(led1StaticColor(), fireCount, flashOn);
  }

  int getLed2Color(int fireCount, bool flashOn) const {
    return resolveLedColor(led2StaticColor(), fireCount, flashOn);
  }

  char getDisplayChar(bool flashOn) const {
    if (!isLit) return ' ';
    if (direction == DIR_AT_EXIT) return 'E';
    if (direction == DIR_NO_PATH) return flashOn ? 'Y' : ' ';
    switch(type) {
      case HORIZONTAL_UP: case HORIZONTAL_DOWN: return (direction == DIR_PRIMARY) ? 'L' : 'R';
      case VERTICAL_LEFT: case VERTICAL_RIGHT: return (direction == DIR_PRIMARY) ? 'U' : 'D';
    }
    return ' ';
  }

private:
  int resolveLedColor(int staticColor, int fireCount, bool flashOn) const {
    if (!isLit) return LED_OFF;
    if (direction == DIR_NO_PATH) return flashOn ? LED_YELLOW : LED_OFF;
    if (direction == DIR_AT_EXIT) return (fireCount > 0) ? (flashOn ? LED_GREEN : LED_OFF) : LED_GREEN;
    return (fireCount > 0) ? (flashOn ? staticColor : LED_OFF) : staticColor;
  }
};

// ==================== 74HC595 硬件映射 ====================
struct { byte byteIndex; byte bitPos; } ledMapping[13][2];

void initLedMapping() {
  ledMapping[0][0] = {8, 0}; ledMapping[0][1] = {8, 2};
  ledMapping[1][0] = {8, 4}; ledMapping[1][1] = {8, 6};
  ledMapping[2][0] = {7, 0}; ledMapping[2][1] = {7, 2};
  ledMapping[3][0] = {7, 4}; ledMapping[3][1] = {7, 6};
  ledMapping[4][0] = {6, 0}; ledMapping[4][1] = {6, 2};
  ledMapping[5][0] = {6, 4}; ledMapping[5][1] = {6, 6};
  ledMapping[6][0] = {5, 0}; ledMapping[6][1] = {5, 2};
  ledMapping[7][0] = {5, 6}; ledMapping[7][1] = {5, 4};
  ledMapping[8][0] = {4, 2}; ledMapping[8][1] = {4, 0};
  ledMapping[9][0] = {4, 4}; ledMapping[9][1] = {4, 6};
  ledMapping[10][0] = {3, 0}; ledMapping[10][1] = {3, 2};
  ledMapping[11][0] = {3, 6}; ledMapping[11][1] = {3, 4};
  ledMapping[12][0] = {2, 0}; ledMapping[12][1] = {2, 2};
}

byte setLedBits(byte original, int bitPos, int color) {
  byte colorBits = 0;
  switch(color) {
    case LED_GREEN:  colorBits = 0b01; break;
    case LED_RED:    colorBits = 0b10; break;
    case LED_YELLOW: colorBits = 0b11; break;
    default:         colorBits = 0b00; break;
  }
  original &= ~(0b11 << bitPos);
  original |= (colorBits << bitPos);
  return original;
}

// ==================== 全局变量 ====================
Position exits[20];
LightSign lights[13];
Position fires[13];
int fireCount = 0;
int exitCount = 0;
int lightCount = 0;

unsigned long lastSensorCheck = 0;
const unsigned long SENSOR_CHECK_INTERVAL = 50;
bool sensorLastState[13] = {false};
bool lightTriggeredFire[13] = {false};

bool yellowFlashOn = false;
unsigned long lastFlashToggle = 0;
const unsigned long FLASH_INTERVAL = 500;

int voiceMode = 0;
unsigned long voiceCycleStart = 0;
const unsigned long VOICE_HIGH_MS = 1000;
const unsigned long VOICE_ESCAPE_INTERVAL = 10000;
const unsigned long VOICE_RESCUE_INTERVAL = 20000;

// BLE 全局变量
BLEServer* bleServer = nullptr;
BLECharacteristic* bleNotifyChar = nullptr;
BLECharacteristic* bleWriteChar = nullptr;
bool bleDeviceConnected = false;
bool oldBleDeviceConnected = false;
unsigned long bleConnectTime = 0;
bool bleConfigSent = false;
unsigned long lastBleHeartbeat = 0;
unsigned long lastBleSensorPush = 0;

// ==================== BLE Server 回调 ====================
class FireServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    bleDeviceConnected = true;
    bleConnectTime = millis();
    bleConfigSent = false;
    Serial.println("[BLE] 设备已连接，2s后推送配置...");
  }
  void onDisconnect(BLEServer* pServer) {
    bleDeviceConnected = false;
    bleConfigSent = false;
    Serial.println("[BLE] 设备已断开，重新广播");
    delay(500);
    pServer->startAdvertising();
    Serial.println("[BLE] 重新开始广播");
  }
};

// BLE 写入回调的前向声明
void processBleCommand(const String& cmd);

class FireWriteCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    String value = pCharacteristic->getValue().c_str();
    if (value.length() > 0) {
      Serial.print("[BLE] 收到命令: "); Serial.println(value);
      processBleCommand(value);
    }
  }
};

// ==================== 函数声明 ====================
// 核心函数
void sendByte(byte data);
void updateHardware();
int manhattanDistance(const Position& a, const Position& b);
bool isWalkable(int x, int y);
Position findNearestWalkableCell(const Position& pos);
Position findSafeWalkableCell(const Position& pos);
bool findPath(const Position& start, const Position& end, Position path[], int& pathLength);
int getFirstStepDirection(const Position& start, const Position& end, LightType type);
Position findNearestReachableExit(const Position& start, LightType type);
void computeLightDirection(int i, bool hasFire);
void computeAllPaths();
void addFire(const Position& pos);
void removeFire(const Position& pos);
void updateVoiceModule();
void scanSensors();
void initData();
int getExitDirection(const Position& exitPos, LightType type);
void printMap();

// BLE 函数
void initBLE();
void sendJson(const String& json);
void pushStaticConfig();
void pushStateUpdate();
void pushVoiceMode();
void processBleCommand(const String& cmd);
void blePeriodicPush();

// ==================== 74HC595 控制 ====================
void sendByte(byte data) {
  for (int i = 7; i >= 0; i--) {
    digitalWrite(PIN_DATA, (data >> i) & 0x01 ? HIGH : LOW);
    delayMicroseconds(5);
    digitalWrite(PIN_CLOCK, HIGH);
    delayMicroseconds(5);
    digitalWrite(PIN_CLOCK, LOW);
    delayMicroseconds(5);
  }
}

void updateHardware() {
  byte dataBytes[9] = {0};
  for (int i = 0; i < lightCount; i++) {
    int byteIdx1 = ledMapping[i][0].byteIndex;
    int bitPos1 = ledMapping[i][0].bitPos;
    dataBytes[byteIdx1] = setLedBits(dataBytes[byteIdx1], bitPos1, lights[i].getLed1Color(fireCount, yellowFlashOn));
    int byteIdx2 = ledMapping[i][1].byteIndex;
    int bitPos2 = ledMapping[i][1].bitPos;
    dataBytes[byteIdx2] = setLedBits(dataBytes[byteIdx2], bitPos2, lights[i].getLed2Color(fireCount, yellowFlashOn));
  }
  digitalWrite(PIN_LATCH, LOW);
  delayMicroseconds(10);
  for (int i = 0; i < 9; i++) sendByte(dataBytes[i]);
  digitalWrite(PIN_LATCH, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_LATCH, LOW);
}

// ==================== 核心函数 ====================
bool isHorizontal(LightType t) { return t == HORIZONTAL_UP || t == HORIZONTAL_DOWN; }

int manhattanDistance(const Position& a, const Position& b) {
  return abs(a.x - b.x) + abs(a.y - b.y);
}

bool isWalkable(int x, int y) {
  if (x < 0 || x >= MAP_WIDTH || y < 0 || y >= MAP_HEIGHT) return false;
  if (ORIGINAL_GRID[y][x] == 1) return false;
  for (int i = 0; i < fireCount; i++) {
    if (fires[i].x == x && fires[i].y == y) return false;
  }
  return true;
}

Position findNearestWalkableCell(const Position& pos) {
  if (pos.x >= 0 && pos.x < MAP_WIDTH && pos.y >= 0 && pos.y < MAP_HEIGHT
      && ORIGINAL_GRID[pos.y][pos.x] != 1)
    return pos;
  int dirs[4][2] = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
  bool visited[MAP_HEIGHT][MAP_WIDTH] = {false};
  Position queue[MAP_WIDTH * MAP_HEIGHT];
  int front = 0, rear = 0;
  queue[rear++] = pos;
  if (pos.y >= 0 && pos.y < MAP_HEIGHT && pos.x >= 0 && pos.x < MAP_WIDTH)
    visited[pos.y][pos.x] = true;
  while (front < rear) {
    Position cur = queue[front++];
    for (int d = 0; d < 4; d++) {
      int nx = cur.x + dirs[d][0]; int ny = cur.y + dirs[d][1];
      if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT && !visited[ny][nx]) {
        visited[ny][nx] = true;
        if (ORIGINAL_GRID[ny][nx] != 1) return Position(nx, ny);
        queue[rear++] = Position(nx, ny);
      }
    }
  }
  return pos;
}

Position findSafeWalkableCell(const Position& pos) {
  int dirs[4][2] = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
  bool visited[MAP_HEIGHT][MAP_WIDTH] = {false};
  Position queue[MAP_WIDTH * MAP_HEIGHT];
  int front = 0, rear = 0;
  queue[rear++] = pos;
  if (pos.y >= 0 && pos.y < MAP_HEIGHT && pos.x >= 0 && pos.x < MAP_WIDTH)
    visited[pos.y][pos.x] = true;
  Position candidates[10]; int candCount = 0;
  while (front < rear && candCount < 10) {
    Position cur = queue[front++];
    for (int d = 0; d < 4; d++) {
      int nx = cur.x + dirs[d][0]; int ny = cur.y + dirs[d][1];
      if (nx >= 0 && nx < MAP_WIDTH && ny >= 0 && ny < MAP_HEIGHT && !visited[ny][nx]) {
        visited[ny][nx] = true;
        if (ORIGINAL_GRID[ny][nx] != 1) {
          bool isFire = false;
          for (int f = 0; f < fireCount; f++) {
            if (fires[f].x == nx && fires[f].y == ny) { isFire = true; break; }
          }
          if (!isFire) candidates[candCount++] = Position(nx, ny);
        }
        queue[rear++] = Position(nx, ny);
      }
    }
  }
  for (int c = 0; c < candCount; c++) {
    for (int e = 0; e < exitCount; e++) {
      if (!isWalkable(exits[e].x, exits[e].y)) continue;
      Position tp[100]; int tl;
      if (findPath(candidates[c], exits[e], tp, tl)) return candidates[c];
    }
  }
  return (candCount > 0) ? candidates[0] : Position(-1, -1);
}

bool findPath(const Position& start, const Position& end, Position path[], int& pathLength) {
  pathLength = 0;
  if (!isWalkable(start.x, start.y) || !isWalkable(end.x, end.y)) return false;
  if (start == end) { path[0] = start; pathLength = 1; return true; }
  int gScore[MAP_HEIGHT][MAP_WIDTH];
  int fScore[MAP_HEIGHT][MAP_WIDTH];
  int parentX[MAP_HEIGHT][MAP_WIDTH];
  int parentY[MAP_HEIGHT][MAP_WIDTH];
  bool closed[MAP_HEIGHT][MAP_WIDTH];
  for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
      gScore[y][x] = INF_COST; fScore[y][x] = INF_COST;
      parentX[y][x] = -1; parentY[y][x] = -1; closed[y][x] = false;
    }
  }
  gScore[start.y][start.x] = 0;
  fScore[start.y][start.x] = manhattanDistance(start, end);
  int dirs[4][2] = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
  while (true) {
    int bestF = INF_COST; int curX = -1, curY = -1;
    for (int y = 0; y < MAP_HEIGHT; y++) {
      for (int x = 0; x < MAP_WIDTH; x++) {
        if (!closed[y][x] && gScore[y][x] < INF_COST && fScore[y][x] < bestF) {
          bestF = fScore[y][x]; curX = x; curY = y;
        }
      }
    }
    if (curX == -1) return false;
    if (curX == end.x && curY == end.y) {
      Position revPath[100]; int revLen = 0;
      int px = end.x, py = end.y;
      while (px != start.x || py != start.y) {
        revPath[revLen++] = Position(px, py);
        int opx = parentX[py][px]; int opy = parentY[py][px];
        px = opx; py = opy;
      }
      revPath[revLen++] = start;
      for (int i = 0; i < revLen; i++) path[i] = revPath[revLen - 1 - i];
      pathLength = revLen; return true;
    }
    closed[curY][curX] = true;
    for (int d = 0; d < 4; d++) {
      int nx = curX + dirs[d][0]; int ny = curY + dirs[d][1];
      if (!isWalkable(nx, ny) || closed[ny][nx]) continue;
      int tentativeG = gScore[curY][curX] + 1;
      if (tentativeG < gScore[ny][nx]) {
        parentX[ny][nx] = curX; parentY[ny][nx] = curY;
        gScore[ny][nx] = tentativeG;
        fScore[ny][nx] = tentativeG + manhattanDistance(Position(nx, ny), end);
      }
    }
  }
}

int getFirstStepDirection(const Position& start, const Position& end, LightType type) {
  Position path[100]; int pathLength = 0;
  if (findPath(start, end, path, pathLength) && pathLength >= 2) {
    bool horiz = isHorizontal(type);
    for (int s = 1; s < pathLength; s++) {
      int dx = path[s].x - path[s-1].x; int dy = path[s].y - path[s-1].y;
      if (horiz) {
        if (dx == 1) return DIR_SECONDARY;
        if (dx == -1) return DIR_PRIMARY;
      } else {
        if (dy == -1) return DIR_PRIMARY;
        if (dy == 1) return DIR_SECONDARY;
      }
    }
    if (horiz) return (end.x < start.x) ? DIR_PRIMARY : DIR_SECONDARY;
    else return (end.y < start.y) ? DIR_PRIMARY : DIR_SECONDARY;
  }
  return 0;
}

Position findNearestReachableExit(const Position& start, LightType type) {
  Position reachableExits[20]; int reachableDists[20]; int reachableCount = 0;
  for (int e = 0; e < exitCount; e++) {
    if (!isWalkable(exits[e].x, exits[e].y)) continue;
    Position tempPath[100]; int tempLen;
    if (findPath(start, exits[e], tempPath, tempLen)) {
      reachableExits[reachableCount] = exits[e];
      reachableDists[reachableCount] = tempLen; reachableCount++;
    }
  }
  for (int i = 0; i < reachableCount - 1; i++) {
    for (int j = i + 1; j < reachableCount; j++) {
      if (reachableDists[j] < reachableDists[i]) {
        Position tempP = reachableExits[i]; reachableExits[i] = reachableExits[j]; reachableExits[j] = tempP;
        int tempD = reachableDists[i]; reachableDists[i] = reachableDists[j]; reachableDists[j] = tempD;
      }
    }
  }
  if (reachableCount > 0) return reachableExits[0];
  Position bestExit; int bestDist = INF_COST;
  for (int e = 0; e < exitCount; e++) {
    if (!isWalkable(exits[e].x, exits[e].y)) continue;
    int dist = manhattanDistance(start, exits[e]);
    if (dist < bestDist) { bestDist = dist; bestExit = exits[e]; }
  }
  return bestExit;
}

int getExitDirection(const Position& exitPos, LightType type) {
  switch(type) {
    case HORIZONTAL_UP: case HORIZONTAL_DOWN:
      if (exitPos.x == 0) return DIR_PRIMARY;
      if (exitPos.x == MAP_WIDTH - 1) return DIR_SECONDARY;
      return DIR_SECONDARY;
    case VERTICAL_LEFT: case VERTICAL_RIGHT:
      if (exitPos.y == 0) return DIR_PRIMARY;
      if (exitPos.y == MAP_HEIGHT - 1) return DIR_SECONDARY;
      return DIR_SECONDARY;
  }
  return DIR_SECONDARY;
}

// ==================== 核心逻辑 ====================
void computeLightDirection(int i, bool hasFire) {
  if (hasFire && lightTriggeredFire[i]) {
    lights[i].setDirection(0);
    return;
  }
  Position walkableStart = findNearestWalkableCell(lights[i].pos);
  if (hasFire) {
    bool lightOnFire = false;
    for (int f = 0; f < fireCount; f++) {
      if (fires[f] == walkableStart) { lightOnFire = true; break; }
    }
    if (lightOnFire) {
      Position alt = findSafeWalkableCell(lights[i].pos);
      if (alt.x == -1) { lights[i].setDirection(0); return; }
      walkableStart = alt;
    }
  }
  bool isOnExit = false;
  for (int e = 0; e < exitCount; e++) {
    if (exits[e] == walkableStart) { isOnExit = true; break; }
  }
  if (isOnExit) {
    lights[i].setDirection(getExitDirection(walkableStart, lights[i].type));
    return;
  }
  Position targetExit = findNearestReachableExit(walkableStart, lights[i].type);
  if (targetExit.isValid()) {
    int dir = getFirstStepDirection(walkableStart, targetExit, lights[i].type);
    if (dir != 0) lights[i].setDirection(dir);
    else lights[i].setDirection(DIR_NO_PATH);
  } else {
    lights[i].setDirection(DIR_NO_PATH);
  }
}

void computeAllPaths() {
  Serial.println("\n========================================");
  Serial.println("重新计算路径 (避开火灾)");
  Serial.println("========================================");
  bool hasFire = (fireCount > 0);
  for (int i = 0; i < lightCount; i++) computeLightDirection(i, hasFire);
  updateHardware();
  printMap();

  // ★ BLE: 推送状态更新
  if (bleDeviceConnected) pushStateUpdate();
}

// ==================== 地图显示 ====================
void printMap() {  /* ... 保留原实现 ... */ }

// ==================== 火灾管理 ====================
void addFire(const Position& pos) {
  for (int i = 0; i < fireCount; i++) {
    if (fires[i] == pos) return;
  }
  if (fireCount < 13) {
    fires[fireCount++] = pos;
    Serial.print("[火] 添加: "); pos.print(); Serial.println();
    computeAllPaths();
  }
}

void removeFire(const Position& pos) {
  for (int i = 0; i < fireCount; i++) {
    if (fires[i] == pos) {
      for (int j = i; j < fireCount - 1; j++) fires[j] = fires[j+1];
      fireCount--;
      Serial.print("[火] 移除: "); pos.print(); Serial.println();
      computeAllPaths();
      break;
    }
  }
}

// ==================== 语音模块 ====================
void updateVoiceModule() {
  unsigned long now = millis();
  int targetMode = 0;
  if (fireCount > 0) {
    bool hasTrapped = false;
    for (int i = 0; i < lightCount; i++) {
      if (lights[i].direction == DIR_NO_PATH) { hasTrapped = true; break; }
    }
    targetMode = hasTrapped ? 2 : 1;
  }
  if (targetMode != voiceMode) {
    voiceMode = targetMode;
    voiceCycleStart = now;
    digitalWrite(PIN_VOICE_ESCAPE, LOW);
    digitalWrite(PIN_VOICE_RESCUE, LOW);
    // ★ BLE: 推送语音模式变化
    if (bleDeviceConnected) pushVoiceMode();
  }
  if (voiceMode == 0) return;
  unsigned long elapsed = now - voiceCycleStart;
  if (voiceMode == 1) {
    unsigned long cyclePos = elapsed % VOICE_ESCAPE_INTERVAL;
    digitalWrite(PIN_VOICE_ESCAPE, (cyclePos < VOICE_HIGH_MS) ? HIGH : LOW);
    digitalWrite(PIN_VOICE_RESCUE, LOW);
  } else {
    unsigned long cyclePos = elapsed % VOICE_RESCUE_INTERVAL;
    if (cyclePos < VOICE_HIGH_MS) {
      digitalWrite(PIN_VOICE_ESCAPE, LOW); digitalWrite(PIN_VOICE_RESCUE, HIGH);
    } else if (cyclePos >= VOICE_ESCAPE_INTERVAL && cyclePos < VOICE_ESCAPE_INTERVAL + VOICE_HIGH_MS) {
      digitalWrite(PIN_VOICE_ESCAPE, HIGH); digitalWrite(PIN_VOICE_RESCUE, LOW);
    } else {
      digitalWrite(PIN_VOICE_ESCAPE, LOW); digitalWrite(PIN_VOICE_RESCUE, LOW);
    }
  }
}

// ==================== 传感器扫描 ====================
void scanSensors() {
  bool stateChanged = false;
  for (int i = 0; i < lightCount; i++) {
    bool currentState = (digitalRead(SENSOR_PINS[i]) == LOW);
    if (currentState != sensorLastState[i]) {
      sensorLastState[i] = currentState;
      stateChanged = true;
      lightTriggeredFire[i] = currentState;
      if (currentState) addFire(findNearestWalkableCell(lights[i].pos));
      else removeFire(findNearestWalkableCell(lights[i].pos));
    }
  }
}

// ==================== 初始化 ====================
void initData() {
  exitCount = 0; lightCount = 0;
  for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
      if (ORIGINAL_GRID[y][x] == 2 && exitCount < 20) {
        exits[exitCount++] = Position(x, y);
      }
    }
  }
  for (int i = 0; i < 13; i++) {
    lights[lightCount++] = LightSign(i + 1, LIGHT_COORDS[i][1], LIGHT_COORDS[i][0], LIGHT_TYPE[i]);
  }
}

// ==================== BLE 函数实现 ====================
void initBLE() {
  BLEDevice::init(BLE_DEVICE_NAME);
  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new FireServerCallbacks());
  BLEService* pService = bleServer->createService(BLE_SERVICE_UUID);

  bleNotifyChar = pService->createCharacteristic(
    BLE_CHAR_NOTIFY_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY
  );
  bleNotifyChar->addDescriptor(new BLE2902());

  bleWriteChar = pService->createCharacteristic(
    BLE_CHAR_WRITE_UUID,
    BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE
  );
  bleWriteChar->setCallbacks(new FireWriteCallbacks());

  pService->start();
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06);
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("[BLE] 服务已启动，广播: " BLE_DEVICE_NAME);
}

void sendJson(const String& json) {
  if (!bleDeviceConnected || bleNotifyChar == nullptr) return;
  bleNotifyChar->setValue(json.c_str());
  bleNotifyChar->notify();
  Serial.print("[BLE] >> ("); Serial.print(json.length()); Serial.print("B) ");
  Serial.println(json.substring(0, min(80, (int)json.length())));
}

/** 连接后一次性推送静态配置 */
void pushStaticConfig() {
  // MAP_CONFIG
  String json = "{\"type\":\"MAP_CONFIG\",\"width\":10,\"height\":5,";
  json += "\"colWidths\":[60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0,60.0,80.0],";
  json += "\"rowHeights\":[80.0,60.0,80.0,60.0,80.0],";
  json += "\"walls\":[";
  bool first = true;
  for (int y = 0; y < MAP_HEIGHT; y++) {
    for (int x = 0; x < MAP_WIDTH; x++) {
      if (ORIGINAL_GRID[y][x] == 1) {
        if (!first) json += ","; first = false;
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
  delay(500);

  // LIGHT_CONFIG
  String lj = "{\"type\":\"LIGHT_CONFIG\",\"lights\":[";
  for (int i = 0; i < lightCount; i++) {
    if (i > 0) lj += ",";
    lj += "{\"id\":"; lj += lights[i].index;
    lj += ",\"x\":"; lj += lights[i].pos.x;
    lj += ",\"y\":"; lj += lights[i].pos.y;
    lj += ",\"type\":\"";
    switch (lights[i].type) {
      case HORIZONTAL_UP: lj += "HU"; break;
      case HORIZONTAL_DOWN: lj += "HD"; break;
      case VERTICAL_LEFT: lj += "VL"; break;
      case VERTICAL_RIGHT: lj += "VR"; break;
    }
    lj += "\"}";
  }
  lj += "]}";
  Serial.print("[BLE] LIGHT_CONFIG len="); Serial.println(lj.length());
  Serial.println(lj);
  sendJson(lj);
}

/** 推送火灾点和方向 */
void pushStateUpdate() {
  // FIRE_UPDATE
  String fj = "{\"type\":\"FIRE_UPDATE\",\"fires\":[";
  for (int i = 0; i < fireCount; i++) {
    if (i > 0) fj += ",";
    fj += "["; fj += fires[i].x; fj += ","; fj += fires[i].y; fj += "]";
  }
  fj += "]}";
  sendJson(fj);
  delay(50);

  // DIRECTION_UPDATE
  String dj = "{\"type\":\"DIRECTION_UPDATE\",\"directions\":{";
  for (int i = 0; i < lightCount; i++) {
    if (i > 0) dj += ",";
    dj += "\""; dj += lights[i].index; dj += "\":"; dj += lights[i].direction;
  }
  dj += "}}";
  sendJson(dj);
}

/** 推送语音模式 */
void pushVoiceMode() {
  String mn = (voiceMode == 0) ? "IDLE" : (voiceMode == 1 ? "ESCAPE" : "RESCUE");
  bool trapped = (voiceMode == 2);
  String vj = "{\"type\":\"VOICE_MODE\",\"mode\":";
  vj += voiceMode; vj += ",\"modeName\":\""; vj += mn; vj += "\"";
  vj += ",\"hasTrapped\":"; vj += trapped ? "true" : "false"; vj += "}";
  sendJson(vj);
}

/** 定时推送：传感器状态 + 心跳 */
void blePeriodicPush() {
  if (!bleDeviceConnected) return;
  unsigned long now = millis();
  if (now - lastBleSensorPush >= BLE_SENSOR_INTERVAL) {
    lastBleSensorPush = now;
    String sj = "{\"type\":\"SENSOR_STATE\",\"states\":[";
    for (int i = 0; i < 13; i++) {
      if (i > 0) sj += ","; sj += sensorLastState[i] ? "true" : "false";
    }
    sj += "]}";
    sendJson(sj);
  }
  if (now - lastBleHeartbeat >= BLE_HEARTBEAT_INTERVAL) {
    lastBleHeartbeat = now;
    String hj = "{\"type\":\"HEARTBEAT\",\"uptime\":";
    hj += millis(); hj += ",\"fireCount\":"; hj += fireCount; hj += "}";
    sendJson(hj);
  }
}

/** 处理 APP 命令 */
void processBleCommand(const String& cmd) {
  if (cmd.startsWith("SET_LIGHT:")) {
    int c1 = cmd.indexOf(':', 10); int id = cmd.substring(10, c1).toInt();
    int dir = cmd.substring(c1 + 1).toInt();
    if (id >= 1 && id <= lightCount && dir >= 0 && dir <= 4) {
      lights[id - 1].setDirection(dir); updateHardware();
      sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"SET_LIGHT\",\"status\":\"OK\"}");
    }
  } else if (cmd.startsWith("ADD_FIRE:")) {
    int comma = cmd.indexOf(',', 9);
    addFire(Position(cmd.substring(9, comma).toInt(), cmd.substring(comma + 1).toInt()));
    sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"ADD_FIRE\",\"status\":\"OK\"}");
  } else if (cmd.startsWith("REMOVE_FIRE:")) {
    String param = cmd.substring(12);
    if (param == "ALL") { while (fireCount > 0) removeFire(fires[0]); }
    else {
      int comma = param.indexOf(',');
      removeFire(Position(param.substring(0, comma).toInt(), param.substring(comma + 1).toInt()));
    }
    sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"REMOVE_FIRE\",\"status\":\"OK\"}");
  } else if (cmd == "SYSTEM_RESET") {
    while (fireCount > 0) removeFire(fires[0]);
    computeAllPaths();
    sendJson("{\"type\":\"CMD_RESPONSE\",\"cmd\":\"SYSTEM_RESET\",\"status\":\"OK\"}");
  }
}

// ==================== setup + loop ====================
void setup() {
  Serial.begin(115200);
  delay(1000);
  initGridFlipped();
  Serial.println("========================================");
  Serial.println(" 火灾疏散引导系统 v2.1 (BLE 集成)");
  Serial.println("========================================");
  pinMode(PIN_DATA, OUTPUT); pinMode(PIN_CLOCK, OUTPUT); pinMode(PIN_LATCH, OUTPUT);
  pinMode(PIN_VOICE_ESCAPE, OUTPUT); pinMode(PIN_VOICE_RESCUE, OUTPUT);
  digitalWrite(PIN_DATA, LOW); digitalWrite(PIN_CLOCK, LOW); digitalWrite(PIN_LATCH, LOW);
  for (int i = 0; i < 9; i++) sendByte(0x00);
  digitalWrite(PIN_LATCH, HIGH); delayMicroseconds(10); digitalWrite(PIN_LATCH, LOW);
  initLedMapping();
  for (int i = 0; i < 13; i++) { pinMode(SENSOR_PINS[i], INPUT_PULLUP); sensorLastState[i] = false; }
  initData();
  initBLE();
  computeAllPaths();

  Serial.println("\n系统就绪，等待传感器触发...");
}

void loop() {
  unsigned long now = millis();

  // ===== BLE 连接状态管理 =====
  if (!bleDeviceConnected && oldBleDeviceConnected) {
    delay(500);
    bleServer->startAdvertising();
    Serial.println("[BLE] 重新开始广播");
    oldBleDeviceConnected = bleDeviceConnected;
  }
  if (bleDeviceConnected && !oldBleDeviceConnected) {
    oldBleDeviceConnected = bleDeviceConnected;
    // 连接成功，但等待 GATT 服务发现 + 通知启用后再推送
  }

  // 连接后延迟 2s 推送静态配置（等待 APP 端完成服务发现和通知启用）
  if (bleDeviceConnected && !bleConfigSent && (now - bleConnectTime > 2000)) {
    bleConfigSent = true;
    Serial.println("[BLE] 推送静态配置 + 当前状态");
    pushStaticConfig();
    delay(500);
    pushStateUpdate();
  }

  if (now - lastSensorCheck >= SENSOR_CHECK_INTERVAL) { lastSensorCheck = now; scanSensors(); }
  updateVoiceModule();

  // ★ BLE 定时推送
  blePeriodicPush();

  bool needsFlash = false;
  for (int i = 0; i < lightCount; i++) {
    int d = lights[i].direction;
    if (d == DIR_NO_PATH || (fireCount > 0 && isDirectionalLight(d))) { needsFlash = true; break; }
  }
  if (needsFlash && now - lastFlashToggle >= FLASH_INTERVAL) { lastFlashToggle = now; yellowFlashOn = !yellowFlashOn; updateHardware(); }
  delay(10);
}
