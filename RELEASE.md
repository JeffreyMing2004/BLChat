# BLChat — 全版本发布声明 / All-Version Release Notes

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20~26.1-green)
![Java](https://img.shields.io/badge/Java-17_/_21_/_25-orange)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-blue)
![License](https://img.shields.io/badge/License-LGPL--2.1-blue)

**在 Minecraft 游戏内实时查看 B 站直播弹幕**
**View Bilibili live danmaku in Minecraft**

支持 Minecraft 1.20 ~ 26.1.2 全版本 / Supports Minecraft 1.20 ~ 26.1.2 (all versions)

</div>

---

## 📖 项目简介 / Overview

BLChat 是一个 B 站直播弹幕 Minecraft 集成模组，将 B 站直播间的弹幕、礼物、Super Chat、大航海等事件实时显示在游戏聊天栏中。采用多 jar 架构，覆盖 Minecraft 1.20 ~ 26.1.2 全版本。

BLChat is a Bilibili live danmaku integration mod for Minecraft, displaying real-time danmaku, gifts, Super Chat, and guard events in the game chat. Uses a multi-jar architecture covering Minecraft 1.20 ~ 26.1.2.

### 核心功能 / Core Features

- 🎮 **游戏内弹幕显示** - 弹幕、礼物、SC、大航海实时同步到聊天栏 / Real-time danmaku in game chat
- ⚙️ **游戏内配置界面** - Mods 列表中直接配置身份码 / In-game config screen
- 🔧 **管理员指令** - `/bilibili identitycode <code>` 快速切换身份码 / Admin command
- 🔄 **断线自动重连** - WebSocket 断开后自动重连 / Auto-reconnect on disconnect
- 💓 **双重心跳保活** - WebSocket 心跳 30s + 项目心跳 20s / Dual heartbeat keep-alive

---

## 📦 下载指南 / Download Guide

### 根据 Minecraft 版本选择 jar / Choose jar by MC version

| MC 版本 / Version | Forge 版本 / Forge | Java | 下载文件 / Download File |
|:---:|:---:|:---:|---|
| 1.20 ~ 1.20.1 | 46 ~ 47 | 17 | `bilibilichatmcforge-1.20-1.20.1-1.0.3.jar` |
| 1.20.2 ~ 1.20.4 | 48 ~ 49 | 17 | `bilibilichatmcforge-1.20.2-1.20.4-1.0.3.jar` |
| 1.20.6 | 50 | 21 | `bilibilichatmcforge-1.20.6-1.0.3.jar` |
| 1.21 ~ 1.21.1 | 51 ~ 52 | 21 | `bilibilichatmcforge-1.21-1.21.1-1.0.0.jar` |
| 1.21.2 ~ 1.21.5 | 53 ~ 55 | 21 | `bilibilichatmcforge-1.21.2-1.21.5-1.0.0.jar` |
| 1.21.6 ~ 1.21.10 | 56 ~ 60 | 21 | `bilibilichatmcforge-1.21.6-1.21.10-1.0.0.jar` |
| 1.21.11 | 61 | 21 | `bilibilichatmcforge-1.21.11-1.0.0.jar` |
| 26.1 | 62 | 25 | `bilibilichatmcforge-26.1-1.0.0.jar` |
| 26.1.1 | 63 | 25 | `bilibilichatmcforge-26.1.1-1.0.0.jar` |
| 26.1.2 | 64 | 25 | `bilibilichatmcforge-26.1.2-1.0.0.jar` |

> ⚠️ **注意 / Note**：
> - MC 1.20.5 无 Forge 版本，使用 1.20.6 jar 即可 / MC 1.20.5 has no Forge build, use 1.20.6 jar
> - 每个 jar 仅兼容对应的 MC 版本范围，请勿混用 / Each jar only supports its target MC version range

---

## 📋 版本历史 / Version History

### v1.0.3 — Minecraft 1.20.x (1.20 ~ 1.20.6)

> B 站直播开放平台正式版适配 / Bilibili Live Open Platform Official API Integration

**新特性 / New Features:**
- 适配 B 站直播开放平台正式版 API，使用 `v2/app/start` 接口连接直播间
- 简化配置流程，用户只需设置身份码（Identity Code）
- 支持直播开放平台消息格式（`OPEN_LIVEROOM_DM`、`OPEN_LIVEROOM_SEND_GIFT` 等）
- 自动重连机制（最多重试 5 次）
- 双重心跳保活（WebSocket 30s + 项目 20s）

**技术栈 / Tech Stack:**
- Forge 46 ~ 50 · Java 17 / 21 · ForgeGradle 6.x
- 3 个独立 jar 覆盖 1.20 ~ 1.20.6 全版本
- Allatori 代码混淆保护

---

### v1.0.0 — Minecraft 1.21.x (1.21 ~ 1.21.11)

> 多 jar 架构重构，支持 1.21.x 全版本 / Multi-jar restructure for all 1.21.x versions

**新特性 / New Features:**
- 4 个独立 Gradle 项目覆盖 1.21 ~ 1.21.11 全版本
- `shared/` 目录存放版本无关源码
- VersionCompat 工具类：基于反射的权限 API 兼容
- EventBus 7.x 适配（`@SubscribeEvent` 包名变更）

**关键适配 / Key Adaptations:**
- `renderBackground()` 签名差异处理（1.21.2+ 自动调用，禁止手动调用）
- `IllegalStateException: Can only blur once per frame` 修复
- `WorldVersion.getName()` 移除适配（1.21.11）
- 权限系统重构适配（1.21.11）：`hasPermission(int)` → `permissions().hasPermission(Permission)`
- `pack.mcmeta` 格式差异处理

**技术栈 / Tech Stack:**
- Forge 51 ~ 61 · Java 21 · ForgeGradle 6.x · Gradle 8.14.5

---

### v1.0.0 — Minecraft 26.1.x (26.1 ~ 26.1.2)

> 适配全新 MC 26.1.x 主版本 / Adapted to the new MC 26.1.x major release

**新特性 / New Features:**
- 3 个独立 jar 覆盖 26.1、26.1.1、26.1.2 三个版本
- ForgeGradle 7.x 全新构建系统
- VersionCompat 反射工具类适配新权限系统

**关键适配 / Key Adaptations:**
- `GuiGraphics` → `GuiGraphicsExtractor`（类重命名）
- `render()` → `extractRenderState()`（渲染管线重构）
- `drawCenteredString()` → `centeredText()`，`drawString()` → `text()`
- `renderBackground()` → `extractBackground()`（自动调用，禁止手动）
- `pack.mcmeta` 新格式：`max_format`/`min_format` 替代 `pack_format`
- MC 26.1.x 移除混淆映射，Minecraft 不再混淆

**技术栈 / Tech Stack:**
- Forge 62 ~ 64 · Java 25 · ForgeGradle 7.x · Gradle 9.3.0

**构建验证 / Build Verification:**
- ✅ 3 个 jar 全部构建成功
- ✅ 3 个 jar 客户端启动成功（LWJGL OpenGL/STB 加载正常）

---

## 🔧 安装步骤 / Installation

### 1. 安装 Forge / Install Forge

从 [Minecraft Forge 官网](https://files.minecraftforge.net/) 安装与你 MC 版本对应的 Forge。

### 2. 下载模组 / Download Mod

从 [GitHub Releases](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/releases) 下载与你 MC 版本对应的 `.jar` 文件。

### 3. 放入 mods 目录 / Place in mods directory

```
.minecraft/
└── mods/
    └── bilibilichatmcforge-*.jar
```

### 4. 启动游戏并配置 / Launch & Configure

1. 启动 Minecraft（Forge 版本）
2. 进入 Mods 列表 → BLChat → 配置
3. 输入 B 站直播间身份码（Identity Code）
4. 保存后即可在游戏内接收弹幕

---

## ⚙️ 配置说明 / Configuration

### 游戏内配置 / In-game Config

Mods 列表 → BLChat → 配置 / Mods list → BLChat → Config

### 配置文件 / Config File

路径 / Path: `config/bilibilichat-config.json`

```json
{
  "identityCode": "你的身份码"
}
```

### 管理员指令 / Admin Command

```
/bilibili identitycode <身份码>
```

### 身份码获取 / Get Identity Code

从 [B 站开播页面](https://link.bilibili.com/p/center/index#/my-room/start-live) 获取主播身份码。
Get the streamer identity code from the [Bilibili live setup page](https://link.bilibili.com/p/center/index#/my-room/start-live).

---

## 📊 完整版本矩阵 / Full Version Matrix

| Jar 文件 | MC 版本范围 | Forge | EventBus | Java | ForgeGradle |
|---|---|:---:|:---:|:---:|:---:|
| `bilibilichatmcforge-1.20-1.20.1-1.0.3.jar` | 1.20 ~ 1.20.1 | 46~47 | 6 | 17 | 6.x |
| `bilibilichatmcforge-1.20.2-1.20.4-1.0.3.jar` | 1.20.2 ~ 1.20.4 | 48~49 | 6 | 17 | 6.x |
| `bilibilichatmcforge-1.20.6-1.0.3.jar` | 1.20.6 | 50 | 6 | 21 | 6.x |
| `bilibilichatmcforge-1.21-1.21.1-1.0.0.jar` | 1.21 ~ 1.21.1 | 51~52 | 6 | 21 | 6.x |
| `bilibilichatmcforge-1.21.2-1.21.5-1.0.0.jar` | 1.21.2 ~ 1.21.5 | 53~55 | 6 | 21 | 6.x |
| `bilibilichatmcforge-1.21.6-1.21.10-1.0.0.jar` | 1.21.6 ~ 1.21.10 | 56~60 | 7 | 21 | 6.x |
| `bilibilichatmcforge-1.21.11-1.0.0.jar` | 1.21.11 | 61 | 7 | 21 | 6.x |
| `bilibilichatmcforge-26.1-1.0.0.jar` | 26.1 | 62 | 7 | 25 | 7.x |
| `bilibilichatmcforge-26.1.1-1.0.0.jar` | 26.1.1 | 63 | 7 | 25 | 7.x |
| `bilibilichatmcforge-26.1.2-1.0.0.jar` | 26.1.2 | 64 | 7 | 25 | 7.x |

---

## 🐛 已知问题 / Known Issues

- `ModLoadingContext.get()` 弃用警告（不影响功能，可忽略）/ Deprecation warning (non-functional, safe to ignore)
- MC 1.20.5 无 Forge 版本 / MC 1.20.5 has no Forge build

---

## 📁 项目结构 / Project Structure

```
BLChat/
├── 1.20.x/                       # MC 1.20~1.20.6
│   ├── shared/                   # 共享源码
│   ├── forge-1.20/               # Jar: 1.20~1.20.1 (Forge 46~47, Java 17)
│   ├── forge-1.20.2/             # Jar: 1.20.2~1.20.4 (Forge 48~49, Java 17)
│   ├── forge-1.20.6/             # Jar: 1.20.6 (Forge 50, Java 21)
│   └── build-all.bat             # 一键构建
├── 1.21.x/                       # MC 1.21~1.21.11
│   ├── shared/                   # 共享源码
│   ├── forge-1.21/               # Jar: 1.21~1.21.1 (Forge 51~52, Java 21)
│   ├── forge-1.21.2/             # Jar: 1.21.2~1.21.5 (Forge 53~55, Java 21)
│   ├── forge-1.21.6/             # Jar: 1.21.6~1.21.10 (Forge 56~60, Java 21)
│   ├── forge-1.21.11/            # Jar: 1.21.11 (Forge 61, Java 21)
│   └── build-all.bat             # 一键构建
├── 26.1.x/                       # MC 26.1~26.1.2
│   ├── shared/                   # 共享源码
│   ├── forge-26.1/               # Jar: 26.1 (Forge 62, Java 25)
│   ├── forge-26.1.1/             # Jar: 26.1.1 (Forge 63, Java 25)
│   ├── forge-26.1.2/             # Jar: 26.1.2 (Forge 64, Java 25)
│   └── build-all.bat             # 一键构建
├── build.gradle                  # Forge 构建配置 (1.20.1 legacy)
├── gradle.properties             # 模组版本与元数据
└── README.md
```

---

## 🔗 相关链接 / Links

- **GitHub**: [JeffreyMing2004/BilibiliChat-MC-Forge](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge)
- **Modrinth**: [modrinth.com/mod/blchat](https://modrinth.com/mod/blchat)
- **H5 弹幕工具**: [h5.mingpixel.net](https://h5.mingpixel.net)
- **B 站直播开放平台**: [open-live.bilibili.com](https://open-live.bilibili.com/)
- **问题反馈**: [GitHub Issues](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/issues)

---

## 💎 致谢 / Credits

- Minecraft Forge 团队 / Minecraft Forge Team
- 哔哩哔哩直播开放平台 / Bilibili Live Open Platform
- 所有测试与反馈的用户 / All testers and contributors

---

## 📄 License

本项目基于 [LGPL-2.1](LICENSE) 许可证发布。
Released under the [GNU Lesser General Public License v2.1](LICENSE).

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐ Star！**
**If this project helps you, please give it a ⭐ Star!**

</div>
