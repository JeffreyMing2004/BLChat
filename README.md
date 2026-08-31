# BLChat

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20~26.2-green)
![Forge](https://img.shields.io/badge/Forge-46~65-orange)
![Java](https://img.shields.io/badge/Java-17_/_21_/_25-red)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-fb7299)
![Modrinth](https://img.shields.io/badge/Modrinth-BLChat-00af54)
![License](https://img.shields.io/badge/License-LGPL--2.1-blue)

**View Bilibili live danmaku in Minecraft, in real time**

Comes with a web management panel and an OBS danmaku overlay

[Download Releases](https://github.com/JeffreyMing2004/BLChat/releases) · [Modrinth](https://modrinth.com/mod/blchat) · [Report an Issue](https://github.com/JeffreyMing2004/BLChat/issues)

**English** | [简体中文](README.zh-CN.md)

</div>

---

## Overview

BLChat displays danmaku (chat comments), gifts, Super Chats, and guard purchases from a Bilibili live room in the Minecraft chat, in real time. A multi-jar architecture covers Minecraft 1.20 through 26.2.

| Component | Description |
|------|------|
| **MC Forge Mod** (this repo) | Displays Bilibili danmaku in the in-game chat |
| **H5 Web Panel** | Identity-code verification, streamer info, OBS overlay URL dispatch (deployed separately) |
| **OBS Danmaku Overlay** | Transparent danmaku page for OBS browser sources |

> Streamers only need an identity code — the Bilibili Open Platform credentials are built into the mod, so there is nothing to apply for.

## Features

- 🎮 **Real-time danmaku in game** — danmaku, gifts, Super Chats, and guard events appear in the chat as they happen
- ⚙️ **In-game config screen** — set the identity code straight from the Mods list
- 🔧 **Admin command** — `/bilibili identitycode <identity code>` to switch identity codes quickly
- 🔄 **Auto-reconnect** — reconnects automatically when the WebSocket drops
- 💓 **Dual heartbeat keep-alive** — a WebSocket heartbeat plus an application-level heartbeat keep the connection alive
- 🛡️ **Hardened** — oversized untrusted packets are dropped to prevent memory exhaustion
- 🔎 **Version check** — verifies the latest version at startup and shows an update notice with the changelog when outdated
- 🌐 **Bilingual** — built-in zh_cn & en_us language files

## Download & Install

1. Install Minecraft Forge for your MC version
2. Download the `.jar` matching your MC version from [Releases](https://github.com/JeffreyMing2004/BLChat/releases) or [Modrinth](https://modrinth.com/mod/blchat)
3. Drop the `.jar` into your `mods/` folder and launch the game

### Version Mapping

| MC Version | Forge | Java | Download File |
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

> **Note**: MC 1.20.5 has no matching Forge build — use the `BLChat-1.20.6-*.jar` instead.

## Configuration

**In-game screen**: Mods list → BLChat → Config

**Config file**: `config/bilibilichat-config.json`

```json
{
  "identityCode": "your identity code"
}
```

**Admin command** (requires OP):

```
/bilibili identitycode <identity code>
```

## Usage Flow

1. Start your Bilibili live stream and grab the identity code from the [stream-start page](https://link.bilibili.com/p/center/index#/my-room/start-live)
2. Fill the code into the mod config (in-game screen / config file / command) — danmaku starts flowing into the chat instantly
3. Optional: verify the code on the [H5 web panel](https://h5.mingpixel.net) to get your OBS overlay URL (`https://your-domain.com/danmu/{id}`), then add it as an OBS browser source

## Web Panel & OBS Overlay

The H5 web panel (identity-code verification, streamer info, OBS overlay URL dispatch, mod version-check API) and the OBS danmaku overlay are maintained and deployed separately — see <https://h5.mingpixel.net>.

At startup the mod checks the latest version against `version.mingpixel.net` and notifies players in game when an update is available.

## Build from Source

**One-click build (Windows, recommended)**:

```bat
build-all.bat
```

Builds all 11 version jars and collects them into `all\`. Requires local JDK 17 / 21 / 25 (paths configured in `tools/build-all-versions.ps1`).

**Single version build**:

```bash
cd 1.21.x/forge-1.21
./gradlew build
# output: build/libs/*.jar
```

On push, GitHub Actions ([build.yml](.github/workflows/build.yml)) builds all version lines automatically and uploads the jars as artifacts.

## Project Structure

```
BLChat/
├── 1.20.x/                       # MC 1.20~1.20.6 (Java 17/21)
│   ├── shared/                   # Shared sources (danmaku client, version check, ...)
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
├── tools/                        # Build & credential tooling
├── build-all.bat                 # One-click build for all version lines
└── version.properties            # Global version (injected at build)
```

Each version line has its own `build.gradle` and Gradle wrapper. The mod version comes from the root `version.properties` and is injected into `blchat-version.properties` and `mods.toml` at build time.

## Tech Stack

| Layer | Technology |
|----|------|
| MC mod | Java 17 / 21 / 25 · Minecraft Forge 46~65 · multi-jar architecture |
| Danmaku access | Bilibili Live Open Platform API v2 (WebSocket + heartbeat) |
| Config storage | JSON (`config/bilibilichat-config.json`) |
| Version check | `version.mingpixel.net` |
| H5 panel (deployed separately) | Node.js · Express · WebSocket · Vue 3 · SQLite · JWT |

## Notes

- Identity codes are account-sensitive. **Do NOT** share them with others or commit them to the repository
- Bilibili Open Platform credentials are obfuscated and built in — streamers do not need to apply; rotate keys with `tools/encode-credentials.ps1`
- The mod is client-side only (`clientSideOnly`); danmaku is rendered in the local game chat

## Support

<div align="center">

If BLChat helps your stream, buying the author a coffee is appreciated ☕

**[Afdian (爱发电)](https://ifdian.net/a/JeffreyMing)**

Donations are purely voluntary. All official releases stay free and open-source with no feature limits — sponsors simply get early access to beta builds before they graduate into stable releases.

</div>

## License

[BLChat](https://github.com/JeffreyMing2004/BLChat) — Bilibili live danmaku integration for Minecraft
Copyright (C) 2026 JeffreyMing

Released under the [GNU Lesser General Public License v2.1](LICENSE).

## Links

- [GitHub](https://github.com/JeffreyMing2004/BLChat)
- [Modrinth](https://modrinth.com/mod/blchat)
- [Issues](https://github.com/JeffreyMing2004/BLChat/issues)
- [H5 Danmaku Tool](https://h5.mingpixel.net)
- [Bilibili Live Open Platform](https://open-live.bilibili.com/)
