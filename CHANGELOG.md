# ReportDesk 更新日志

> 本文件按语义化版本（SemVer）记录 ReportDesk 的全部变更，格式遵循 `AGENTS.md` 第 10 节模板：
> 每条日志包含版本号、发布日期、中英文概述各一段，以及按分类的更新一览。已有文件只追加、不覆盖。

---

## Release 1.0.0

**发布日期** 2026-11-01 · **标签** `v1.0.0` · **产物** `ReportDesk-1.0.0.jar` · **许可证** GPL-3.0-or-later

> 本条目为项目**首个版本**：所有条目均为首次引入的功能与基线设计，因此不设「优化」「修复」分类
>（无既有版本可作为对比基线）。

### 概述

ReportDesk 1.0.0 是项目首个正式发布版本，完整实现了「玩家举报 → 管理员受理 → 结案归档」工作流：玩家端举报 GUI 与 `/report` 命令、管理端 `/reports` 工作台式受理流程（20 余个子命令与 54 格管理 GUI）、四级优先级与 SLA 超时提醒、举报回复串、关注列表与离线举报人补发队列、Discord Webhook 推送、可整体开关的证据采集增强层（方块破坏采样、移动轨迹、聊天记录、自动检测）与联动处罚（均默认关闭），以及游戏内配置编辑器。所有业务阈值集中于 `config.yml`，代码中无任何硬编码魔法数；举报记录、证据、审计日志全部在专用异步线程上持久化，主线程零文件 IO、零数据库 IO；所有增强功能默认 `false`，关闭路径入口仅一次开关判断即短路返回。本版本兼容 Paper 1.20–1.21.x（`api-version` 锁定为 `'1.20'`，Java 17 字节码），并随附 27 个测试类覆盖配置加载与校验、限流、输入校验、举报状态机、存储 WAL 回放、审计日志、CSV 导出、Webhook 载荷与 i18n 键完整性。

1.0.0 同时确立了工程基线：`AGENTS.md` 十项开发硬约束（`api-version` 锁定、阈值配置驱动、高频事件限制、权限/限流/输入校验、异步存储、固定构建命令、Git 备份流程、更新日志强制记录）、GPLv3 许可证、README 与 ROADMAP 文档、`docs/` 文档站（9 个章节 + 索引），以及官方图标资产。

### Summary

ReportDesk 1.0.0 is the first official release, delivering the complete "report → triage → closure" workflow: a player-facing report GUI and `/report` command, an admin `/reports` desk with 20+ subcommands and a 54-slot admin GUI, four-tier report priority with SLA overdue alerts, per-report reply threads, a staff watchlist with an offline reporter notice queue, Discord Webhook push, an opt-in evidence layer (block-break sampling, movement trails, chat capture, auto-detection) and opt-in linked punishments — all disabled by default — plus an in-game configuration editor. Every business threshold lives in `config.yml` with no hardcoded values in code; report, evidence and audit data are persisted on dedicated asynchronous threads, keeping the main thread free of file and database IO; every enhancement feature defaults to `false` and short-circuits on a single switch check when disabled. This release targets Paper 1.20–1.21.x (`api-version` pinned to `'1.20'`, Java 17 bytecode) and ships 27 test classes covering configuration loading and validation, rate limiting, input validation, the report state machine, storage WAL replay, audit logging, CSV export, Webhook payloads and i18n key parity.

The 1.0.0 release also establishes the project's engineering baseline: the ten hard constraints of `AGENTS.md` (pinned `api-version`, config-driven thresholds, high-frequency event restrictions, permission/rate-limit/input validation, asynchronous storage, a fixed build command, a mandatory git backup workflow and a mandatory changelog), GPLv3 licensing, README/ROADMAP documentation, a nine-section documentation site under `docs/` (10 files, 933 lines), and the official icon assets.

### 亮点

