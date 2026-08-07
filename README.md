# DG-LAB Lib - Minecraft Forge 模组

一个用于 Minecraft Forge 1.20.1 的 DG-LAB 设备控制库，通过 WebSocket 连接到 DG-LAB 后端，支持设备配对和强度控制。

## 发布/依赖信息

- Group ID：`org.nooberic`
- Artifact ID：`dglib`
- Mod ID：`dglib`
- 当前版本：`1.3.3`
- 兼容版本：Minecraft `1.20.1` / Forge `47.4.20`

### 版本说明

- 当前版本为稳定版本：`1.3.3`

### 作为其他模组的依赖

建议在依赖方使用：

- 编译期：`compileOnly`
- 运行期：`runtimeOnly`

如果你希望强制前置，则在依赖方的 `mods.toml` 中声明 `mandatory=true`。

#### Gradle 依赖示例

```groovy
repositories {
    maven {
        url = uri("https://nooberic0746.github.io/dg-lib/")
    }
}

dependencies {
    compileOnly fg.deobf("org.nooberic:dglib:1.3.3")
    runtimeOnly fg.deobf("org.nooberic:dglib:1.3.3")
}
```

#### 依赖方 `mods.toml` 示例

```toml
[[dependencies.yourmodid]]
modId = "dglib"
mandatory = true
versionRange = "[1.3.3,)"
ordering = "AFTER"
side = "BOTH"
```

### 运行时入口

- 完整实例 API：`org.nooberic.dglib.api.DgLibApi`
- 客户端静态 API：`org.nooberic.dglib.coyote.api.CoyoteClientApi`
- 多人联机服务端 API：`org.nooberic.dglib.multiplayer.DgServerCoyoteApi`

### 已注册的内置波形

- `basic_breath`
- `chaos`
- `const`

### 给其他开发者的接入教程

如果你正在编写另一个 Forge `1.20.1` 模组，并希望把 `DG Lib` 作为前置库接入，可以按下面步骤配置。

#### 第一步：在 `build.gradle` 添加 Maven 仓库

```groovy
repositories {
    maven {
        url = uri("https://nooberic0746.github.io/dg-lib/")
    }
}
```

#### 第二步：添加依赖坐标

```groovy
dependencies {
    compileOnly fg.deobf("org.nooberic:dglib:1.3.3")
    runtimeOnly fg.deobf("org.nooberic:dglib:1.3.3")
}
```

说明：

- `compileOnly`：用于编译时引用 API
- `runtimeOnly`：用于开发运行时把 `DG Lib` 一起加载进游戏

#### 第三步：在你的 `mods.toml` 声明前置依赖

```toml
[[dependencies.yourmodid]]
modId = "dglib"
mandatory = true
versionRange = "[1.3.3,)"
ordering = "AFTER"
side = "BOTH"
```

请把 `yourmodid` 替换成你自己的模组 ID。

#### 第四步：在代码中使用 API

运行时入口类：

- `org.nooberic.dglib.api.DgLibApi`

示例：

```java
import org.nooberic.dglib.api.DgLibApi;
```

然后根据你的需求调用 `DgLibApi` 提供的公开能力。

#### 第五步：版本

当前发布版本为：

- `org.nooberic:dglib:1.3.3`

对应地，依赖方的 `versionRange` 可以写成兼容当前版本的范围，例如：

```toml
versionRange = "[1.3.3,2.0.0)"
```

## 功能特性

