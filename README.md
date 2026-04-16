# DG-LAB Lib - Minecraft Forge 模组

一个用于 Minecraft Forge 1.20.1 的 DG-LAB 设备控制库，通过 WebSocket 连接到 DG-LAB 后端，支持设备配对和强度控制。

## 功能特性

- ✅ WebSocket 连接管理
- ✅ 设备自动配对（扫描二维码）
- ✅ 两通道强度控制（0-200 范围）
- ✅ 基础波形发送（官方 clientMsg 格式）
- ✅ 实时状态查询
- ✅ 自动重连机制
- ✅ 游戏内 GUI 界面

## 游戏内命令

所有命令使用统一前缀 `/dg`

### `/dg connect`
**功能**：连接到 DG-LAB 后端并自动显示配对二维码界面

**使用**：
```
/dg connect
```

**效果**：
- 连接到配置的 WebSocket 地址
- 自动弹出二维码 UI（等待约 10 秒或连接成功后立即显示）
- 游戏聊天显示连接状态

---

### `/dg disconnect`
**功能**：断开与 DG-LAB 后端的连接

**使用**：
```
/dg disconnect
```

---

### `/dg status`
**功能**：查询当前设备连接状态和强度信息

**使用**：
```
/dg status
```

**输出示例**：
```
[DG Lib] State: PAIRED | Paired: YES | Client: deac65d2 | Target: sd73sjgf
        | Strength A: 50/200 | Strength B: 75/200
```

**状态值说明**：
- `IDLE`：未连接
- `CONNECTING`：连接中
- `CONNECTED`：已连接，等待配对
- `PAIRED`：已配对，设备就绪

---

### `/dg pair`
**功能**：检查当前配对状态

**使用**：
```
/dg pair
```

**输出示例**：
- 已配对：`[DG Lib] Paired! Target: sd73sjgf`
- 未配对：`[DG Lib] Not paired. Scan /dg qr with your phone.`

---

### `/dg qr`
**功能**：打开二维码 UI 界面进行设备配对

**使用**：
```
/dg qr
```

**界面说明**：
- 显示配对二维码
- 扫描二维码使用 DG-LAB 手机应用进行配对
- 按 ESC 关闭界面

---

### `/dg ui`
**功能**：打开强度控制设置界面

**使用**：
```
/dg ui
```

**界面说明**：
- **通道 A 滑动条**：控制通道 A 的强度上限（0-200）
- **通道 B 滑动条**：控制通道 B 的强度上限（0-200）
- 实时显示当前值
- 拖动滑动条时实时发送指令到设备
- 按 ESC 或点击 "Close" 按钮关闭

---

### `/dg set <channel> <value>`
**功能**：直接设置指定通道的强度值

**使用**：
```
/dg set 1 150     # 设置通道 A（通道 1）强度为 150
/dg set 2 100     # 设置通道 B（通道 2）强度为 100
```

**参数说明**：
- `<channel>`：通道号（1 = 通道 A，2 = 通道 B）
- `<value>`：强度值（0-200）

**返回**：
- 成功：`[DG Lib] Set Ch1 to 150 sent.`
- 失败：`[DG Lib] Not paired or unavailable.`

---

### `/dg wave <channel> <seconds>`
**功能**：发送最基础的官方格式波形（type = clientMsg）

**使用**：
```
/dg wave 1 3
/dg wave 2 5
```

**参数说明**：
- `<channel>`：通道号（1 = 通道 A，2 = 通道 B）
- `<seconds>`：持续时长（1-10 秒）

**协议说明**：
- 按官方 v2 文档格式发送：`type: "clientMsg"`
- `channel` 使用 `A` / `B`
- `message` 使用 `A:["HEX", ...]` 或 `B:["HEX", ...]`
- 服务端会转发为 `pulse-...` 给 APP

---

### `/dg_server`（多人联机服务端指令）
需要 OP 权限（权限等级 2），用于指定玩家并远程调用该玩家客户端 DG Lib。

#### `/dg_server set <player> <channel> <value>`
- 示例：`/dg_server set Steve 1 150`
- 含义：设置 `Steve` 的通道 1 强度为 150。