- **完整闭环工作流**：玩家 GUI 提交（选人 → 选分类 → 聊天输入详情 → 确认提交）与 20 余个子命令的管理受理流程（认领 → 关闭/驳回/重复 → 重开 → 删除 → 归档清理）全部产品化。
- **主线程零存储 IO**：高频操作经 WAL 追加一行即返回（O(1) 持久化），落盘、压缩、审计与导出全部在专用线程执行。
- **阈值零硬编码**：全部业务参数由 `config.yml` 驱动，缺失或非法值自动回退默认值并输出控制台警告。
- **默认安全的增强层**：证据采集、自动检测、联动处罚、轨迹采样默认关闭；关闭状态下零监听器注册、零事件开销。
- **全程可审计**：10 个权限节点 + 玩家侧 8 重限流 + 管理侧节流，所有管理操作与自动维护动作写入按天分文件的异步审计日志。
- **工程约束固化**：`AGENTS.md` 十项硬约束 + 固定构建命令 `./gradlew test build` + 27 个测试类。

### 1.0.0 更新一览（2026-11-01）

#### 新增

*玩家侧*

- **举报入口**（`/report`，别名 `rdreport`/`jb`，权限 `reportdesk.report` 默认所有人）：
  - 无参数打开举报 GUI：选择被举报对象（在线玩家分页列表）→ 选择分类 → 聊天输入详情（默认 120 秒自动过期，`/report cancel` 随时取消）→ 确认页提交；GUI 关闭时自动切换聊天输入模式。
  - `/report <玩家>` 直接对指定玩家发起举报；`/report <玩家> <分类> [详情...]` 命令行提交，分类支持配置别名（如 `cheat`/`xray`/`wai_gua`）。
  - 玩家自助子命令：`status [页码]` 分页查看本人举报列表（每页条数由 `reports.text-page-size` 控制，默认 10）、`view`/`info <id>` 查看详情（同时标记未读回复为已读）、`reply <id> <内容>`、`replies`/`thread <id>`、`help`。
  - 聊天记录多选举报（`reports.chat-report`，默认关闭）：启用证据层聊天日志采集后，可在举报流程中勾选目标最近至多 5 条聊天消息作为证据随单提交，管理端 GUI 提供「聊天记录审阅」分页页。
- **举报分类体系**：内置 7 个默认分类——`cheating`（high）、`grief`（high）、`chat`（normal）、`advertising`（normal）、`bug`（normal）、`inappropriate`（low）、`other`（low）——每个分类可独立配置显示名、图标（Bukkit Material）、命令别名、启用开关、展示优先级与默认严重度，并支持在 `config.yml` 中增删；语言文件可本地化覆盖分类名与描述（覆盖 `config.yml` 的 `display` 值）。

*管理侧*

- **`/reports` 管理命令**（别名 `reportdesk`/`rd`，权限 `reportdesk.admin`；无参数时直接打开管理 GUI）：
  - 受理子命令：`menu`/`gui`/`admin`、`list [页码]`、`search <关键字>`、`view`/`info <id>`、`claim <id>`、`release <id>`、`close <id> <裁决> [备注]`、`reopen <id>`、`tp <玩家>`、`evidence <id>`、`note <id> <内容>`、`reply <id> <内容>` / `replies <id>`、`delete <id>`、`purge <天数>`、`overdue`。
  - 运维子命令：`watch <玩家>` / `unwatch` / `watching`（关注列表）、`stats`（统计面板）、`export`（CSV 导出）、`audit [页码]`（审计日志分页）、`reload`（重载配置与语言）、`config list/get/set/reset`（游戏内配置编辑器）。
  - 裁决支持 5 种：`RESOLVED` / `REJECTED` / `DUPLICATE` / `INSUFFICIENT`（以 `REJECTED` 状态存储、独立裁决标签）/ `AUTO_CLOSED`，解析接受英文与中文别名（如 `成立`/`不成立`/`证据不足`/`重复`/`自动关闭`，及 `ACCEPT`/`FALSE`/`DUP` 等）。
  - 删除与清除类操作要求 `reportdesk.admin.delete` 权限；查看证据要求 `reportdesk.evidence`。
