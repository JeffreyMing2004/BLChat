# Bilibili Chat Mod 正式版 (1.20.1)

Minecraft Forge Mod，在游戏内实时查看 B 站直播间弹幕。

## 特性

- 实时弹幕显示
- 礼物、SC、大航海消息通知
- 游戏内配置界面（仅需房间码）
- 自动重连
- 管理员指令 `/bilibili roomcode <房间码>`

## 配置

只需一个参数：**房间码**

- 游戏内：Mods 列表 → bilibiliChat → 配置
- 配置文件：`config/bilibilichat-config.json`

```json
{
  "roomCode": "1234567"
}
```

## 指令

需要 OP 权限（权限等级 2）：

```
/bilibili roomcode <房间码>
```

## 技术说明

- 使用 B 站公开直播 WebSocket（`wss://broadcastlv.chat.bilibili.com/sub`）
- 无需 Open Platform 认证（无 AccessKey / AccessSecret / AppId）
- 支持的消息类型：弹幕、礼物、SC、大航海
