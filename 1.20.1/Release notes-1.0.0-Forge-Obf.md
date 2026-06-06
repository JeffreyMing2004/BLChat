# Release Notes - bilibiliChat-MC-Forge v1.0.0

## 版本信息

- **版本号**: 1.0.0
- **Minecraft 版本**: 1.20.1
- **Forge 版本**: 47.4.20

## 功能特性

### 核心功能
- 实时弹幕显示：在 Minecraft 聊天栏中显示 B 站直播间弹幕
- 礼物通知：显示观众赠送的礼物信息（名称和数量）
- SC 消息：显示 Super Chat 消息（发送者、金额、内容）
- 大航海通知：显示用户开通大航海（总督、提督、舰长）的信息

### 配置方式
- 游戏内配置界面：通过 Mods 列表直接访问，仅需输入房间码
- 配置文件：`config/bilibilichat-config.json`
- 管理员指令：`/bilibili roomcode <房间码>`（需要 OP 权限等级 2）

### 连接机制
- 使用 B 站公开直播 WebSocket（`wss://broadcastlv.chat.bilibili.com/sub`）
- 无需 Open Platform 认证（无 AccessKey / AccessSecret / AppId）
- 自动重连：连接断开时自动尝试重新连接
- 心跳保活：每 30 秒发送心跳包维持连接

## 技术实现

### 消息协议
- 弹幕消息：`DANMU_MSG` 命令
- 礼物消息：`SEND_GIFT` 命令
- SC 消息：`SUPER_CHAT_MESSAGE` 命令
- 大航海消息：`GUARD_BUY` 命令

### 消息格式
- 弹幕：`[B站 弹幕] 用户名: 消息内容`
- 礼物：`[B站 礼物] 用户名 赠送了 礼物名称 x数量`
- SC：`[B站 SC] 用户名 (￥金额): 消息内容`
- 大航海：`[B站 大航海] 用户名 开通了 大航海等级`

## 文件说明

### 构建产物
- `bilibilichatmcforge-1.0.0.jar` - ForgeGradle 重混淆版（19.45 KB）
- `bilibilichatmcforge-1.0.0-obfuscated.jar` - ProGuard 混淆版（11.53 KB）

### ProGuard 混淆规则
- 保留 Mod 入口类和注解
- 保留事件处理器方法
- 保留 Gson 序列化类
- 保留 Minecraft/Forge API 引用
- 移除调试信息
- 代码优化（5 轮）

## 使用说明

### 安装步骤
1. 安装 Minecraft Forge 1.20.1（版本 47.4.20 或兼容版本）
2. 将 `bilibilichatmcforge-1.0.0-obfuscated.jar` 放入 `mods/` 目录
3. 启动 Minecraft

### 配置方法
1. **游戏内配置**：
   - 打开 Mods 列表
   - 找到 "bilibiliChat-MC-Forge"
   - 点击 "配置" 按钮
   - 输入 B 站直播间房间码
   - 点击 "保存并应用"

2. **配置文件**：
   ```json
   {
     "roomCode": "你的房间码"
   }
   ```

3. **管理员指令**：
   ```
   /bilibili roomcode <房间码>
   ```

### 获取房间码
房间码是 B 站直播间的数字 ID，可以从直播间 URL 中获取：
- 直播间 URL：`https://live.bilibili.com/1234567`
- 房间码：`1234567`

## 系统要求

- **Minecraft**: 1.20.1
- **Forge**: 47.4.20 或兼容版本
- **Java**: 17 或更高版本
- **网络**: 需要能够访问 B 站直播服务器

## 已知限制

- 仅支持 B 站直播间弹幕，暂不支持其他平台
- 需要直播间正在直播才能接收弹幕
- 部分特殊弹幕（如表情包）可能无法完整显示

## 更新日志

### v1.0.0 (2026-06-06)
- 初始正式版本发布
- 实现基本弹幕显示功能
- 支持礼物、SC、大航海消息显示
- 提供游戏内配置界面
- 支持管理员指令
- 应用 ProGuard 代码混淆

## 许可证

All Rights Reserved

## 作者

**JeffreyMing**
- GitHub: https://github.com/JeffreyMing2004

## 相关链接

- 项目主页：https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge
- 问题反馈：https://github.com/JeffreyMing2004/BilibiliChat-MC-Forge/issues