- **管理 GUI**（54 格工作台，`gui.admin.enabled` 默认开启）：
  - 首页（hub）：数据概览与入口——举报列表、下一条未处理举报快捷入口、玩家搜索、统计面板、关注列表、设置、审计日志等。
  - 举报列表页：按状态、分类、关键字、认领人、优先级、仅超时（`overdueOnly`）组合筛选，三种排序（`NEWEST` 默认 / `OLDEST` / `PRIORITY`），分页浏览。
  - 详情页：状态、优先级、SLA 截止与超时标记、认领/关闭时间线、备注、证据概要、回复串快捷入口，以及认领、关闭、重开、删除（二次确认）、备注、回复等操作按钮。
  - 玩家搜索页：在线/离线玩家检索与按目标聚合的未处理举报数；审计分页页；统计页（总量、状态/裁决分布、平均处理时长、分类分布、Top 目标 3 名、Top 管理员 5 名）；设置页（配置「区段 → 键 → 值」三级浏览，查看当前值与默认值）。
  - 底部统一导航栏（上页 / 返回 / 刷新 / 关闭 / 首页 / 下页）；破坏性操作默认二次确认（`gui.admin.confirm-destructive`）；点击/成功/错误音效可配置。

*工作流增强*

- **四级优先级与 SLA**（`reports.priority`，默认启用）：
  - 优先级 `urgent` / `high` / `normal` / `low`，处理时限默认 2 / 6 / 24 / 72 小时（`sla-hours`，0 表示不追踪）。
  - 分类默认严重度（`severity`）作为初始优先级；同一目标未处理举报达到 `repeat-offender-threshold`（默认 3）时自动提升至 `repeat-offender-level`（默认 `high`）。
  - 超时检查任务每 `overdue-check-minutes`（默认 15）分钟异步运行，超期举报对在线管理员每条仅提醒一次（`sla-alerted-at` 去重）。
- **举报回复串**（`reports.replies`，默认启用）：玩家与管理员在同一举报下交错回复；单条最长 300 字符、每条举报最多保留 50 条；新回复可通知对方（`notify`）；玩家侧独立开关（`allow-player-replies`）；玩家打开详情即标记已读（`reporter-read-at`）。
- **关注列表**（`WatchlistService`，`notifications.watchlist` 默认启用）：`/reports watch <玩家>` 关注目标，每管理员默认上限 20（`max-targets`）；被关注玩家收到新举报时立即站内提醒关注者（`notify-on-report`）；列表持久化为 `watchlist.json`，启动时异步加载、按目标名去重并按上限裁剪。
- **离线举报人通知队列**（`PendingNoticeService`）：举报人离线期间的状态变更（认领 / 关闭 / 重开 / 新回复）暂存为待办通知，每人最多 20 条（`max-queued`）、默认保留 30 天（`queue-retention-days`），超出丢弃最旧；上线时由 `PlayerSessionListener` 触发补发。

*通知与联动*

- **管理员与举报人通知**：新举报通知在线管理员（`reportdesk.notify` 权限 + 可选音符音效）；认领/关闭/重开/回复同步通知举报人（在线直发、离线入队）；可选全服广播（默认关闭）。
- **Discord Webhook 推送**（`notifications.webhook`，默认关闭）：
  - `created` / `claimed` / `closed` 事件独立开关；JDK `HttpClient` 异步 POST（连接 10 秒、请求 5 秒），失败仅记录日志、不阻塞任何流程。
  - 载荷为标准 Discord embed（`username` / `avatar_url` / `content` + `embeds[title, color, fields]`），各字段按 Discord 上限截断并追加省略号：用户名 80、正文 1900、标题 256、字段名 256、字段值 1024。
