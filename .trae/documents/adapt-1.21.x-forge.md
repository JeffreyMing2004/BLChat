# 完整适配 1.21.x Forge 分支

## Context / 背景

`1.21.x/Forge` 分支当前**不可用**，与项目其它分支架构不一致：

- `Bilibilichatmcforge.java` 主类**未注册任何服务端事件**（无 `@SubscribeEvent onServerStarting/onServerStopping`），导致 `startBilibiliClient/stopBilibiliClient` 从不被调用 → Mod 实际从未启动 B 站连接。
- `BilibiliClient.java` 使用**非官方旧版** `wss://broadcastlv.chat.bilibili.com/sub` 直连（无需开放平台凭据），与 `1.20.x/Forge`、根目录 `src/` 使用的**官方开放平台 API v2**（`live-open.biliapi.com/v2/app/start`）完全不同。
- `mods.toml` 硬编码 `version = "1.0.2"`；`pack.mcmeta` 仍是 1.20.1 时代的 `pack_format: 15`。

**用户决策**：采用「开放平台 API v2 + 硬编码开发者凭据」架构，以 `1.20.x/Forge` 为镜像目标（用户只需填 `identityCode`，开发者 AccessKey/Secret/AppID 硬编码在代码中）。构建由用户本地验证。

**目标**：将 `1.20.x/Forge` 的成熟实现移植到 `1.21.x/Forge`，适配 Forge 1.21.11 / Java 21 的 API 差异，使 1.21.x 达到与 1.20.x 等价的功能。

## 关键发现：1.20.x 代码几乎可逐字移植

`1.20.x/Forge` 的业务代码几乎不使用版本敏感 API（`java.net.http.*` 标准库 + `Component.translatable` + `broadcastSystemMessage` 均在 1.21.x 稳定）。**唯一必须适配的点**：
- `Screen.renderBackground`：1.20.1 为 1 参 `(GuiGraphics)`；1.21.x 为 4 参 `(GuiGraphics, int, int, float)`（已由仓库内反编译的 `1.21.x/Forge/net/minecraft/client/gui/screens/Screen.java` 确认）。
- **颜色为严格 ARGB**：1.20.x 的 `0xFFFFFF`/`0xA0A0A0` 在 1.21.x 下 alpha=0 会**透明不可见**，必须改为 `0xFFFFFFFF`/`0xFFA0A0A0`（1.21.x 当前 `BilibiliConfigScreen` 已正确使用，佐证此差异）。
- `drawCenteredString(Font, Component, int, int, int)` / `drawString(Font, Component, int, int, int)` 在 1.21.11 仍存在（由 `GuiGraphics.java` 参考确认）。
- 主类用 `ModLoadingContext.get()`（1.21.x 已在用，现代 API），而非 1.20.x 的 `FMLJavaModLoadingContext.get()`——二者调用同一方法，保持 1.21.x 现状即可。

## 实施步骤 / 文件改动

### 1. `Bilibilichatmcforge.java`（重写）
镜像 `1.20.x/Forge` 主类逻辑，保留 `ModLoadingContext.get()`：
- 构造器：`JsonConfigManager.load()` → `MinecraftForge.EVENT_BUS.register(this)` → `addListener(this::onRegisterCommands)` → `ModLoadingContext.get().registerConfig(COMMON, Config.SPEC)` → `registerExtensionPoint(ConfigScreenFactory)`。
- 新增 `@SubscribeEvent onServerStarting`：创建 `BilibiliClient(server)` 并 `start()`。
- 新增 `@SubscribeEvent onServerStopping`：`bilibiliClient.stop()`。
- 新增 `onRegisterCommands(RegisterCommandsEvent)`：`/bilibili identitycode <id>` 指令（权限 2），更新 `JsonConfigManager.setIdentityCode` 并重启客户端。
- 保留 `restartClient()`；**删除**未被调用的 `startBilibiliClient/stopBilibiliClient` 静态方法。