#### `/dg_server wave <player> <channel> <seconds>`
- 示例：`/dg_server wave Steve 2 5`
- 含义：让 `Steve` 的通道 2 播放 5 秒基础波形。

说明：
- 指令通过服务端 -> 目标玩家客户端网络请求执行，再回传结果。
- 若目标玩家未配对或本地 DG Lib 不可用，指令会返回失败提示。

---

## Java API 调用

如果你是模组开发者，可以在代码中直接使用 DG Lib 提供的 API。

### 获取 API 实例

```java
import org.nooberic.dg_lib.api.DgLibApi;

DgLibApi api = DgLibApi.get();
```

### 可用方法

#### 连接管理

```java
// 连接到后端
api.connect();

// 断开连接
api.disconnect();

// 获取当前连接状态
ConnectionState state = api.getConnectionState();
// 可能的值：IDLE, CONNECTING, CONNECTED, PAIRED
```

#### 配对状态

```java
// 检查是否已配对
boolean isPaired = api.isPaired();
```

#### 状态查询

```java
// 获取设备当前状态
DeviceStatus status = api.getStatus();

// 访问状态信息
String clientId = status.getClientId();           // 本端 ID
String targetId = status.getTargetId();           // 对端 ID
int channelAStrength = status.getChannelAStrength();  // 通道 A 当前强度
int channelBStrength = status.getChannelBStrength();  // 通道 B 当前强度
int channelALimit = status.getChannelALimit();        // 通道 A 上限
int channelBLimit = status.getChannelBLimit();        // 通道 B 上限
String wsUrl = status.getWsUrl();                     // WebSocket 地址
String lastError = status.getLastErrorCode();         // 最后一条错误

// 快速读取通道当前强度和上限
int currentA = api.getCurrentStrength(1);             // 通道 A 当前强度
int limitA = api.getStrengthLimit(1);                 // 通道 A 当前上限
```

#### 强度回传监听（实时）

```java
import org.nooberic.dg_lib.service.StrengthFeedbackListener;

api.setStrengthFeedbackListener(new StrengthFeedbackListener() {
    @Override
    public void onStrengthFeedback(int aStrength, int bStrength, int aLimit, int bLimit) {
        System.out.println("A=" + aStrength + "/" + aLimit + ", B=" + bStrength + "/" + bLimit);
    }
});
```

说明：
- 当收到 APP 回传的 `strength-A+B+ALimit+BLimit` 消息时会触发回调。
- 传入 `null` 可取消监听：`api.setStrengthFeedbackListener(null);`

#### 强度控制

```java
// 设置通道强度（1 = 通道 A，2 = 通道 B，值范围 0-200）
boolean success = api.setStrength(1, 150);

// 增加强度（返回 true 表示成功）
boolean success = api.increaseStrength(1, 10);

// 减少强度（返回 true 表示成功）
boolean success = api.decreaseStrength(1, 10);

// 发送基础波形（channel: 1=A, 2=B, seconds: 1-10）
boolean success = api.playBasicWave(1, 5);
```

#### 初始化和清理

```java
// 初始化 API（通常自动调用，不需要手动调用）
api.initialize();

// 清理资源（模组卸载时自动调用）
api.shutdown();
```

### 使用示例

```java
import org.nooberic.dg_lib.api.DgLibApi;
import org.nooberic.dg_lib.service.ConnectionState;

public class MyModIntegration {
    public void connectAndControl() {
        DgLibApi api = DgLibApi.get();
        
        // 连接设备
        api.connect();
        
        // 检查连接状态
        ConnectionState state = api.getConnectionState();
        if (state == ConnectionState.PAIRED) {
            // 设置强度
            api.setStrength(1, 100);  // 通道 A 设置为 100
            api.setStrength(2, 80);   // 通道 B 设置为 80
            
            // 查询状态
            var status = api.getStatus();
            System.out.println("Channel A: " + status.getChannelAStrength() + "/" + status.getChannelALimit());
        }
    }
}
```

## 多人联机服务端 API

