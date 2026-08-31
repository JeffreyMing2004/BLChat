# BLChat

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20~26.2-green)
![Forge](https://img.shields.io/badge/Forge-46~65-orange)
![Java](https://img.shields.io/badge/Java-17_/_21_/_25-red)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-fb7299)
![Modrinth](https://img.shields.io/badge/Modrinth-BLChat-00af54)
![License](https://img.shields.io/badge/License-LGPL--2.1-blue)

**在 Minecraft 游戏内实时查看 B 站直播弹幕**
**View Bilibili live danmaku in Minecraft**

配套 Web 管理面板与 OBS 弹幕覆盖层 / With a web panel & OBS danmaku overlay

[下载 Releases](https://github.com/JeffreyMing2004/BLChat/releases) · [Modrinth](https://modrinth.com/mod/blchat) · [问题反馈 Issues](https://github.com/JeffreyMing2004/BLChat/issues)

</div>

---

## 项目简介 / Overview

BLChat 将 B 站直播间的弹幕、礼物、Super Chat、大航海等事件实时显示在 Minecraft 游戏聊天栏中。采用多 jar 架构，覆盖 Minecraft 1.20 ~ 26.2 全版本。

BLChat displays real-time danmaku, gifts, Super Chat, and guard events from a Bilibili live room in the Minecraft chat. A multi-jar architecture covers Minecraft 1.20 ~ 26.2.

| 组件 / Component | 说明 / Description |
|------|------|
| **MC Forge Mod**（本仓库 / this repo） | 将 B 站弹幕实时显示在游戏聊天栏 / Displays Bilibili danmaku in game chat |
| **H5 管理面板 / Web Panel** | 主播身份码验证、OBS 弹幕地址获取（独立部署 / deployed separately） |
| **OBS 弹幕覆盖层 / Overlay** | 透明弹幕页面，适配 OBS 浏览器源 / Transparent danmaku page for OBS browser source |

> 主播只需一个身份码：B 站开放平台的凭据已内置在模组中，无需自行申请。
> Streamers only need an identity code — the Bilibili Open Platform credentials are built into the mod.

## 功能特性 / Features

- 🎮 **游戏内弹幕显示** — 弹幕、礼物、SC、大航海事件实时同步到聊天栏 / Real-time danmaku, gifts, SC & guard events in game chat
- ⚙️ **游戏内配置界面** — Mods 列表中直接配置身份码 / In-game config screen
- 🔧 **管理员指令** — `/bilibili identitycode <身份码>` 快速切换身份码 / Admin command to switch identity code
- 🔄 **断线自动重连** — WebSocket 断开后自动重连 / Auto-reconnect on disconnect
- 💓 **双重心跳保活** — WebSocket 心跳 + 应用心跳双通道维持连接 / Dual heartbeat keep-alive
- 🛡️ **安全防护** — 未知数据包超限直接丢弃，防止恶意包撑爆内存 / Oversized untrusted packets are dropped
- 🔎 **版本检测** — 启动时向版本服务器校验，过低时提示更新并附更新日志 / Startup version check with update notice
- 🌐 **中英双语** — 语言文件随模组内置 / Built-in zh_cn & en_us language files

## 下载安装 / Download & Install

1. 安装对应版本的 Minecraft Forge
2. 从 [Releases](https://github.com/JeffreyMing2004/BLChat/releases) 或 [Modrinth](https://modrinth.com/mod/blchat) 下载与你 MC 版本对应的 `.jar`
3. 将 `.jar` 放入 `mods/` 目录，启动游戏

1. Install Minecraft Forge for your MC version
2. Download the `.jar` matching your MC version from [Releases](https://github.com/JeffreyMing2004/BLChat/releases) or [Modrinth](https://modrinth.com/mod/blchat)
3. Put the `.jar` into your `mods/` folder and launch

### 版本对照 / Version Mapping

| MC 版本 / Version | Forge | Java | 下载文件 / Download |
|:---:|:---:|:---:|---|
| 1.20 ~ 1.20.1 | 46 ~ 47 | 17 | `BLChat-1.20-1.20.1-*.jar` |
| 1.20.2 ~ 1.20.4 | 48 ~ 49 | 17 | `BLChat-1.20.2-1.20.4-*.jar` |
| 1.20.6 | 50 | 21 | `BLChat-1.20.6-*.jar` |
| 1.21 ~ 1.21.1 | 51 ~ 52 | 21 | `BLChat-1.21-1.21.1-*.jar` |
| 1.21.2 ~ 1.21.5 | 53 ~ 55 | 21 | `BLChat-1.21.2-1.21.5-*.jar` |
| 1.21.6 ~ 1.21.10 | 56 ~ 60 | 21 | `BLChat-1.21.6-1.21.10-*.jar` |
| 1.21.11 | 61 | 21 | `BLChat-1.21.11-*.jar` |
| 26.1 | 62 | 25 | `BLChat-26.1-*.jar` |
| 26.1.1 | 63 | 25 | `BLChat-26.1.1-*.jar` |
| 26.1.2 | 64 | 25 | `BLChat-26.1.2-*.jar` |
| 26.2 | 65 | 25 | `BLChat-26.2-*.jar` |

> **注意**：MC 1.20.5 无对应 Forge 构建，使用 `BLChat-1.20.6-*.jar` 即可。
> **Note**: MC 1.20.5 has no matching Forge build — use the `BLChat-1.20.6-*.jar` instead.

## 配置 / Configuration

**游戏内界面 / In-game screen**：Mods 列表 → BLChat → Config

**配置文件 / Config file**：`config/bilibilichat-config.json`

```json
{
  "identityCode": "你的身份码 / your identity code"
}
```

**管理员指令 / Admin command**（需 OP 权限 / requires OP）：

```
/bilibili identitycode <身份码>
```

## 使用流程 / Usage Flow

```
主播在 B 站开播 → 从开播页面获取身份码 → 模组配置填入身份码
Stream starts → Get identity code → Fill it in the mod config
                              ↓
                    游戏聊天栏实时显示弹幕 / Live danmaku in game chat
                              ↓
        （可选 / optional）在 H5 面板验证身份码 → 获取 OBS 弹幕覆盖层地址
        Verify identity code in the H5 panel → Get the OBS overlay URL
```

1. 主播在 [B 站开播页面](https://link.bilibili.com/p/center/index#/my-room/start-live)获取身份码
2. 在模组配置（游戏内界面 / 配置文件 / 指令）中填入身份码，弹幕即实时显示
3. 如需 OBS 弹幕覆盖层，在 [H5 管理面板](https://h5.mingpixel.net)验证身份码后获取 `https://your-domain.com/danmu/{识别码}` 页面地址，添加为 OBS 浏览器源

## H5 管理面板与 OBS 覆盖层 / Web Panel & OBS Overlay

H5 管理面板（身份码验证、主播信息、OBS 覆盖层地址分发、模组版本检测接口）与 OBS 弹幕覆盖层独立于本仓库部署与维护，详见 <https://h5.mingpixel.net>。

The H5 web panel (identity code verification, streamer info, OBS overlay URL dispatch, mod version-check API) and the OBS danmaku overlay are maintained and deployed separately — see <https://h5.mingpixel.net>.

本模组启动时访问版本检测服务（`version.mingpixel.net`）比对最新版本，版本过低时在游戏内提示更新。

The mod checks the latest version against `version.mingpixel.net` at startup and notifies players in game when an update is available.

## 从源码构建 / Build from Source

**Windows 一键构建（推荐）/ One-click build (recommended)**：

```bat
build-all.bat
```

自动编译全部 11 个版本 jar 并收集到根目录 `all\` 文件夹。需要本机已安装 Java 17 / 21 / 25（`tools/build-all-versions.ps1` 中配置的路径）。

Builds all 11 jars and collects them into `all\`. Requires local JDK 17 / 21 / 25.

**单版本构建 / Single line build**：

```bash
cd 1.21.x/forge-1.21
./gradlew build
# 产物 / output: build/libs/*.jar
```

推送代码后，GitHub Actions（[build.yml](.github/workflows/build.yml)）会自动以矩阵方式构建全部版本并在 Artifacts 中上传。

On push, GitHub Actions builds all version lines automatically and uploads the jars as artifacts.

## 项目结构 / Project Structure

```
BLChat/
├── 1.20.x/                       # MC 1.20~1.20.6 / For MC 1.20~1.20.6 (Java 17/21)
│   ├── shared/                   # 共享源码（弹幕客户端、版本检测等）/ Shared sources
│   ├── forge-1.20/               # Jar: 1.20~1.20.1 (Forge 47)
│   ├── forge-1.20.2/             # Jar: 1.20.2~1.20.4 (Forge 49)
│   └── forge-1.20.6/             # Jar: 1.20.6 (Forge 50)
├── 1.21.x/                       # MC 1.21~1.21.11 / For MC 1.21~1.21.11 (Java 21)
│   ├── shared/
│   ├── forge-1.21/               # Jar: 1.21~1.21.1 (Forge 52)
│   ├── forge-1.21.2/             # Jar: 1.21.2~1.21.5 (Forge 55)
│   ├── forge-1.21.6/             # Jar: 1.21.6~1.21.10 (Forge 60)
│   └── forge-1.21.11/            # Jar: 1.21.11 (Forge 61)
├── 26.1.x/                       # MC 26.1~26.1.2 / For MC 26.1~26.1.2 (Java 25)
│   ├── shared/
│   ├── forge-26.1/               # Jar: 26.1 (Forge 62)
│   ├── forge-26.1.1/             # Jar: 26.1.1 (Forge 63)
│   └── forge-26.1.2/             # Jar: 26.1.2 (Forge 64)
├── 26.2.x/                       # MC 26.2 / For MC 26.2 (Java 25)
│   ├── shared/
│   └── forge-26.2/               # Jar: 26.2 (Forge 65)
├── tools/                        # 构建与凭据工具脚本 / Build & credential tooling
├── build-all.bat                 # 一键构建全部版本 / One-click build for all lines
└── version.properties            # 全局版本号（构建时注入）/ Global version (injected at build)
```

每个版本线内含独立的 `build.gradle` 与 Gradle Wrapper；模组版本号统一来自根目录 `version.properties`，构建时自动生成 `blchat-version.properties` 与 `mods.toml`。

Each version line has its own `build.gradle` and Gradle wrapper. The mod version comes from the root `version.properties` and is injected into `blchat-version.properties` and `mods.toml` at build time.

## 技术栈 / Tech Stack

| 层 / Layer | 技术 / Technology |
|----|------|
| MC 模组 | Java 17 / 21 / 25 · Minecraft Forge 46~65 · 多 jar 架构 / Multi-jar architecture |
| 弹幕接入 / Danmaku | 哔哩哔哩直播开放平台 API v2（WebSocket + 心跳） |
| 配置存储 / Config | JSON（`config/bilibilichat-config.json`） |
| 版本检测 / Version check | `version.mingpixel.net` |
| H5 面板（独立部署） | Node.js · Express · WebSocket · Vue 3 · SQLite · JWT |

## 注意事项 / Notes

- 身份码属于账号敏感信息，**不要**分享给他人或提交到仓库
- B 站开放平台凭据已混淆内置，主播无需申请；密钥轮换使用 `tools/encode-credentials.ps1`
- 模组面向单机/客户端场景（`clientSideOnly`），弹幕显示在本地游戏聊天栏

- Identity codes are account-sensitive. **Do NOT** share them or commit them to the repository
- Bilibili Open Platform credentials are obfuscated and built in — streamers don't need to apply; rotate keys with `tools/encode-credentials.ps1`
- The mod is client-side (`clientSideOnly`); danmaku is rendered in the local game chat

## 支持项目 / Support

<div align="center">

如果 BLChat 帮到了你的直播，欢迎请作者喝杯咖啡 ☕
If BLChat helps your stream, buying the author a coffee is appreciated ☕

<!-- 在此添加捐赠链接，例如：[爱发电](https://afdian.net/a/你的ID) · [Ko-fi](https://ko-fi.com/你的ID) -->

捐赠完全自愿，不影响任何功能使用 / Donations are purely voluntary and unlock nothing

</div>

## License

[BLChat](https://github.com/JeffreyMing2004/BLChat) - B站直播弹幕 Minecraft 集成方案
Copyright (C) 2026 JeffreyMing

本项目基于 [LGPL-2.1](LICENSE) 许可证发布。
Released under the [GNU Lesser General Public License v2.1](LICENSE).

## 相关链接 / Links

- [GitHub](https://github.com/JeffreyMing2004/BLChat)
- [Modrinth](https://modrinth.com/mod/blchat)
- [问题反馈 / Issues](https://github.com/JeffreyMing2004/BLChat/issues)
- [H5 弹幕工具 / H5 Danmaku Tool](https://h5.mingpixel.net)
- [B 站直播开放平台 / Bilibili Live Open Platform](https://open-live.bilibili.com/)