- ✅ WebSocket 连接管理
- ✅ 设备自动配对（扫描二维码）
- ✅ 两通道强度控制（0-200 范围，支持 `channel=3` 同时操作 A/B）
- ✅ 内置与自定义波形发送（`.frame` / `Pulse`）
- ✅ 实时状态查询
- ✅ 自动重连机制
- ✅ 游戏内 GUI 界面
- ✅ 本地调度式 `control` / `controlsoft`（同一通道的新任务替换当前任务）
- ✅ 直接执行的 `set` / `wave` / `increase` / `decrease` / `clear`

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
    | Strength A: 50/200 | Strength B: 75/200 | Pain A/B: 30/40 | Floor A/B: 10/12
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
/dg set 3 80      # 同时设置通道 A 和 B 为 80
```

**参数说明**：
- `<channel>`：通道号（`1` = 通道 A，`2` = 通道 B，`3` = 同时对 A/B 两个通道执行）
- `<value>`：强度值（0-200）

**返回**：
- 成功：`[DG Lib] Set Ch1 to 150 sent.`
- 双通道成功：`[DG Lib] Set Ch1+2 to 80 sent.`
- 失败：`[DG Lib] Not paired or unavailable.`

**安全值处理**：
- 当已收到设备回传的通道安全上限后，若设置值超过该上限，会自动下调到安全上限再发送。
- 不会因为超限而直接丢弃操作。
- `set` 为直接执行，不进入本地调度器。

---

### `/dg setsoft <channel> <value>`

**功能**：使用 0-100 的软强度值设置通道强度。

**使用**：
```
/dg setsoft 1 50
/dg setsoft 3 40
```

**参数说明**：
- `<channel>`：`1` = 通道 A，`2` = 通道 B，`3` = 同时设置 A/B
- `<value>`：软强度值（0-100），会根据通道配置换算为设备强度

`setsoft` 是直接执行，不进入本地调度器。

---

### `/dg increase <channel> <delta>`
**功能**：直接增加指定通道的强度值

**使用**：
```
/dg increase 1 10
/dg increase 3 5
```

**参数说明**：
- `<channel>`：通道号（`1` = 通道 A，`2` = 通道 B，`3` = 同时对 A/B 两个通道执行）
- `<delta>`：增加值（1-200）

**说明**：
- `increase` 为直接执行，不进入本地调度器。

---

### `/dg decrease <channel> <delta>`
**功能**：直接减少指定通道的强度值

**使用**：
```
/dg decrease 2 10
/dg decrease 3 5
```

**参数说明**：
- `<channel>`：通道号（`1` = 通道 A，`2` = 通道 B，`3` = 同时对 A/B 两个通道执行）
- `<delta>`：减少值（1-200）

**说明**：
- `decrease` 为直接执行，不进入本地调度器。

---

### `/dg clear <channel>`
**功能**：清空指定通道的调度内容，并立即清空当前波形与强度

**使用**：
```
/dg clear 1
/dg clear 3
```

**说明**：
- `clear` 会调用清波并把强度重置为 `0`。
- `channel=3` 时会同时作用于 A/B 两个通道。
- 该命令用于结束当前输出并清空调度状态。

---

### `/dg control <channel> <strength> <pulse_id> <seconds>`
**功能**：把一个已注册波形加入本地调度器并按指定强度播放

**使用**：
```
/dg control 1 80 basic_breath 3
/dg control 2 60 chaos 1
/dg control 3 40 const 5
```

**参数说明**：
- `<channel>`：通道号（`1` = 通道 A，`2` = 通道 B，`3` = 同时对 A/B 两个通道分别调度）
- `<strength>`：目标强度（0-200）
- `<pulse_id>`：已注册波形 ID，例如 `basic_breath`、`chaos`、`const`
- `<seconds>`：持续时长（整数 `1-60` 秒）

**调度说明**：
- `control` 使用本地调度器管理波形播放，而不是简单的一次性直发。
- 高优先级（更高强度）的任务会抢占低优先级任务。
- 同一通道的新调度会替换当前调度任务。
- `channel=3` 会拆成通道 A / B 两条独立调度任务，各自独立抢占和结束，互不影响。
- 调度执行时会按 `control` 传入的目标通道重建运行时波形，不会沿用已注册波形对象里原始的通道字段。

**返回**：
- 成功：`[DG Lib] Control sent. Ch1 strength=80, pulse='basic_breath', 3.0s.`
- 双通道成功：`[DG Lib] Control sent. Ch1+2 strength=40, pulse='const', 5.0s.`
- 波形不存在：`[DG Lib] Unknown pulse id: ...`

---

### `/dg controlsoft <channel> <strength> <pulse_id> <seconds>`

**功能**：使用 0-100 的软强度值调度波形。库会按每个通道的配置换算成设备强度。

**使用**：
```
/dg controlsoft 1 50 basic_breath 5
/dg controlsoft 3 40 const 10
```

参数范围与 `/dg control` 相同，但 `<strength>` 为 `0-100`。

---

### `/dg wave <channel> <pulse_id> <seconds>`
**功能**：直接播放指定波形，不经过调度器强度优先级管理

**使用**：
```
/dg wave 1 basic_breath 3
/dg wave 3 chaos 5
```

**参数说明**：
- `<channel>`：`1` = 通道 A，`2` = 通道 B，`3` = 同时播放到 A/B
- `<pulse_id>`：已注册波形 ID
- `<seconds>`：持续时长（1-60 秒，整数）

**说明**：
- 该命令会把波形内容按给定秒数直接发送到设备。
- `wave` 会根据命令中的通道重建运行时波形。
- `wave` 为直接执行，不进入本地调度器。

---

### `/dg_server`（多人联机服务端指令）
需要 OP 权限（权限等级 2），用于指定玩家并远程调用该玩家客户端 DG Lib。

#### `/dg_server set <player> <channel> <value>`
- 示例：`/dg_server set Steve 1 150`
- 示例：`/dg_server set Steve 3 80`
- 含义：设置 `Steve` 的通道 1，或在 `channel=3` 时同时设置 A/B 两个通道。

#### `/dg_server increase <player> <channel> <delta>`
- 示例：`/dg_server increase Steve 1 10`
- 示例：`/dg_server increase Steve 3 5`
- 含义：直接增加目标玩家指定通道强度，不走调度器。

#### `/dg_server decrease <player> <channel> <delta>`
- 示例：`/dg_server decrease Steve 2 10`
- 示例：`/dg_server decrease Steve 3 5`
- 含义：直接减少目标玩家指定通道强度，不走调度器。

#### `/dg_server clear <player> <channel>`
- 示例：`/dg_server clear Steve 1`
- 示例：`/dg_server clear Steve 3`
- 含义：清空目标玩家该通道当前波形并将强度归零，同时清理对应调度状态。

#### `/dg_server control <player> <channel> <strength> <pulse_id> <seconds>`
- 示例：`/dg_server control Steve 1 60 basic_breath 5`
- 示例：`/dg_server control Steve 3 40 const 10`
- 含义：请求 `Steve` 客户端本地 DG Lib 执行调度式波形控制。
- 服务端命令与客户端本地 `/dg control` 的 `<seconds>` 都是整数秒（1-60）。

#### `/dg_server wave <player> <pulse_id> <seconds>`
- 示例：`/dg_server wave Steve chaos 5`
- 含义：让 `Steve` 客户端直接播放指定波形 `5` 秒。

#### `/dg_server status <player>`
- 示例：`/dg_server status Steve`
- 含义：通过网络向 `Steve` 客户端请求最新状态并回传显示（含强度、上限、痛感强度、感受下限）。

说明：
- 指令通过服务端 -> 目标玩家客户端网络请求执行，再回传结果。
- 若目标玩家未配对或本地 DG Lib 不可用，指令会返回失败提示。

---

## Java API 调用

如果你是模组开发者，可以在代码中直接使用 DG Lib 提供的 API。

### 获取 API 实例

```java
import org.nooberic.dglib.api.DgLibApi;

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