支持在服务端通过 `ServerPlayer` 获取该玩家对应的 Coyote 远程对象，操作会通过网络下发到该玩家客户端的 DG Lib 执行，再把结果回传服务端。

### 获取玩家 Coyote 对象

```java
import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dg_lib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dg_lib.multiplayer.ServerCoyote;

ServerPlayer targetPlayer = ...;
ServerCoyote coyote = DgServerCoyoteApi.get().getCoyote(targetPlayer);
```

### 常用操作

```java
// 设置强度（异步）
coyote.setStrength(1, 120).thenAccept(success -> {
    if (success) {
        // 已下发并执行成功
    }
});

// 查询当前强度/上限（会向客户端请求并刷新）
coyote.getCurrentStrength(1).thenAccept(aStrength -> {
    // A 通道当前强度
});

coyote.getStrengthLimit(1).thenAccept(aLimit -> {
    // A 通道当前上限
});

// 获取完整状态快照
coyote.refreshStatus().thenAccept(snapshot -> {
    int a = snapshot.getChannelAStrength();
    int b = snapshot.getChannelBStrength();
    int aLimit = snapshot.getChannelALimit();
    int bLimit = snapshot.getChannelBLimit();
});
```

### 已覆盖的联机关键点

- Player -> Coyote 映射缓存（服务端维护）
- 请求-响应关联（requestId）
- 响应来源校验（必须由目标玩家客户端返回）
- 超时保护（默认 5 秒）
- 玩家下线自动清理映射与挂起请求

### 建议你在业务层补充的策略

- 权限控制：只允许特定身份/关系的玩家发起远程控制
- 同意机制：被控玩家是否允许被谁控制
- 频率限制：避免短时间高频调度造成刷包
- 失败重试：对超时和离线进行可控重试
- 审计日志：记录谁在什么时候控制了谁

## 配置

### WebSocket 连接地址

默认连接地址配置在游戏配置目录中，路径为：
```
config/dg_lib-common.toml
```

### 修改连接地址

编辑 `dg_lib-common.toml` 文件，修改以下配置：
```toml
wsUrl = "wss://ws.dungeon-lab.cn/"
```

推荐连接方案：
- 手机扫码优先使用公网端点：`wss://ws.dungeon-lab.cn/`（与官方网页示例一致）
- 本地 mock 后端调试使用：`ws://你的局域网IP:9999`（不要使用 `127.0.0.1`）

## 事件和回调

当前版本通过命令系统和游戏聊天提供反馈。如需集成事件系统，请参考 Forge 事件总线的使用。

## 状态代码

### ConnectionState 值

| 值 | 说明 |
|---|---|
| `IDLE` | 未连接 |
| `CONNECTING` | 连接中 |
| `CONNECTED` | 已连接，等待配对 |
| `PAIRED` | 已配对，设备就绪 |

## 错误处理

### 常见错误

| 错误 | 原因 | 解决方案 |
|---|---|---|
| "Waiting for connection" | 后端未连接 | 检查 WebSocket 地址，确保后端运行 |
| "Not paired or unavailable" | 设备未配对或连接断开 | 执行 `/dg qr` 扫描二维码重新配对 |
| "Failed to open QR UI" | 二维码界面打开失败 | 检查游戏日志，尝试重新连接 |

## 日志输出

模组使用 SLF4J 日志框架，日志输出到 `logs/latest.log`。

关键日志前缀：
- `[DG Lib]` - 游戏聊天消息（用户可见）
- `[or.no.dg.cl.JdkWsTransportClient]` - WebSocket 连接日志
- `[or.no.dg.se.DgLabServiceImpl]` - 服务层日志
- `[QrCodeScreen]` / `[StrengthControlScreen]` - UI 日志

## 开发依赖

如果你要在自己的模组中使用 DG Lib，需要：

1. 添加模组依赖配置（gradle 中）
2. 在 `src/main/resources/META-INF/mods.toml` 中声明依赖

## 许可证

待定

## 反馈和问题

如遇到问题，请：
1. 检查 `logs/latest.log` 中的错误信息
2. 确保 WebSocket 后端正常运行
3. 验证游戏和 Forge 版本为 1.20.1
