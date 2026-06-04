# 贡献指南（Contributing）

感谢你愿意为本项目做贡献。

## 提交内容类型

- Bug 修复
- 新功能
- 文档与示例补充
- 本地化（语言文件）完善
- 性能与稳定性改进

## 开发环境

- Java：17
- Minecraft：1.20.1
- Forge：47.4.20

## 开始开发

1. Fork 本仓库并创建分支
2. 使用 Java 17 打开工程
3. 通过 Gradle 任务运行调试

```bash
./gradlew runClient
```

## 构建

```bash
./gradlew build
```

构建产物：`build/libs/*.jar`

## 配置与敏感信息

本 Mod 需要哔哩哔哩开放平台参数（Access Key / Secret / App ID / 身份码）。

- 不要在任何提交中包含真实的 Access Key / Secret
- 不要把你的 `config/bilibilichat-config.json` 提交到仓库
- 如果需要演示或测试，请使用假数据或临时申请的测试凭据

## 提交规范

- 尽量保持一次 PR 做一件事（单一目的）
- PR 描述中说明：
  - 解决了什么问题 / 新增了什么能力
  - 如何复现 / 如何验证
  - 是否涉及配置、网络请求或协议字段变更

## 代码风格

- 以现有代码风格为准
- 避免在日志中输出敏感信息
- 若新增可配置项，请同步更新语言文件（`assets/bilibilichatmcforge/lang/`）

## PR 检查项

提交前至少确认：

- `./gradlew build` 可通过
- 不包含敏感信息（密钥、token、个人信息）
- 对用户可见的文本已补充中英文（如适用）