### 2. `utils/BilibiliClient.java`（整体替换）
用 `1.20.x/Forge` 的实现整体替换当前旧版 broadcastlv 实现：
- Open Live API v2：`v2/app/start`（取 wss_link + auth_body + game_id）→ WebSocket 鉴权 → `v2/app/heartbeat` 每 20s → 关闭时 `v2/app/end`。
- 硬编码凭据常量（复用 1.20.x 中已公开的同一组 `ACCESS_KEY_ID`/`ACCESS_KEY_SECRET`/`APP_ID`），内联 `getHeaders/md5/hmacSha256`（与 1.20.x 一致，不拆分 BilibiliAuth）。
- 消息解析兼容新旧 CMD：`LIVE_OPEN_PLATFORM_*` + `OPEN_LIVEROOM_*`（开放平台新名）+ 旧版 `DANMU_MSG/SEND_GIFT/SUPER_CHAT_MESSAGE/GUARD_BUY`。
- 重连上限 `MAX_RECONNECT_ATTEMPTS=5`，5s 间隔。
- 跨线程广播：`server.execute(() -> server.getPlayerList().broadcastSystemMessage(...))`。
- 该文件无 MC GUI 代码，Java 21 标准库兼容，**可逐字移植**。

### 3. `Config.java`（重写为 1.20.x 版）
镜像 1.20.x：`IDENTITY_CODE` ForgeConfigSpec + `@Mod.EventBusSubscriber` + `onLoad(ModConfigEvent)` 同步到 `JsonConfigManager.setIdentityCode`。删除当前多余的 `getIdentityCode/setIdentityCode`。

### 4. `JsonConfigManager.java`（无需改动）
已与 1.20.x 完全一致（仅 `identityCode` 字段）。确认即可。

### 5. `client/BilibiliConfigScreen.java`（小改）
镜像 1.20.x 结构，做 2 处 1.21.x 适配：
- `render` 中补 `this.renderBackground(guiGraphics, mouseX, mouseY, partialTick)`（4 参），顺序：背景 → 标题/标签 → `super.render`。
- 颜色用 ARGB：标题 `0xFFFFFFFF`、标签 `0xFFA0A0A0`。
- `drawCenteredString/drawString` 用 `Component` 重载（与 1.20.x 一致，更干净）。`setHint(Component)`、`Button.builder`、`EditBox` 均已在 1.21.x 当前代码验证可用。

### 6. `resources/META-INF/mods.toml`
- `version` 改用 `${mod_version}`（build.gradle 的 processResources 已展开，比硬编码更规范）。
- 保留 `clientSideOnly = true`（与 1.20.x 一致；Forge 忽略此非标准字段，无害）。
- 依赖范围沿用 `gradle.properties`（`[1.21.11,1.22)` / `[61,62)`）。

### 7. `resources/pack.mcmeta`
- `pack_format: 15`（1.20.1）错误。改为 1.21.x 值并加 `supported_formats` 区间以兼容整个 1.21.x：
```json
{ "pack": { "description": "...", "pack_format": 34, "supported_formats": { "min_inclusive": 34, "max_inclusive": 99 } } }
```
（`supported_formats` 自 1.20.2 起支持，1.21.11 可用；具体值以构建期资源包警告为准微调。）

### 8. 语言文件（无需改动）
`en_us.json` / `zh_cn.json` 已与 1.20.x 完全一致，覆盖所有用到的键（`config.*`/`chat.*`/`error.*`/`info.*`）。

### 不改动
- `build.gradle` / `gradle.properties` / `settings.gradle`：已正确配置 1.21.11 / Forge 61.1.5 / Java 21 / shadow 插件 / 资源合并 hack。
- 仓库根的 `fg_src/`、`fml_src/`、`net/minecraft/...` 反编译参考文件：仅作 API 参考，不参与编译，保留。

## 安全提示（超范围，仅告知）
1.20.x 已将真实 Bilibili 开放平台凭据硬编码于 `BilibiliClient.java` 并提交仓库。按用户选定方案，1.21.x 将复用同一组凭据。建议后续在 B 站开放平台轮换密钥，并考虑改为环境变量/配置注入。本次任务不处理。

## 验证（用户本地执行）
1. `cd 1.21.x\Forge ; .\gradlew build` —— 确认编译通过。
2. 预期最可能的报错点（优先排查）：`pack.mcmeta` 的 pack_format 警告（按警告值微调）、`renderBackground` 签名。
3. 将 jar 放入 1.21.11-Forge 客户端+服务端，通过配置界面或 `/bilibili identitycode <身份码>` 设置身份码。
4. 端到端验证：连接成功广播 → 弹幕/礼物/SC/大航海显示 → 20s 心跳无掉线 → 断线 5s 重连（≤5 次）→ 停服时 `v2/app/end` 正常结束。
