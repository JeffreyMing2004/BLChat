## BLChat v1.0.3 - Minecraft 1.20.x(1.20.1-1.20.6) Forge

### B站直播开放平台正式版适配 / Bilibili Live Open Platform Official API Integration

本版本全面适配B站直播开放平台正式版API，使用Allatori进行代码混淆保护。
This version fully integrates with the Bilibili Live Open Platform official API, with Allatori code obfuscation protection.

---

### ✨ 新特性 / New Features

- **适配B站直播开放平台正式版API** - 使用 `v2/app/start` 接口连接直播间
  **Bilibili Live Open Platform API Integration** - Uses `v2/app/start` endpoint to connect to live rooms

- **简化配置流程** - 用户只需设置身份码（Identity Code），Access Key等凭证已内置
  **Simplified Configuration** - Users only need to set the Identity Code; Access Key credentials are built-in

- **支持直播开放平台消息格式** - 兼容 `OPEN_LIVEROOM_DM`、`OPEN_LIVEROOM_SEND_GIFT`、`OPEN_LIVEROOM_SUPER_CHAT`、`OPEN_LIVEROOM_GUARD` 等CMD
  **Live Open Platform Message Support** - Compatible with `OPEN_LIVEROOM_DM`, `OPEN_LIVEROOM_SEND_GIFT`, `OPEN_LIVEROOM_SUPER_CHAT`, `OPEN_LIVEROOM_GUARD` and other CMDs

- **自动重连机制** - WebSocket断开后自动重连，最多重试5次
  **Auto Reconnection** - Automatically reconnects WebSocket after disconnection, up to 5 retries

- **双重心跳保活** - WebSocket心跳（30秒）+ 项目心跳（20秒）
  **Dual Heartbeat Keep-alive** - WebSocket heartbeat (30s) + Project heartbeat (20s)

- **Allatori代码混淆** - 使用Allatori v9.8进行字符串加密、控制流混淆和名称混淆
  **Allatori Code Obfuscation** - String encryption, control flow obfuscation, and name obfuscation using Allatori v9.8

---

### 🔧 修复 / Bug Fixes

- **修复大航海事件解析** - 正确从 `data.user_info.uname` 获取用户名，使用 `guard_level` 映射大航海类型（总督/提督/舰长）
  **Fixed Guard Event Parsing** - Correctly retrieves username from `data.user_info.uname`, maps guard types using `guard_level` (Governor/Admiral/Captain)

- **修复签名算法** - 使用HMAC-SHA256签名，版本号1.0
  **Fixed Signature Algorithm** - Uses HMAC-SHA256 signature with version 1.0

- **修复API域名** - 使用 `live-open.biliapi.com` 作为API端点
  **Fixed API Domain** - Uses `live-open.biliapi.com` as the API endpoint

- **兼容新旧CMD名称** - 同时支持 `OPEN_LIVEROOM_*` 和 `LIVE_OPEN_PLATFORM_*`
  **Backward Compatible CMD Names** - Supports both `OPEN_LIVEROOM_*` and `LIVE_OPEN_PLATFORM_*`

---

### 📦 安装 / Installation

1. 下载 `BLChat-1.0.3(1.20.1-1.20.6).jar`
   Download `BLChat-1.0.3(1.20.1-1.20.6).jar`

2. 放入 `.minecraft/mods/` 目录
   Place it in the `.minecraft/mods/` directory

3. 启动游戏，在Mod配置中输入B站直播间身份码
   Launch the game and enter your Bilibili live room Identity Code in the Mod configuration

---

### ⚙️ 配置 / Configuration

- **身份码 / Identity Code** - 在B站直播开放平台获取的主播身份码
  The streamer's Identity Code obtained from the Bilibili Live Open Platform
- Forge版本要求 / Forge Version: 47.x - 50.x
- Minecraft版本 / Minecraft Version: 1.20.1

---

### 🔗 相关链接 / Links

- [B站直播开放平台 / Bilibili Live Open Platform](https://open-live.bilibili.com/)
- [项目仓库 / Project Repository](https://github.com/JeffreyMing2004/BLChat)
