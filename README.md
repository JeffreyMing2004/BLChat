# BilibiliChat-MC-Forge

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green)
![Java](https://img.shields.io/badge/Java-17+-orange)
![Node.js](https://img.shields.io/badge/Node.js-18+-brightgreen)
![Bilibili](https://img.shields.io/badge/Bilibili-Live-blue)
![License](https://img.shields.io/badge/License-All_Rights_Reserved-yellow)

在 Minecraft 游戏内实时查看 B 站直播弹幕 · 提供 Web 管理面板与 OBS 弹幕覆盖层

</div>

---

## 项目简介

BilibiliChat-MC-Forge 是一个完整的 B 站直播弹幕 Minecraft 集成方案，包含两个核心组件：

| 组件 | 说明 |
|------|------|
| **MC Forge Mod** | Minecraft 模组，将 B 站弹幕实时显示在游戏聊天栏 |
| **H5 管理插件** | Web 端管理面板 + OBS 透明弹幕覆盖层 |

两者通过「哔哩哔哩直播开放平台」API 连接同一个直播间弹幕源。

## 功能特性

### MC 模组

- 游戏聊天栏实时显示弹幕消息
- 礼物赠送、Super Chat、大航海开通事件同步
- 游戏内 Mod 配置界面，填写开放平台参数
- 管理员指令 `/bililive roomcode <code>` 快速切换身份码

### H5 管理面板

- 主播身份码验证，获取主播昵称、头像、房间号
- 头像通过后端代理获取，绕过 B 站防盗链
- 识别码机制：对外展示使用随机识别码，不暴露主播身份码
- 识别码持久化存储（SQLite），重启不丢失
- 实时弹幕预览（弹幕、礼物、SC、大航海，颜色区分）
- MC 模组版本检测，自动获取 GitHub Releases 最新版本

### OBS 弹幕覆盖层

- 独立透明页面 `/danmu/{识别码}`，适配 OBS 浏览器源
- 无需登录即可访问，直接添加到 OBS
- 断线自动重连
- 文字阴影适配各种直播背景

## 项目结构

```
bilibiliChat-MC-Forge/
├── src/                          # MC Forge Mod 源码
│   └── main/java/net/ming/bilibilichatmcforge/
│       ├── client/               # 客户端配置界面
│       ├── utils/                # B 站认证与连接工具
│       ├── Bililichatmcforge.java  # 模组主类
│       └── Config.java           # 配置管理
├── build.gradle                  # Forge 构建配置
└── gradle.properties             # 模组版本与元数据
```

## 快速开始

### 前置条件

- **MC 模组**：Java 17、Minecraft 1.20.1 + Forge 47.4.20
- **H5 插件**：Node.js 18+
- **B 站开放平台**：已申请并获得 Access Key、Access Secret、App ID

申请地址：[哔哩哔哩直播开放平台](https://open-live.bilibili.com/)

### 一、部署 H5 管理插件

#### 1. 配置环境变量

```bash
cd h5-plugin/server
cp .env.example .env
```

编辑 `.env` 文件，填入以下内容：

```env
BILIBILI_ACCESS_KEY=你的AccessKey
BILIBILI_ACCESS_SECRET=你的AccessSecret
BILIBILI_APP_ID=你的AppID
JWT_SECRET=自定义一个随机字符串
PORT=3000
CLIENT_ORIGIN=https://your-domain.com
```

#### 2. 安装依赖

```bash
# 后端
cd h5-plugin/server
npm install

# 前端
cd ../client
npm install
```

#### 3. 本地开发

Windows 用户可直接双击 `h5-plugin/start.bat` 一键启动前后端。

手动启动：

```bash
# 终端 1：启动后端
cd h5-plugin/server
npm run dev

# 终端 2：启动前端
cd h5-plugin/client
npm run dev
```

前端开发服务器默认运行在 `http://localhost:5174`，API 请求自动代理到后端 `http://localhost:3000`。

#### 4. 生产部署

```bash
# 构建前端
cd h5-plugin/client
npm run build

# 启动后端（自动托管前端静态文件）
cd ../server
npm start
```

生产环境只有一个服务运行在 `PORT`（默认 3000），同时提供 API 和前端页面。

### 二、安装 MC 模组

1. 安装 Minecraft Forge 1.20.1
2. 从 [GitHub Releases](https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/releases) 下载最新 `.jar` 文件
3. 将 `.jar` 放入 Minecraft 的 `mods/` 目录
4. 启动游戏

### 三、配置 MC 模组

**方式一：游戏内配置**

进入 Mods 列表 → 找到 BilibiliChat → 打开配置界面，填写：
- B 站开放平台 Access Key / Secret / App ID
- H5 插件服务地址（如 `https://your-domain.com`）
- 主播身份码（roomCode）

**方式二：编辑配置文件**

编辑 Minecraft 实例目录下的 `config/bilibilichat-config.json`。

**方式三：管理员指令**

在游戏内聊天栏输入（需 OP 权限）：

```
/bililive roomcode <你的身份码>
```

## 使用流程

```
主播在 B 站开播 → 获取身份码 → 在 H5 面板验证 → 获取 OBS 弹幕地址
                                         ↓
                              MC 模组自动连接弹幕 → 游戏内显示弹幕
```

1. 主播在 B 站开播，从[开播页面](https://link.bilibili.com/p/center/index#/my-room/start-live)获取身份码
2. 在 H5 管理面板（`https://your-domain.com`）输入身份码完成验证
3. 验证成功后获得：
   - **OBS 弹幕地址**：`https://your-domain.com/danmu/{识别码}`，添加到 OBS 浏览器源
   - **MC 模组下载**：获取最新版本模组
4. 在 MC 模组配置中填入服务地址和身份码，游戏内即可显示弹幕

## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/login` | POST | 身份码验证，返回 JWT + 识别码 + 主播信息 |
| `/api/me` | GET | 查询当前登录状态 |
| `/api/room-info` | GET | 获取主播信息（需 JWT） |
| `/api/avatar` | GET | 头像代理（绕过 B 站防盗链） |
| `/api/mod/latest` | GET | 获取 GitHub Releases 最新版本 |
| `/api/health` | GET | 健康检查 |
| `/ws/danmu/{识别码}` | WS | 公开弹幕 WebSocket（无需认证） |
| `/danmu/ws?token=` | WS | 管理弹幕 WebSocket（需 JWT） |

## 反向代理配置（Nginx）

```nginx
server {
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location /danmu/ws {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    listen 443 ssl;
    # ssl_certificate /path/to/cert.pem;
    # ssl_certificate_key /path/to/key.pem;
}
```

## 技术栈

| 层 | 技术 |
|----|------|
| MC 模组 | Java 17 · Minecraft Forge 47.x |
| 前端 | Vue 3 · Vite 5 |
| 后端 | Node.js · Express · WebSocket |
| 存储 | SQLite（sql.js，纯 WASM） |
| 认证 | JWT |
| 数据源 | 哔哩哔哩直播开放平台 API v2 |

## 注意事项

- `BILIBILI_ACCESS_KEY` 等凭据属于敏感信息，**不要**提交到仓库或公开分享
- B 站头像通过后端代理获取，前端不直接请求 B 站 CDN
- 识别码为 8 位随机字符串，同一身份码始终映射到同一识别码
- OBS 弹幕页面断线后会自动重连（3 秒间隔）
- GitHub Releases 版本信息缓存 10 分钟

## License

当前工程为 All Rights Reserved。详情见 [SECURITY.md](SECURITY.md)。