- **联动处罚**（`PunishmentService`，默认关闭）：管理员结案后按裁决执行预设命令列表——`RESOLVED` → `resolved-commands`，`REJECTED` / `INSUFFICIENT` → `rejected-commands`；占位符 `%player%` `%reason%` `%id%` `%staff%` `%verdict%` `%category%`（换行/制表符归一为空格，防命令拼接）；每条命令执行均写审计日志；关闭状态下入口仅一次布尔判断即返回。

*证据与取证*

- **证据采集增强层**（`EvidenceService`，总开关 `evidence.enabled` 默认 `false`）：
  - 提交举报时采集在线目标快照，存为 `data/evidence/evidence-<id>.json`（带 schema 版本号）：世界与坐标（含朝向 yaw/pitch）、游戏模式、生命/饥饿/经验、Ping、物品栏（最多 41 槽）与盔甲、最近聊天（20 条 × 256 字符）；各子项独立开关（`capture.on-report.*`）。
  - 方块破坏采样：`BlockBreakEvent`（仅 `block-break.enabled=true` 时注册），内存环形队列每玩家默认上限 100 条，事件内零 IO。
  - 移动轨迹采样：`PlayerMoveEvent`（仅 `movement.enabled=true` 时注册，默认关闭；监听器第一行判断开关、关闭立即返回），最小间隔 20 tick、每玩家默认最多 60 个样本，结案后可在 GUI 查看。
  - 自动检测（`auto-detect`，默认关闭）：异步任务每 5 秒检查一次，统计 10 秒窗口内方块破坏达 40 次即触发，同目标 300 秒冷却；可生成 `SYSTEM` 系统举报或仅通知管理员；只报告、不处罚。
  - 拥有 `reportdesk.bypass.evidence` 权限的玩家不被任何采样。

*审计、导出与配置*

- **异步审计日志**（`AuditStorage`）：全部管理操作（`CLAIM` / `RELEASE` / `CLOSE` / `REOPEN` / `DELETE` / `NOTE` / `REPLY` / `PLAYER_REPLY` / `REPORT_CREATE` / `SYSTEM_REPORT` / `PUNISH` / `AUTO_CLOSE` / `RETENTION_PURGE`）以追加方式写入按天分文件的 `audit/audit-YYYY-MM-DD.log`，独立 `ReportDesk-Audit` 单线程串行保证行序；默认保留 180 天并由维护任务自动清理；`/reports audit [页码]` 与 GUI 按最新优先分页（默认每页 10 条）。
- **CSV 导出**：`/reports export` 在存储线程生成 `data/export/reports-YYYYMMDD-HHmmss.csv`；列：`id, created_at, status, category, target, reporter, claimed_by, closed_by, verdict, detail, notes`（备注计数）；RFC 4180 引号转义；文件带 UTF-8 BOM，Excel 打开中文不乱码。
- **游戏内配置编辑器**（`ConfigEditService`，`config-editor` 默认启用）：
  - `/reports config list` 列出全部可编辑键、`get` 查看当前值、`set` 修改、`reset` 恢复默认；修改前复用 `ConfigLoader` 完整校验——会触发截断、替换或新警告的值直接拒绝、不落盘。
  - 写回时先自动备份 `config.yml`（默认最多 5 份），再在存储线程原子写入；失败自动回滚旧配置并提示；单值长度上限 256 字符。
  - `config-editor.enabled=false` 时 `set` / `reset` 被拒绝，`list` / `get` 仍可用；GUI 设置页可按「区段 → 键 → 值」三级浏览。

*本地化与数据模型*

- **双语 i18n**：随附 `lang/zh_CN.yml` 与 `lang/en_US.yml`（各约 940 个键，`MessagesTest` 保证两文件键完整性一致）；`language` 配置切换；主文件缺失键自动回退 `en_US`；合并顺序为内置默认 < 服务器覆盖文件 < 语言文件，旧版本拷出的语言文件升级后仍可渲染全部文案。
- **举报数据模型**（`Report`，28 字段）：举报人/目标（UUID + 名称）、分类、详情、状态、创建/更新时间、认领人/时间、关闭人/时间、裁决、关闭备注、管理备注（每条举报最多 20 条）、证据元数据、系统生成标记、服务器名、优先级、SLA 截止/提醒时间、回复串（最多 50 条）、玩家已读时间、聊天勾选列表；状态机为 `OPEN` / `CLAIMED`（未处理）→ `RESOLVED` / `REJECTED` / `DUPLICATE` / `AUTO_CLOSED`（终态）。

