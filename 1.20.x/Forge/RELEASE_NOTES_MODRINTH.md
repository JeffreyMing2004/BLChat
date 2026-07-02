## BLChat v1.0.3

### B站直播开放平台正式版适配 / Bilibili Live Open Platform Official API Integration

全面适配B站直播开放平台正式版API，使用Allatori v9.8代码混淆保护。
Fully integrates with the Bilibili Live Open Platform official API, with Allatori v9.8 code obfuscation protection.

---

### 更新内容 / Changelog

**新特性 / New Features:**
- 适配B站直播开放平台正式版API（v2/app/start）
  Bilibili Live Open Platform official API integration (v2/app/start)
- 简化配置 - 只需设置身份码，凭证已内置
  Simplified config - only Identity Code needed, credentials built-in
- 支持直播开放平台消息格式（弹幕/礼物/醒目留言/大航海）
  Live Open Platform message support (Danmaku/Gift/Super Chat/Guard)
- WebSocket断开自动重连（最多5次）
  Auto WebSocket reconnection (up to 5 retries)
- 双重心跳保活机制
  Dual heartbeat keep-alive mechanism

**修复 / Bug Fixes:**
- 修复大航海事件解析（正确获取用户名和大航海类型）
  Fixed guard event parsing (correct username and guard type retrieval)
- 修复签名算法（HMAC-SHA256，版本1.0）
  Fixed signature algorithm (HMAC-SHA256, version 1.0)
- 修复API域名（live-open.biliapi.com）
  Fixed API domain (live-open.biliapi.com)
- 兼容新旧CMD名称格式
  Backward compatible with old and new CMD name formats

---

### 安装说明 / Installation

1. 下载 `BLChat-1.0.3(1.20.1-1.20.6).jar`
   Download `BLChat-1.0.3(1.20.1-1.20.6).jar`
2. 放入 `.minecraft/mods/` 目录
   Place it in the `.minecraft/mods/` directory
3. 启动游戏，在Mod配置中输入B站直播间身份码
   Launch the game and enter your Bilibili live room Identity Code in the Mod config

---

### 环境要求 / Requirements

- Minecraft 1.20.1-1.20.6
- Forge 47.x - 50.x
- Java 17+

---

### 相关链接 / Links

- [B站直播开放平台 / Bilibili Live Open Platform](https://open-live.bilibili.com/)
- [GitHub仓库 / GitHub Repository](https://github.com/JeffreyMing2004/BLChat)
