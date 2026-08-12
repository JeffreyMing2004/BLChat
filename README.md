# BLChat

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20~1.21.11-green)
![Java](https://img.shields.io/badge/Java-17_/_21-orange)
![Node.js](https://img.shields.io/badge/Node.js-18+-brightgreen)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-blue)
![License](https://img.shields.io/badge/License-All_Rights_Reserved-yellow)

在 Minecraft 游戏内实时查看 B 站直播弹幕 · 提供 Web 管理面板与 OBS 弹幕覆盖层

View Bilibili live danmaku in Minecraft · Web management panel & OBS danmaku overlay

</div>

---

## 项目简介 / Overview

BLChat 是一个完整的 B 站直播弹幕 Minecraft 集成方案，包含三个核心组件：

BLChat is a complete Bilibili live danmaku integration for Minecraft, consisting of three core components:

| 组件 / Component | 说明 / Description |
|------|------|
| **MC Forge Mod** | Minecraft 模组，将 B 站弹幕实时显示在游戏聊天栏 / Displays Bilibili danmaku in Minecraft chat |
| **H5 管理面板** | Web 端管理面板，主播身份验证与 OBS 弹幕地址获取 / Web panel for streamer authentication & OBS overlay URL |
| **OBS 弹幕覆盖层** | 透明弹幕页面，适配 OBS 浏览器源 / Transparent danmaku page for OBS browser source |

## 功能特性 / Features

### MC 模组 / MC Mod

- 游戏聊天栏实时显示弹幕消息 / Real-time danmaku display in game chat
- 礼物赠送、Super Chat、大航海开通事件同步 / Gift, Super Chat, and Guard notifications
- 游戏内 Mod 配置界面 / In-game configuration screen
- 管理员指令 `/bilibili identitycode <code>` 快速切换身份码 / Admin command to switch identity code
- 断线自动重连 / Auto-reconnect on disconnect

### H5 管理面板 / H5 Panel

- 主播身份码验证，获取主播昵称、头像、房间号 / Streamer identity code verification
- 识别码机制：对外展示使用随机识别码，不暴露主播身份码 / Random display code to protect streamer identity
- 识别码持久化存储（SQLite）/ Persistent storage with SQLite
- MC 模组版本检测 / MC mod version detection

### OBS 弹幕覆盖层 / OBS Overlay

- 独立透明页面 `/danmu/{识别码}` / Standalone transparent page at `/danmu/{displayCode}`
- 无需登录即可访问 / No login required
- 断线自动重连 / Auto-reconnect
- 文字阴影适配各种直播背景 / Text shadow for various backgrounds

## 快速开始 / Quick Start

### 前置条件 / Prerequisites

- **MC 模组**：Java 17（1.20.x）或 Java 21（1.21.x）、Minecraft 1.20~1.21.11 + Forge 46~61
- **H5 插件**：Node.js 18+（独立仓库 / Separate repo）
- **B 站开放平台**：H5 面板部署需 Access Key、Access Secret、App ID；MC 模组仅需主播身份码（凭据已内置）

MC Mod: Java 17 (1.20.x) or Java 21 (1.21.x), Minecraft 1.20~1.21.11 + Forge 46~61
H5 Plugin: Node.js 18+ (separate repo)
Bilibili Open Platform: H5 panel requires Access Key, Access Secret, App ID; MC mod only needs streamer identity code (credentials built-in)

申请地址 / Apply at: [哔哩哔哩直播开放平台](https://open-live.bilibili.com/)

### 一、部署 H5 管理面板 / Deploy H5 Panel

> H5 管理面板源码位于独立仓库，详见 [h5.mingpixel.net](https://h5.mingpixel.net)。
> The H5 panel source is maintained in a separate repo, see [h5.mingpixel.net](https://h5.mingpixel.net).

#### 1. 配置环境变量 / Configure Environment

```bash
cd h5-plugin/server
cp .env.example .env
```

编辑 `.env` 文件 / Edit `.env`:

```env
BILIBILI_ACCESS_KEY=你的AccessKey
BILIBILI_ACCESS_SECRET=你的AccessSecret
BILIBILI_APP_ID=你的AppID
JWT_SECRET=自定义一个随机字符串
PORT=3000
CLIENT_ORIGIN=https://your-domain.com
```

#### 2. 安装依赖 / Install Dependencies

```bash
# 后端 / Backend
cd h5-plugin/server
npm install

# 前端 / Frontend
cd ../client
npm install
```

#### 3. 本地开发 / Local Development

Windows 用户可直接双击 `h5-plugin/start.bat` 一键启动。

Windows users can double-click `h5-plugin/start.bat` to start.

手动启动 / Manual start:

```bash
# 终端 1 / Terminal 1: 后端 / Backend
cd h5-plugin/server
npm run dev

# 终端 2 / Terminal 2: 前端 / Frontend
cd h5-plugin/client
npm run dev
```

#### 4. 生产部署 / Production

```bash
# 构建前端 / Build frontend
cd h5-plugin/client
npm run build

# 启动后端 / Start backend
cd ../server
npm start
```

### 二、安装 MC 模组 / Install MC Mod

1. 安装 Minecraft Forge / Install Minecraft Forge
2. 从 [GitHub Releases](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/releases) 下载与你 MC 版本对应的 `.jar` / Download the `.jar` matching your MC version
3. 将 `.jar` 放入 `mods/` 目录 / Put `.jar` into `mods/` directory
4. 启动游戏 / Launch game

**版本对照 / Version mapping:**

| MC 版本 / Version | 下载文件 / Download |
|-------------------|---------------------|
| 1.20 ~ 1.20.1 | `bilibilichatmcforge-1.20-1.20.1-*.jar` |
| 1.20.2 ~ 1.20.4 | `bilibilichatmcforge-1.20.2-1.20.4-*.jar` |
| 1.20.6 | `bilibilichatmcforge-1.20.6-*.jar` |
| 1.21 ~ 1.21.1 | `bilibilichatmcforge-1.21-1.21.1-*.jar` |
| 1.21.2 ~ 1.21.5 | `bilibilichatmcforge-1.21.2-1.21.5-*.jar` |
| 1.21.6 ~ 1.21.10 | `bilibilichatmcforge-1.21.6-1.21.10-*.jar` |
| 1.21.11 | `bilibilichatmcforge-1.21.11-*.jar` |

> **注意**：MC 1.20.5 无 Forge 版本，使用 1.20.6 jar 即可。
> **Note**: MC 1.20.5 has no Forge build, use the 1.20.6 jar instead.

### 三、配置 MC 模组 / Configure MC Mod

**游戏内配置 / In-game:**
Mods 列表 → BLChat → 配置 / Mods list → BLChat → Config

**配置文件 / Config file:**
`config/bilibilichat-config.json`

```json
{
  "identityCode": "你的身份码"
}
```

**管理员指令 / Admin command:**
```
/bilibili identitycode <身份码>
```

## 使用流程 / Usage Flow

```
主播在 B 站开播 → 获取身份码 → 在 H5 面板验证 → 获取 OBS 弹幕地址
Stream starts → Get identity code → Verify in H5 panel → Get OBS URL
                                         ↓
                              MC 模组自动连接弹幕 → 游戏内显示弹幕
                              MC Mod connects → Display danmaku in game
```

1. 主播在 B 站开播，从[开播页面](https://link.bilibili.com/p/center/index#/my-room/start-live)获取身份码
2. 在 H5 管理面板输入身份码完成验证
3. 验证成功后获得 OBS 弹幕地址：`https://your-domain.com/danmu/{识别码}`
4. 在 MC 模组配置中填入身份码，游戏内即可显示弹幕

1. Streamer gets identity code from [live setup page](https://link.bilibili.com/p/center/index#/my-room/start-live)
2. Enter identity code in H5 panel to verify
3. Get OBS danmaku URL: `https://your-domain.com/danmu/{displayCode}`
4. Enter identity code in MC mod config, danmaku appears in game

## API 接口 / API Endpoints

| 接口 / Endpoint | 方法 / Method | 说明 / Description |
|------|------|------|
| `/api/login` | POST | 身份码验证 / Identity code verification |
| `/api/me` | GET | 查询登录状态 / Check login status |
| `/api/room-info` | GET | 获取主播信息 / Get streamer info |
| `/api/avatar` | GET | 头像代理 / Avatar proxy |
| `/api/mod/latest` | GET | 获取最新版本 / Get latest version |
| `/api/health` | GET | 健康检查 / Health check |
| `/ws/danmu/{code}` | WS | 公开弹幕 / Public danmaku |
| `/danmu/ws?token=` | WS | 管理弹幕 / Admin danmaku |

## 技术栈 / Tech Stack

| 层 / Layer | 技术 / Technology |
|----|------|
| MC 模组 | Java 17 / 21 · Minecraft Forge 46~61 · 多 jar 架构 / Multi-jar architecture |
| 前端 / Frontend | Vue 3 · Vite 5 |
| 后端 / Backend | Node.js · Express · WebSocket |
| 存储 / Storage | SQLite（sql.js） |
| 认证 / Auth | JWT |
| 数据源 / Data | 哔哩哔哩直播开放平台 API v2 |

## 项目结构 / Project Structure

```
BLChat/
├── 1.20.x/                       # MC 1.20~1.20.6 多版本 / Multi-version for 1.20~1.20.6
│   ├── shared/                   # 共享源码 / Shared source code
│   ├── forge-1.20/               # Jar: 1.20~1.20.1 (Forge 46~47, Java 17)
│   ├── forge-1.20.2/             # Jar: 1.20.2~1.20.4 (Forge 48~49, Java 17)
│   ├── forge-1.20.6/             # Jar: 1.20.6 (Forge 50, Java 21)
│   └── build-all.bat             # 一键构建 / One-click build
├── 1.21.x/                       # MC 1.21~1.21.11 多版本 / Multi-version for 1.21~1.21.11
│   ├── shared/                   # 共享源码 / Shared source code
│   ├── forge-1.21/               # Jar: 1.21~1.21.1 (Forge 51~52, Java 21)
│   ├── forge-1.21.2/             # Jar: 1.21.2~1.21.5 (Forge 53~55, Java 21)
│   ├── forge-1.21.6/             # Jar: 1.21.6~1.21.10 (Forge 56~60, Java 21)
│   ├── forge-1.21.11/            # Jar: 1.21.11 (Forge 61, Java 21)
│   └── build-all.bat             # 一键构建 / One-click build
├── build.gradle                  # Forge 构建配置 (1.20.1 legacy) / Forge build config (1.20.1 legacy)
├── gradle.properties             # 模组版本与元数据 / Mod version & metadata
└── README.md
```

> H5 管理面板与 OBS 弹幕覆盖层源码位于独立仓库。
> H5 panel & OBS overlay sources are maintained in a separate repo.

## 注意事项 / Notes

- `BILIBILI_ACCESS_KEY` 等凭据属于敏感信息，**不要**提交到仓库或公开分享
- 识别码为 8 位随机字符串，同一身份码始终映射到同一识别码
- OBS 弹幕页面断线后会自动重连（3 秒间隔）

- `BILIBILI_ACCESS_KEY` and other credentials are sensitive. **Do NOT** commit to repository or share publicly
- Display codes are 8-character random strings, consistently mapped to identity codes
- OBS danmaku page auto-reconnects on disconnect (3s interval)

## License

当前工程为 All Rights Reserved / All Rights Reserved

## 相关链接 / Links

- [GitHub](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge)
- [Modrinth](https://modrinth.com/mod/blchat)
- [问题反馈 / Issues](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/issues)
- [H5 弹幕工具 / H5 Danmaku Tool](https://h5.mingpixel.net)