#### 存储与性能（首次引入的设计）

- **主线程零存储 IO**：举报记录、证据、待办通知、关注列表、配置文本的读写全部运行在单线程执行器 `ReportDesk-Storage`；审计写入运行在独立的 `ReportDesk-Audit` 线程；主线程的 GUI 渲染与命令处理只读内存 `ReportRepository`，打开 GUI、提交举报与受理操作均不因存储 IO 阻塞。
- **WAL 写前日志 + 周期压缩**：单条举报的保存/删除只向 `reports.log` 追加一行紧凑 JSON（高频操作 O(1)），累计 50 条（`compact-after-entries`）或距上次压缩达 360 分钟（`compact-interval-minutes`）时自动合并进 `reports.json` 并清空日志；插件关闭时强制最终压缩（默认等待 5 秒），超时未完成的部分保留在日志中，下次启动与快照合并重放，崩溃后不丢失已确认的写入。
- **原子写入与备份轮转**：`reports.json`、证据文件与 `config.yml` 一律经临时文件 + 原子 move 写入（文件系统不支持时降级为普通 move）；`reports.json` 保存前按 60 分钟间隔备份至 `backups/`（默认最多保留 5 份）。
- **schema 版本号**：报告文件标记 v2，旧 v1 快照可直接读入，为后续版本的数据迁移框架（`ROADMAP` P0 #1）预留。
- **低频维护任务**（默认每 30 分钟、异步）：自动关闭超过 14 天未处理的举报（裁决 `AUTO_CLOSED`，操作人 `SYSTEM`）、清除超过 90 天保留期的已结案举报及其证据文件、按保留期清理证据（30 天）与审计文件（180 天）；全部清理动作的数量写入审计日志。
- **高频事件隔离**：基础举报流程只注册 `PlayerJoin` / `PlayerQuit`、聊天输入与 GUI 点击；`BlockBreakEvent`、`PlayerMoveEvent` 仅属于证据增强层且默认不注册，监听器入口一次开关判断即返回，事件内只做轻量采样入队，无文件/数据库 IO 与大范围统计。
- **聊天输入隔离**（`ChatInputService`）：每玩家至多一个挂起输入（举报详情、搜索关键字、管理备注共用），超时任务只操作内存 map，不触碰存储。
- **限流器可测试化**（`RateLimiter`）：注入 `Clock` 与 `ZoneId`，冷却、每日计数、同目标间隔、重复窗口、未处理上限的规则判定全部确定性可单测，判定过程零 IO。
- **阈值全部配置驱动**：举报冷却、每日上限、同目标重复间隔、重复窗口、同目标未处理上限、账号注册时长、分页大小、自动关闭天数、记录保留、维护间隔、SLA 时限、通知开关、证据各子开关与保留时长、审计保留、输入长度与限流间隔等全部由 `config.yml` 读取，缺失或非法值自动回退默认值并在控制台输出警告，代码路径中不存在业务魔法数。

#### 配置

随附带中文注释的完整默认 `config.yml`（约 344 行），各命名空间默认值如下（均可修改，缺失或非法值自动回退并警告）：

