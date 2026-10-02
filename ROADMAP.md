# BLChat 后续更新与修复计划 / Roadmap

> 当前版本：`1.0.5-308`（5 条版本线，12 个 jar）· 更新于 2026-10-02
>
> 本计划基于 2026-10-02 的全量代码审查编写，缺陷均有源码位置可查。
> 顺序表示优先级，不承诺具体日期；勾选框用于跟踪进度。

---

## 目录

1. [现状盘点](#现状盘点)
2. [修复计划（按严重度分级）](#修复计划)
3. [功能更新计划（里程碑）](#功能更新计划)
4. [版本与发布计划](#版本与发布计划)
5. [服务端协同待办](#服务端协同待办)
6. [新版本线例行适配清单](#新版本线例行适配清单)
7. [回归测试清单](#回归测试清单)
8. [决策点](#决策点)

---

## 现状盘点

- 5 条版本线（1.20.x / 1.21.x / 26.1.x / 26.2.x / 26.3.x）共 12 个 jar，Forge 46~66，Java 17/21/25
- 已支持：弹幕 / 礼物 / SC / 大航海四类事件进聊天栏；身份码三种配置方式；断线重连；双心跳；包大小防护；版本检测；中英双语
- 架构：`shared/`（BilibiliClient、VersionChecker、JsonConfigManager）+ 各 forge 子工程（主类、Config、配置屏幕、VersionCompat）
- 版本检测端点按版本线划分：`/1.20/`、`/1.21/`、`/26/26.1/`、`/26/26.2/`、`/26/26.3/`
- 源码重复现状：`BilibiliClient` / `JsonConfigManager` 在 5 条版本线间为纯复制；1.20.x 线结构与其它线不同（主类与 Config 放在 shared 中）

---

## 修复计划

> 分级说明：**P0** = 影响核心可用性，下一版必修；**P1** = 稳定性缺陷，v1.1 修复；
> **P2** = 打磨项，可排入 v1.1~v1.2。除特别标注外，**每个缺陷都存在于全部 5 条版本线**，
> 修复时需同步各线 shared（在 CI 一致性校验落地前靠人工同步）。

### P0 — 核心可用性

> **进度：F1、F2、F3 已于 2026-10-02 完成代码修复**（JSON 单一数据源 + 统一重连入口 `scheduleReconnect` + 心跳失败计数触发重连），已同步全部 5 条版本线，待构建与发布。

#### F1. ✅ 已修复：双轨配置互相覆盖，重启后身份码丢失

- **证据**：`Config.onLoad`（如 `26.3.x/forge-26.3/.../Config.java:20`）在每次 `ModConfigEvent`（Loading 与 Reloading 都会触发）时执行 `JsonConfigManager.setIdentityCode(IDENTITY_CODE.get())`，用 Forge TOML（`bilibilichatmcforge-common.toml`，默认空）的值覆盖 JSON 配置；而配置屏幕（`BilibiliConfigScreen.java:35`）和 `/bilibili identitycode` 指令只写 JSON，**没有任何代码路径写 TOML**。
- **影响**：时序为「构造器读 JSON → COMMON 配置加载事件覆盖为 TOML 空值（且 save() 把空值落盘）」，用户通过界面/指令设置的身份码在**重启游戏后丢失**，只能每次进游戏重填。保存→重连在本会话内生效，掩盖了该问题。
- **方案**：JSON 作为唯一数据源。删除 `ForgeConfigSpec` 注册（顺带消除多余的 `-common.toml` 文件），配置屏幕继续读写 JSON；`Mods` 列表入口保留自定义屏幕。
- **验收**：设置身份码 → 重启游戏 → 无需重新配置即可连接直播间；`config/bilibilichat-config.json` 内容不再被清空。

#### F2. ✅ 已修复：`app/start` 失败后永久停止，不重试

- **证据**：`BilibiliClient.connect()`（如 `26.3.x/shared/.../BilibiliClient.java:152-158`）任何异常（DNS 瞬断、HTTP 超时、接口 5xx）都走 `fail()` → `running=false`，之后无人再触发重连。WebSocket 的 `onClose` 有重连逻辑，但 connect 阶段失败不在其覆盖范围。
- **影响**：开播前网络抖动一下，本次游戏会话内弹幕永久失效，只能重启游戏或重填身份码触发重连。
- **方案**：connect 失败纳入与 WS 断线相同的退避重试（见 F4），重试次数计入同一上限。
- **验收**：启动时断网 30 秒后恢复 → 自动连上；日志可见退避重试序列。

#### F3. ✅ 已修复：应用心跳失败被静默忽略，会话被回收后无感知

- **证据**：`BilibiliClient.postAsync()`（`BilibiliClient.java:199-205`）丢弃 `sendAsync` 返回值，20 秒一次的 `v2/app/heartbeat` 失败（断网、B 站回收）无任何处理。
- **影响**：心跳断流 → B 站回收弹幕会话 → 游戏内弹幕静默停止，且 WS 可能仍显示"已连接"，主播毫无察觉。
- **方案**：记录连续失败次数，超阈值（如 3 次）主动终止 WS 并走重连；同时作为 F5 心跳 watchdog 的一部分。
- **验收**：模拟心跳接口不可达 → 1 分钟内游戏内收到重连提示并恢复。

### P1 — 稳定性

#### F4. 重连策略过于简单

- **证据**：`BilibiliClient.java:319-326`，固定 5 秒间隔、上限 5 次，达上限即 `fail()` 永久停止。
- **方案**：指数退避（5s → 10s → 20s → 40s → … 封顶 5 分钟）；重连上限提高且可配置；长时间存活后成功计数清零已有（`thenAccept` 中 `reconnects=0`），保留。
- **验收**：直播中拔网线 10 分钟再恢复 → 自动恢复弹幕。

#### F5. WS 假死无 watchdog

- **证据**：`handlePackets` 跳过 op=3 心跳回复（`BilibiliClient.java:372-373`），从不校验"发出心跳是否收到回复"；半开连接（无 close 帧）永远不会触发 `onClose` 重连。
- **方案**：记录最近一次 op=3 回复时间；连续 N 个心跳周期（如 2 个 × 30s）无回复即判定假死，主动 `sendClose` 并重连。与 F3、F4 共用重连通道。
- **验收**：用代理工具静默丢弃入站包 → 2 分钟内自动重建连接。

#### F6. HTTP 客户端无超时

- **证据**：`BilibiliClient.java:45` 的 `HttpClient` 未设 `connectTimeout`；`post()`（191-197 行）的 `HttpRequest` 未设 `.timeout()`。与 `VersionChecker`（已设 10s/15s）不一致。
- **影响**：`connect()` 在 ForkJoinPool 公共线程池上执行，挂起的请求会无限阻塞。
- **方案**：HttpClient 设 `connectTimeout(10s)`，每个请求设 `timeout(15s)`。
- **验收**：代码审查 + 构建通过；行为由 F2 的重试覆盖。

#### F7. 重连时旧 game_id 泄漏

- **证据**：`connect()` 重新 `app/start` 时直接覆盖 `gameId`（`BilibiliClient.java:132`），旧会话从未调用 `v2/app/end`。
- **影响**：开放平台对未结束会话有数量限制；异常断开多次后可能 `start` 报错。
- **方案**：重新 connect 前若 `gameId != null`，异步 `app/end` 旧 id（复用 `stop()` 中的逻辑，抽成 `endSession(String id)`）。
- **验收**：日志确认每次新建会话前旧的被 end。

#### F8. 缺失语言键 `error.app_start_failed`（✅ 已随 F2 修复吸收：app/start 失败改走统一重连入口，最终提示复用已存在的 `connect_failed` 键，缺失键不再被引用）

- **证据**：`BilibiliClient.java:126` 使用 `mod.bilibilichatmcforge.error.app_start_failed`，但 5 条版本线的 `zh_cn.json` / `en_us.json` 中均无此键（已 grep 验证计数为 0）。
- **影响**：`app/start` 失败时玩家看到原始翻译键而非可读文案。
- **方案**：两个语言文件补键；顺手全量核对代码中 `Component.translatable` 的键与语言文件差集。
- **验收**：填写错误身份码启动 → 显示完整中文/英文文案。

### P2 — 打磨

- [ ] **F9. `guardName` 硬编码中文**（`BilibiliClient.java:437-443`）：总督/提督/舰长作为字符串参数传入 translatable 模板，英文用户看到中文。改为 `guard.level.1/2/3` 翻译键。
- [ ] **F10. `VersionChecker` 版本回退值误导**（`VersionChecker.java:132`）：资源读取失败时回退 `"1.0.4.0"`，会参与版本比较；改为回退 `"0"`（永不提示更新）并告警。
- [ ] **F11. `VersionCompat.checkPermission` 忽略 level 参数**（`VersionCompat.java:52`）：形参 `level` 未使用，恒判 GAMEMASTERS。要么按 level 映射权限，要么删参数并在调用处注明。
- [ ] **F12. 配置屏幕无状态反馈**：不显示当前连接状态/直播间号，空身份码点保存无提示。配合 v1.1 的 `/bilibili status` 一起做。
- [ ] **F13. 指令改码不回写屏幕**：从指令设置后打开配置屏幕能看到新值（都读 JSON），无需修改；此条核实后关闭即可。

---

## 功能更新计划

> 依赖关系：v1.0.6（P0/P1 修复）→ v1.1（体验增强）→ v1.2（工程化，为 v1.3/v2.0 铺路）
> → v1.3（互动）→ v2.0（架构）。里程碑内条目可按反馈调整。

### v1.0.6 — 稳定性修复版（首个发布）

范围 = 上述 **P0 全部 + P1 全部**（F1~F8）。F1~F3（含 F8）已完成代码修复，剩余 F4~F7；不改任何功能行为，只修缺陷。
发布前跑一遍[回归测试清单](#回归测试清单)。

### v1.1 — 显示与配置增强

**目标：把"看弹幕"的体验做专业，主播能按自己直播间风格定制显示。**

- [ ] **事件开关与消息模板** — 每类事件独立开关；消息格式（前缀/颜色）可配置。配置项数量增加，JSON 配置结构需设计版本号字段（`config_version`）与迁移逻辑
- [ ] **礼物连击聚合** — 同一观众连击礼物合并展示总数（用开放平台 combo 信息），避免刷屏
- [ ] **SC / 大航海高亮** — ActionBar 或 Title 突出展示，可选音效
- [ ] **过滤器** — 关键词黑名单、用户黑名单、最小礼物价值阈值
- [ ] **指令扩展** — `/bilibili status`（连接状态/game_id/直播间）、`/bilibili reconnect`、`/bilibili reload`；语言文件补齐对应键
- [ ] **新事件接入** — 进入直播间、点赞、关注（以开放平台 v2 实际推送的事件类型为准，逐个验证字段）
- [ ] **配置屏幕升级**（含 F12）— 显示连接状态与错误原因，身份码格式预校验

### v1.2 — 稳定性与工程化

**目标：长时间直播无人值守；多版本维护成本降下来。**

- [ ] **shared 一致性 CI** — 校验各版本线 `shared/src` 一致（注意 1.20.x 线结构不同：主类/Config 在 shared 中），不一致构建失败；进一步可评估单一 shared 工程统一编译
- [ ] **单元测试** — 协议解析（`handlePackets` 用录制样例二进制包）、版本比较（曾出非标准版本号 bug）、HMAC 签名、`guardName`、配置迁移逻辑；均不依赖 MC 类，JUnit 直接跑
- [ ] **发布自动化** — 打 tag → GitHub Actions 自动 Release（附 12 个 jar 与更新说明）→ 推送 Modrinth
- [ ] **CI 缓存** — `actions/setup-java` 加 `cache: gradle`，当前每次构建全量下载依赖
- [ ] **文档还账** — 新建 `CHANGELOG.md`；`RELEASE.md` 整体重构（当前版本历史停在 v1.0.3/26.1.2，仓库链接还是旧仓库名 `BilibiliChat-MC-Forge`）；CONTRIBUTING 补「加版本线」指引

### v1.3 — 互动玩法

**目标：从"显示弹幕"到"弹幕影响游戏"。**

- [ ] **弹幕指令互动** — 观众发 `!<指令>` 触发可配置游戏内动作；白名单 + 频率限制 + OP 总开关，默认关闭
- [ ] **礼物 / SC 触发游戏效果** — 模板化规则（如 SC ≥ X 元触发全屏烟花），规则可配置
- [ ] **感谢消息聚合** — 感谢礼物/上舰/关注，带延迟聚合窗口防刷屏
- [ ] **（调研）反向发弹幕** — 评估开放平台 v2 发送弹幕接口的权限要求与可行性

### v2.0 — 架构升级

**目标：治本凭据安全问题，扩展使用场景。**

- [ ] **代理接入模式（重点）** — 签名逻辑与凭据移到自有后端（复用 h5.mingpixel.net 基础设施），模组仅持身份码。现状是凭据经 XOR+Base64 混淆内置 jar（`BilibiliClient.unmask`），属可逆混淆，存在被提取滥用风险。收益：密钥不随 jar 分发、轮换不用发版、防配额滥用。**主播体验不变：仍只填一个身份码**；直连模式保留为回退
- [ ] **多直播间支持** — 一个客户端聚合多个主播（联机服务器多主播场景）
- [ ] **（决策）加载器扩展** — 评估 NeoForge / Fabric 移植的投入产出比
- [ ] **H5 面板联动** — 面板验证身份码后游戏内免配置

---

## 版本与发布计划

- **版本号**：单一来源 `version.properties`（`mod_version=1.0.5-308`，`-308` 为构建号），构建时注入 `mods.toml` 与 `blchat-version.properties`。修复版 +1 次版本号（1.0.6），功能里程碑按次版本号（1.1.0）
- **发布流程**（v1.2 自动化前的手工版）：
  1. `version.properties` 升版本号
  2. 全版本修复同步到各线 shared（CI 一致性校验落地前人工核对 diff）
  3. 本地 `build-all.bat` 或推送触发 CI，跑[回归测试清单](#回归测试清单)
  4. GitHub Release：`v<版本>` tag，附 12 个 jar（命名 `BLChat-<MC版本段>-<模组版本>.jar`）与更新说明
  5. Modrinth 同步发布（支持版本矩阵在后台勾选）
  6. `version.mingpixel.net` 对应版本线路径更新 `version.blchat`（JSON：`version` + `changelog` 字段）
- **Beta 通道**：README 承诺赞助者优先体验 Beta。方案：GitHub prerelease + 赞助者群发链接，稳定后转正发布。v1.1 发布前落地

## 服务端协同待办

- [ ] `version.mingpixel.net` 新增 `/26/26.3/version.blchat`（26.3 客户端已在请求，缺失时只告警 404）
- [ ] 评估版本检测路径收敛：模组端改为统一路径 + 版本线参数（如 `/check?line=26.3`），以后加版本线不用配服务端路由
- [ ] v2.0 代理模式的后端排期（H5 后端独立仓库，需预留签名转发接口）

## 新版本线例行适配清单

新版本线（如 26.4）固定走这份清单：

1. 复制最近版本线目录，改 `build.gradle`（Forge 依赖号、archivesName）、`settings.gradle`、`mods.toml`（loaderVersion、forge/minecraft 依赖范围）
2. 对照新版本官方 MDK 核对：ForgeGradle 插件版本、Gradle wrapper、eventbus-validator、`pack.mcmeta` 格式号（26.2→26.3 曾从 101 变 121）
3. `shared/` 中 `VersionChecker` 的版本检测 URL 改为新线路径
4. `tools/build-all-versions.ps1`、对应 `build-all.bat`、`.github/workflows/build.yml` 矩阵加入新线
5. README（中英）与 RELEASE.md 版本表更新
6. 编译级验证：本地 `gradlew build`（源码在相邻 MC 版本间通常零改动即可编译）
7. 回归：连接直播间 → 四类事件显示 → 断网重连 → 退出无挂起
8. 服务端加对应版本检测路径

## 回归测试清单

每次发布前全过（单机 + 局域网各一遍）：

- [ ] 全新安装（无任何配置文件）→ 启动不崩，提示配置身份码
- [ ] 配置屏幕填码保存 → 立即连接成功；**重启游戏后仍保留**（F1 修复的专项验收）
- [ ] `/bilibili identitycode` 改码 → 立即重连生效，重启后保留
- [ ] 弹幕 / 礼物 / SC / 大航海 四类事件全部正确显示（中英文语言各查一遍，含舰队名称）
- [ ] 填错误身份码 → 显示可读的错误文案而非翻译键（F8 专项）
- [ ] 启动阶段断网 30 秒后恢复 → 自动重连成功（F2 专项）
- [ ] 直播中断网 10 分钟再恢复 → 自动恢复（F4 专项）
- [ ] WS 假死模拟（防火墙静默丢弃入站包）→ 2 分钟内重建连接（F5 专项）
- [ ] 长时间挂机（≥4 小时）弹幕不断流（F3 专项）
- [ ] 退出游戏进程正常结束（守护线程验证）
- [ ] 版本检测三态：版本一致 / 过低提示+更新日志 / 服务不可达仅日志告警

## 决策点

1. **是否支持专用服务器** — 目前 `clientSideOnly=true`，模组只在客户端（单人/局域网）加载；去掉后可支持纯服务端（多人联机统一看弹幕），需在专用服务器完整测试事件总线与权限行为
2. **代理模式节奏** — v2.0 依赖 H5 后端改造排期；可先在模组端预留代理配置项
3. **NeoForge / Fabric 是否投入** — 与专用服务器决策相关：若 26.x 的服务端生态转向 NeoForge，需要一起评估
