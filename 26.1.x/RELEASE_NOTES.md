# BLChat v1.0.0 — Minecraft 26.1.x Support

> 🎉 首个支持 Minecraft 26.1.x 的正式版本 / First release supporting Minecraft 26.1.x
>
> 适配 MC 26.1 / 26.1.1 / 26.1.2 全版本，基于 Forge 62~64 + Java 25 + ForgeGradle 7.x 全新构建。

---

## 📦 下载 / Downloads

请根据你的 Minecraft 版本选择对应的 jar 文件 / Choose the jar that matches your Minecraft version:

| Minecraft | Forge | Download |
|-----------|-------|----------|
| 26.1 | 62.0.9 | `bilibilichatmcforge-26.1-1.0.0.jar` |
| 26.1.1 | 63.0.2 | `bilibilichatmcforge-26.1.1-1.0.0.jar` |
| 26.1.2 | 64.0.8 | `bilibilichatmcforge-26.1.2-1.0.0.jar` |

> ⚠️ 每个 jar 仅兼容对应的 MC 版本，请勿混用 / Each jar only supports its target MC version.

---

## ✨ 新特性 / What's New

### 多版本支持 / Multi-Version Support
- 3 个独立 jar 覆盖 MC 26.1、26.1.1、26.1.2 三个版本
- 3 standalone jars covering MC 26.1, 26.1.1, and 26.1.2
- 共享源码架构，保证功能一致性
- Shared source architecture ensures feature parity

### 适配 MC 26.1.x 全新 API / Adapted to MC 26.1.x New APIs
- **GuiGraphicsExtractor**：渲染类重命名适配
- **extractRenderState**：新的渲染管线状态提取模式
- **新权限系统**：通过反射调用 `permissions().hasPermission(Permission)` API
- **EventBus 7.x**：使用 `net.minecraftforge.eventbus.api.listener.SubscribeEvent`

### 构建系统升级 / Build System Upgrade
- **ForgeGradle 7.x**：全新插件与依赖语法
- **Java 25**：适配 Mojang 官方运行时
- **Gradle 9.3.0**：最新构建工具

---

## 🔧 安装 / Installation

1. 安装对应版本的 [Minecraft Forge](https://files.minecraftforge.net/)
2. 将下载的 jar 文件放入 `.minecraft/mods/` 目录
3. 启动游戏，进入 Mod 配置界面输入 B 站直播间身份码
4. 保存后即可在游戏内接收弹幕

```
.minecraft/
└── mods/
    └── bilibilichatmcforge-26.1.x-1.0.0.jar
```

### 前置要求 / Requirements
- Minecraft 26.1 / 26.1.1 / 26.1.2
- 对应版本的 Forge（62.x / 63.x / 64.x）
- Java 25（Forge 安装器会自动安装）

---

## 📋 版本对照 / Version Matrix

| Jar | MC 版本 | Forge 版本 | EventBus | Java |
|-----|---------|-----------|----------|------|
| `bilibilichatmcforge-26.1-1.0.0.jar` | 26.1 | 62.0.9 | 7.x | 25 |
| `bilibilichatmcforge-26.1.1-1.0.0.jar` | 26.1.1 | 63.0.2 | 7.x | 25 |
| `bilibilichatmcforge-26.1.2-1.0.0.jar` | 26.1.2 | 64.0.8 | 7.x | 25 |

---

## 🐛 已知问题 / Known Issues

- `ModLoadingContext.get()` 弃用警告（不影响功能，可忽略）
- `ModLoadingContext.get()` deprecation warning (non-functional, safe to ignore)

---

## 📝 技术细节 / Technical Details

### MC 26.1.x 主要变更 / MC 26.1.x Key Changes
- 移除混淆映射，Minecraft 不再混淆
- `GuiGraphics` 重命名为 `GuiGraphicsExtractor`
- 渲染方法 `render()` 改为 `extractRenderState()`
- `pack.mcmeta` 使用 `max_format`/`min_format` 替代 `pack_format`
- 强制要求 Java 25

### 构建验证 / Build Verification
- ✅ 3 个 jar 全部构建成功
- ✅ 3 个 jar 客户端启动成功（LWJGL OpenGL/STB 加载正常）

---

## 🔗 相关链接 / Links

- [完整更新日志 / Full Changelog](../26.1.x/CHANGELOG.md)
- [项目主页 / Project Home](https://github.com/your-repo/BLChat)
- [问题反馈 / Issue Tracker](https://github.com/your-repo/BLChat/issues)

---

## 💎 致谢 / Credits

- Minecraft Forge 团队
- 哔哩哔哩直播开放平台
- 所有测试与反馈的用户

---

**Full Changelog**: `1.21.x/v1.0.0...26.1.x/v1.0.0`