- `storage`：数据目录 `data`；保存前备份开启（最多 5 份、间隔 60 分钟）；WAL 压缩条数 50、时间间隔 360 分钟；关机等待异步写入完成 5 秒；数据目录变更需重启服务器。
- `limits`：命令冷却 1000 毫秒；举报冷却 60 秒；每日上限 10 条；同目标重复间隔 300 秒；重复窗口 600 秒；同目标未处理上限 3 条；账号最低注册时长 0 分钟（关闭）；禁止举报自己；允许举报离线目标；强制目标在线关闭；`reportdesk.bypass.limits` 可绕过全部限制。
- `reports`：文本页 10 条、GUI 页 28 条；自动关闭开启（14 天）；已结案保留 90 天；维护间隔 30 分钟；统计面板 Top 目标 3 名 / Top 管理员 5 名；默认分类 `other`。
- `reports.priority`：启用；重复违规阈值 3 条提升至 `high`；超时检查间隔 15 分钟、提醒开启；SLA 时限 `urgent=2` / `high=6` / `normal=24` / `low=72` 小时。
- `reports.replies`：启用；玩家可回复；单条 300 字符、每条举报 50 条；新回复通知开启。
- `reports.chat-report`：默认关闭；触发分类 `chat`；单次最多勾选 5 条。
- `reports.categories`：7 个默认分类（`cheating` / `grief` 为 `high`，`chat` / `advertising` / `bug` 为 `normal`，`inappropriate` / `other` 为 `low`），各含 `display` / `icon` / `aliases` / `enabled` / `priority` / `severity` / `description`。
- `gui`：GUI 与点击/成功/错误音效启用；空白槽填充；操作后不自动关闭；管理台启用且破坏性操作需二次确认。
- `notifications`：管理员新举报通知（`reportdesk.notify` 权限 + `block.note_block.pling` 音效）；举报人通知离线排队（每人最多 20 条、保留 30 天）；关注列表启用（每管理员 20 目标、新举报即时提醒）；全服广播与 Webhook 默认关闭。
- `evidence`：总开关默认关闭；`capture-on-report` 开启；证据保留 30 天；聊天 20 条 × 256 字符；采集子开关（位置 / 游戏模式 / 生命值 / 物品栏 41 槽 / 显示名 80 字符 / 聊天 / Ping）；方块破坏、移动轨迹、自动检测默认关闭（自动检测参数：检查 5 秒、窗口 10 秒、阈值 40 次、冷却 300 秒、生成系统举报并通知管理员）。
- `punishments`：默认关闭，`resolved-commands` / `rejected-commands` 为空列表。
- `audit`：启用；保留 180 天；每页 10 条。
- `config-editor`：启用；写前备份（最多 5 份）；单值长度上限 256。
- `security`：管理操作限流 300 毫秒；聊天输入等待 120 秒；详情 2–200 字符；备注上限 500 字符、每条举报最多 20 条备注；详情非必填；默认禁止 § 颜色码；玩家名正则 `^[A-Za-z0-9_]{3,16}$`；违禁词列表为空（可扩展）。

#### 安全

- **玩家侧限流**（`/report` 提交管线）：命令级防刷冷却 1000 毫秒（可配 0 关闭）；举报冷却 60 秒；每日 10 条上限；同目标 300 秒重复间隔；同举报人+目标 600 秒重复窗口；同目标未处理 3 条上限；账号注册时长阈值；禁止举报自己；全部限制可被 `reportdesk.bypass.limits` 权限绕过。
- **管理侧限流**：`/reports` 管理操作对每位操作者按 300 毫秒节流（`security.admin-cooldown-millis`），防止 GUI 连点与脚本式批量操作。
- **输入校验**：玩家名按可配正则校验（`security.player-name-pattern`）；举报详情、管理员备注、回复串执行空值与长度校验；控制字符与 § 颜色码默认拒绝（`security.allow-color-codes`）；`blocked-words` 违禁词表不区分大小写过滤；搜索关键字同样受限。
- **权限体系**：注册 10 个权限节点——`reportdesk.report`（默认所有人）、`reportdesk.admin` / `admin.close` / `admin.delete` / `notify` / `watch` / `evidence` / `reload` / `config`（默认 op）、`reportdesk.bypass.limits` / `bypass.evidence`（默认 false）；无对应权限的管理操作一律拒绝并提示。
- **审计覆盖**：全部管理操作与自动维护动作异步写审计日志，按天分文件、按保留期自动清理，不阻塞主线程。
- **默认安全姿态**：Webhook、全服广播、自动检测、联动处罚、方块破坏与移动轨迹采样默认全部关闭；自动检测只生成系统举报或通知、不执行任何处罚；处罚仅在管理员结案后按裁决执行预设命令。
- **渲染防注入**：所有玩家输入在 MiniMessage 渲染前以 `Placeholder.unparsed` 注入，无法携带标签、颜色码或可点击组件；处罚命令占位符归一化换行与制表符，防止通过举报详情拼接命令。

