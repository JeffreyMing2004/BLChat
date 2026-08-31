# BLChat

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20~26.2-green)
![Forge](https://img.shields.io/badge/Forge-46~65-orange)
![Java](https://img.shields.io/badge/Java-17_/_21_/_25-red)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-fb7299)
![Modrinth](https://img.shields.io/badge/Modrinth-BLChat-00af54)
![License](https://img.shields.io/badge/License-LGPL--2.1-blue)

**在 Minecraft 游戏内实时查看 B 站直播弹幕**

配套 Web 管理面板与 OBS 弹幕覆盖层

[下载 Releases](https://github.com/JeffreyMing2004/BLChat/releases) · [Modrinth](https://modrinth.com/mod/blchat) · [问题反馈 Issues](https://github.com/JeffreyMing2004/BLChat/issues)

[English](README.md) | **简体中文**

</div>

---

## 项目简介

BLChat 将 B 站直播间的弹幕、礼物、Super Chat、大航海等事件实时显示在 Minecraft 游戏聊天栏中。采用多 jar 架构，覆盖 Minecraft 1.20 ~ 26.2 全版本。

| 组件 | 说明 |
|------|------|
| **MC Forge Mod**（本仓库） | 将 B 站弹幕实时显示在游戏聊天栏 |
| **H5 管理面板** | 主播身份码验证、主播信息、OBS 弹幕地址获取（独立部署） |
| **OBS 弹幕覆盖层** | 透明弹幕页面，适配 OBS 浏览器源 |

> 主播只需一个身份码：B 站开放平台的凭据已内置在模组中，无需自行申请。

## 功能特性

- 🎮 **游戏内弹幕显示** — 弹幕、礼物、SC、大航海事件实时同步到聊天栏
- ⚙️ **游戏内配置界面** — Mods 列表中直接配置身份码
- 🔧 **管理员指令** — `/bilibili identitycode <身份码>` 快速切换身份码
- 🔄 **断线自动重连** — WebSocket 断开后自动重连
- 💓 **双重心跳保活** — WebSocket 心跳 + 应用心跳双通道维持连接
- 🛡️ **安全防护** — 未知数据包超限直接丢弃，防止恶意包撑爆内存
- 🔎 **版本检测** — 启动时向版本服务器校验，过低时提示更新并附更新日志
- 🌐 **中英双语** — 语言文件随模组内置

## 下载安装

1. 安装对应版本的 Minecraft Forge
2. 从 [Releases](https://github.com/JeffreyMing2004/BLChat/releases) 或 [Modrinth](https://modrinth.com/mod/blchat) 下载与你 MC 版本对应的 `.jar`
3. 将 `.jar` 放入 `mods/` 目录，启动游戏

### 版本对照

| MC 版本 | Forge | Java | 下载文件 |
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

## 配置

**游戏内界面**：Mods 列表 → BLChat → Config

**配置文件**：`config/bilibilichat-config.json`

```json
{
  "identityCode": "你的身份码"
}
```

**管理员指令**（需 OP 权限）：

```
/bilibili identitycode <身份码>
```

## 使用流程

1. 主播在 [B 站开播页面](https://link.bilibili.com/p/center/index#/my-room/start-live)获取身份码
2. 在模组配置（游戏内界面 / 配置文件 / 指令）中填入身份码，弹幕即实时显示
3. 如需 OBS 弹幕覆盖层，在 [H5 管理面板](https://h5.mingpixel.net)验证身份码后获取 `https://your-domain.com/danmu/{识别码}` 页面地址，添加为 OBS 浏览器源

## H5 管理面板与 OBS 覆盖层

H5 管理面板（身份码验证、主播信息、OBS 覆盖层地址分发、模组版本检测接口）与 OBS 弹幕覆盖层独立于本仓库部署与维护，详见 <https://h5.mingpixel.net>。

本模组启动时访问版本检测服务（`version.mingpixel.net`）比对最新版本，版本过低时在游戏内提示更新。

## 从源码构建

**Windows 一键构建（推荐）**：

```bat
build-all.bat
```

自动编译全部 11 个版本 jar 并收集到根目录 `all\` 文件夹。需要本机已安装 Java 17 / 21 / 25（`tools/build-all-versions.ps1` 中配置的路径）。

**单版本构建**：

```bash
cd 1.21.x/forge-1.21
./gradlew build
# 产物：build/libs/*.jar
```

推送代码后，GitHub Actions（[build.yml](.github/workflows/build.yml)）会自动以矩阵方式构建全部版本并在 Artifacts 中上传。

## 项目结构

```
BLChat/
├── 1.20.x/                       # MC 1.20~1.20.6 (Java 17/21)
│   ├── shared/                   # 共享源码（弹幕客户端、版本检测等）
│   ├── forge-1.20/               # Jar: 1.20~1.20.1 (Forge 47)
│   ├── forge-1.20.2/             # Jar: 1.20.2~1.20.4 (Forge 49)
│   └── forge-1.20.6/             # Jar: 1.20.6 (Forge 50)
├── 1.21.x/                       # MC 1.21~1.21.11 (Java 21)
│   ├── shared/
│   ├── forge-1.21/               # Jar: 1.21~1.21.1 (Forge 52)
│   ├── forge-1.21.2/             # Jar: 1.21.2~1.21.5 (Forge 55)
│   ├── forge-1.21.6/             # Jar: 1.21.6~1.21.10 (Forge 60)
│   └── forge-1.21.11/            # Jar: 1.21.11 (Forge 61)
├── 26.1.x/                       # MC 26.1~26.1.2 (Java 25)
│   ├── shared/
│   ├── forge-26.1/               # Jar: 26.1 (Forge 62)
│   ├── forge-26.1.1/             # Jar: 26.1.1 (Forge 63)
│   └── forge-26.1.2/             # Jar: 26.1.2 (Forge 64)
├── 26.2.x/                       # MC 26.2 (Java 25)
│   ├── shared/
│   └── forge-26.2/               # Jar: 26.2 (Forge 65)
├── tools/                        # 构建与凭据工具脚本
├── build-all.bat                 # 一键构建全部版本
└── version.properties            # 全局版本号（构建时注入）
```

每个版本线内含独立的 `build.gradle` 与 Gradle Wrapper；模组版本号统一来自根目录 `version.properties`，构建时自动生成 `blchat-version.properties` 与 `mods.toml`。

## 技术栈

| 层 | 技术 |
|----|------|
| MC 模组 | Java 17 / 21 / 25 · Minecraft Forge 46~65 · 多 jar 架构 |
| 弹幕接入 | 哔哩哔哩直播开放平台 API v2（WebSocket + 心跳） |
| 配置存储 | JSON（`config/bilibilichat-config.json`） |
| 版本检测 | `version.mingpixel.net` |
| H5 面板（独立部署） | Node.js · Express · WebSocket · Vue 3 · SQLite · JWT |

## 注意事项

- 身份码属于账号敏感信息，**不要**分享给他人或提交到仓库
- B 站开放平台凭据已混淆内置，主播无需申请；密钥轮换使用 `tools/encode-credentials.ps1`
- 模组面向单机/客户端场景（`clientSideOnly`），弹幕显示在本地游戏聊天栏

## 支持项目

<div align="center">

如果 BLChat 帮到了你的直播，欢迎请作者喝杯咖啡 ☕

**[爱发电](https://ifdian.net/a/JeffreyMing)**

捐赠完全自愿。所有正式版始终免费、开源，功能不受任何影响；赞助者可优先体验新版本的测试版（Beta），测试版稳定后即发布为正式版。

</div>

## 许可证

[BLChat](https://github.com/JeffreyMing2004/BLChat) - B站直播弹幕 Minecraft 集成方案
Copyright (C) 2026 JeffreyMing

本项目基于 [LGPL-2.1](LICENSE) 许可证发布。

## 相关链接

- [GitHub](https://github.com/JeffreyMing2004/BLChat)
- [Modrinth](https://modrinth.com/mod/blchat)
- [问题反馈 / Issues](https://github.com/JeffreyMing2004/BLChat/issues)
- [H5 弹幕工具 / H5 Danmaku Tool](https://h5.mingpixel.net)
- [B 站直播开放平台 / Bilibili Live Open Platform](https://open-live.bilibili.com/)