// UI 附加参数（供附属模组读取/写入）
int painA = api.getPainStrength(1);                   // 通道 A 痛感强度
int floorA = api.getSensationLowerLimit(1);           // 通道 A 感受下限
api.setPainStrength(1, 60);
api.setSensationLowerLimit(1, 20);
```

#### 强度回传监听（实时）

```java
import org.nooberic.dglib.service.StrengthFeedbackListener;

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
- `pain` / `sensation floor` 会持久化到 `config/dglib-common.toml`，重启游戏后仍保留。

## 快捷键

- `U`：打开 DG 强度控制界面
- `I`：急停（将 A/B 强度置 0 并断开配对）

#### 强度控制与调度

```java
// 设置通道强度（1 = 通道 A，2 = 通道 B，3 = 同时操作 A/B，值范围 0-200）
boolean setOk = api.setStrength(1, 150);

// 同时设置两个通道
boolean dualSet = api.setStrength(3, 80);

// 清空当前通道的波形并把强度置 0
boolean clearOk = api.clear(3);

// 增加强度（返回 true 表示成功）
boolean increaseOk = api.increaseStrength(1, 10);

// 减少强度（返回 true 表示成功）
boolean decreaseOk = api.decreaseStrength(1, 10);

// 调度式波形控制（channel: 1=A, 2=B, 3=A+B；seconds 为 1-60 的整数）
boolean controlOk = api.control(3, 50, "const", 2);

// 软强度调度（softStrength 为 0-100）
boolean softControlOk = api.controlSoft(1, 50, "const", 5);

// 发送基础波形（channel: 1=A, 2=B, 3=A+B；seconds: 1-60）
boolean waveOk = api.playBasicWave(1, 5);
```