#### 架构

- **纯核心层**：`core` 包（举报状态机、提交管线、限流、校验、过滤排序、统计）不依赖任何 Bukkit API，`RateLimiter` 注入 `Clock` 实现确定性单测；Bukkit 侧服务、存储、GUI、命令与监听器分层组装。
- **固定构建与测试**：构建命令固定为 `./gradlew test build`；随附 27 个测试类（配置加载/校验/标签、YAML 文档改写、限流器、输入校验、举报状态机与提交管线、过滤/排序/分页、WAL 回放与原子写、审计日志、CSV 导出、Webhook 载荷、i18n 键完整性、GUI 点击、插件描述符等），全部通过。
- **开发硬约束**：`AGENTS.md` 固化十项红线（`api-version` 锁定、阈值配置驱动、高频事件限制、增强层默认关闭、权限/限流/校验/审计、异步存储、固定构建命令、Git 备份流程、更新日志强制记录），作为后续所有版本的验收标准。

#### 文档与工程

- 新增 `AGENTS.md`：十项开发硬约束与「变更日志强制记录」规则（本文件的格式与提交顺序即其产物）。
- 新增 `README.md`：功能总览、指令与权限速查表、安装说明与支持版本说明；新增 `ROADMAP.md`：v1.1.0 的 P0（数据迁移框架、`Report` 字段扩展、已知缺陷清单）、P1（指派/转派、SLA 报表、双 Webhook、数据看板、审计防篡改）、P2（更多入口、富文本、轨迹回放、策略插件化、生态互通、批量操作、语言热加载）规划及「明确不做」边界（不引入数据库、不做网页面板、不做跨服同步、不自动处罚、不兼容 1.20 以下）。
- 新增 `docs/` 文档站（10 个文件、933 行：9 个章节 + 索引——总览 / 安装 / 指令 / 权限 / 配置 / 增强层 / i18n / 架构 / 开发指南），作为随仓库分发的完整使用与开发文档。
- 许可证改为 GNU GPLv3（`GPL-3.0-or-later`），随附 `LICENSE`；新增官方图标资产（`icon/reportdesk-icon.png` / `.svg`）。

#### 兼容性

- **服务端**：Paper 1.20 – 1.21.x；`paper-plugin.yml` 为权威描述符（`api-version: '1.20'`），另保留遗留 `plugin.yml` 供仍从该文件解析命令的旧服务器使用，并有运行时 CommandMap 兜底，别名 `rdreport` / `jb` / `reportdesk` / `rd` 均有效。
- **运行时**：Java 17+（产物为 Java 17 字节码；1.20 的默认 JDK 17 与 1.21 的 JDK 21 服务端均可直接加载）。
- **构建**：Gradle 9.6.1 Wrapper，固定命令 `./gradlew test build`；编译目标 Java 17、验证基于 Paper 1.21.11 API。
- **数据文件**（均位于插件数据目录，拷目录即可迁移）：`data/reports.json`（schema v2 + `reports.log` WAL）、`data/pending-notices.json`、`data/watchlist.json`、`data/evidence/evidence-<id>.json`、`data/export/*.csv`、`audit/audit-YYYY-MM-DD.log`、`backups/` 备份轮转目录。

---
