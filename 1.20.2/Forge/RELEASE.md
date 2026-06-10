# BLChat v1.0.0

Minecraft Forge Mod，在游戏内实时查看 B 站直播间弹幕。

## 功能

- 弹幕显示：游戏聊天栏实时显示 B 站直播间弹幕
- 礼物通知：显示观众赠送的礼物信息（名称和数量）
- SC 消息：显示 Super Chat 醒目留言消息
- 大航海通知：显示总督、提督、舰长开通信息
- 自动重连：连接断开时自动尝试重新连接
- 游戏内配置：通过 Mods 列表直接配置，无需编辑文件
- 管理员指令：`/bilibili identitycode <身份码>` 快速切换身份码

## 配置

只需一个参数：**身份码**

从 B 站开播设置 - 个人中心 - bilibili link 中获取。

配置方式：
1. 游戏内：Mods 列表 → BLChat → 配置
2. 配置文件：`config/bilibilichat-config.json`
3. 指令：`/bilibili identitycode <身份码>`

## 消息格式

| 类型 | 格式 |
|------|------|
| 弹幕 | `[B站 弹幕] 用户名: 消息内容` |
| 礼物 | `[B站 礼物] 用户名 赠送了 礼物名称 x数量` |
| SC | `[B站 SC] 用户名 (￥金额): 消息内容` |
| 大航海 | `[B站 大航海] 用户名 开通了 大航海等级` |

## 环境要求

- Minecraft 1.20.1
- Forge 47.4.20+
- Java 17+

## 下载

| 文件 | 说明 |
|------|------|
| `blchat-1.0.0.jar` | Forge 版（未混淆） |
| `blchat-1.0.0-obf.jar` | Forge 版（代码混淆） |

## 链接

- https://github.com/JeffreyMing2004/BLChat
- https://modrinth.com/mod/blchat