说明：
- `setStrength()`、`clear()`、`increaseStrength()`、`decreaseStrength()`、`playBasicWave()` / `playPulse()` 都是直接执行。
- 只有 `control()` 会进入本地调度器。
- 注册波形表中保存的是模板数据（`id + frames`）；真正下发到设备的执行波形会在运行时再附带 `channel + seconds`。

#### 初始化和清理

```java
// 初始化 API（通常自动调用，不需要手动调用）
api.initialize();

// 清理资源（模组卸载时自动调用）
api.shutdown();
```

### 使用示例

```java
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.service.ConnectionState;

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

            // 按已注册波形 ID 调度播放（seconds 为整数秒）
            api.control(3, 60, "basic_breath", 2);
            
            // 查询状态
            var status = api.getStatus();
            System.out.println("Channel A: " + status.getChannelAStrength() + "/" + status.getChannelALimit());
        }
    }
}
```

### `.frame` 波形文件与转换

当前运行时使用的是 `.frame` 文件，内容只保留 `frames` 数组；加载后会转换为内存中的 `Pulse` 对象。

相关类：

- `RegisteredPulse`：注册表中的波形模板（id + frames）
- `Pulse`：发送到设备的运行时波形（channel + seconds + frames）
- `PulseFileParser`：解析 `.frame` 文件为 `RegisteredPulse`
- `PulseRegistry`：全局波形注册表（id -> `RegisteredPulse`）

`src/main/resources/pulse/` 目录用于存放 `.frame` 文件。

支持的 `.frame` 内容格式：

1. 对象格式（推荐）

```json
{
    "frames": [
        "0A0A0A0A00000000",
        "0A0A0A0A14141414",
        "0A0A0A0A64646464"
    ]
}
```

2. 纯数组格式

```json
[
    "0A0A0A0A00000000",
    "0A0A0A0A14141414",
    "0A0A0A0A64646464"
]
```

约束：
- `frames`: 每帧必须为 16 位 HEX 字符串

说明：
- 当前运行时最终只读取 `frames`；通道和时长由命令或 Java API 在播放时指定。

### 内置波形

当前客户端启动时会自动注册以下内置波形：

- `basic_breath`
- `chaos`
- `const`

### 波形注册机制（文件名 + 游戏内 id）

```java
import org.nooberic.dglib.api.DgLibApi;
import org.nooberic.dglib.pulse.RegisteredPulse;

DgLibApi api = DgLibApi.get();

// 注册：文件位于 src/main/resources/pulse/my_wave.frame
boolean ok = api.registerPulse("my_wave_id", "my_wave.frame");

// 全局获取（主模组和附属模组都可按 id 获取）
RegisteredPulse pulse = api.getPulse("my_wave_id");
if (pulse != null) {
    api.playPulse(1, pulse, 5);
}
```

可获取全部已注册波形：

```java
var all = api.getAllPulses(); // Map<String, RegisteredPulse>
```

## 多人联机服务端 API

支持在服务端通过 `ServerPlayer` 获取该玩家对应的 Coyote 远程对象，操作会通过网络下发到该玩家客户端的 DG Lib 执行，再把结果回传服务端。

### 获取玩家 Coyote 对象

```java
import net.minecraft.server.level.ServerPlayer;
import org.nooberic.dglib.multiplayer.DgServerCoyoteApi;
import org.nooberic.dglib.multiplayer.ServerCoyote;

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

// 调度式波形控制（seconds 当前为整数秒）
coyote.control(3, 50, "const", 5).thenAccept(success -> {
    // 让目标客户端同时调度 A/B 两个通道
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
config/dglib-common.toml
```

### 修改连接地址

编辑 `dglib-common.toml` 文件，修改以下配置：
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
| "Unknown pulse id" | 波形 ID 未注册 | 检查 `src/main/resources/pulse/` 中的 `.frame` 文件是否已注册 |
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
3. 验证 Minecraft 版本为 `1.20.1`、Forge 版本为 `47.4.20`。
