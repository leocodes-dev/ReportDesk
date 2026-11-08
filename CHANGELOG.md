# ReportDesk 更新日志

> 本文件按语义化版本记录 ReportDesk 的全部变更。格式遵循 `AGENTS.md` 第 10 节模板：
> 每条日志包含版本号、发布日期、中英文概述各一段，以及按分类的更新一览。

## Release 1.0.0

ReportDesk 1.0.0 是项目首个正式发布版本，完整实现了「玩家举报 → 管理员受理 → 结案归档」工作流：玩家端举报 GUI 与 `/report` 命令、管理端 `/reports` 工作台式受理流程（20 余个子命令与 54 格管理 GUI）、举报优先级与 SLA 超时提醒、举报回复串、关注列表与离线举报人补发队列、Discord Webhook 推送、可整体开关的证据采集增强层（方块破坏采样、移动轨迹、聊天记录、自动检测）与联动处罚（均默认关闭），以及游戏内配置编辑器。所有业务阈值集中于 `config.yml`，代码中无任何硬编码魔法数；举报记录、证据、审计日志全部在专用异步线程上持久化，主线程零文件 IO、零数据库 IO；所有增强功能默认 `false`，关闭路径入口仅一次开关判断即短路返回。本版本兼容 Paper 1.20–1.21.x（`api-version` 锁定为 `'1.20'`，Java 17 字节码），并随附 27 个测试类覆盖配置加载与校验、限流、输入校验、举报状态机、存储 WAL 回放、审计日志、CSV 导出、Webhook 载荷与 i18n 键完整性。

1.0.0 同时确立了工程基线：`AGENTS.md` 十项开发硬约束（`api-version` 锁定、阈值配置驱动、高频事件限制、权限/限流/输入校验、异步存储、固定构建命令、Git 备份流程、更新日志强制记录）、GPLv3 许可证、README 与 ROADMAP 文档，以及官方图标资产。

ReportDesk 1.0.0 is the first official release, delivering the complete "report → triage → closure" workflow: a player-facing report GUI and `/report` command, an admin `/reports` desk with 20+ subcommands and a 54-slot admin GUI, report priority with SLA overdue alerts, per-report reply threads, a staff watchlist with an offline reporter notice queue, Discord Webhook push, an opt-in evidence layer (block-break sampling, movement trails, chat capture, auto-detection) and opt-in linked punishments — all disabled by default — plus an in-game configuration editor. Every business threshold lives in `config.yml` with no hardcoded values in code; report, evidence and audit data are persisted on dedicated asynchronous threads, keeping the main thread free of file and database IO; every enhancement feature defaults to `false` and short-circuits on a single switch check when disabled. This release targets Paper 1.20–1.21.x (`api-version` pinned to `'1.20'`, Java 17 bytecode) and ships 27 test classes covering configuration loading and validation, rate limiting, input validation, the report state machine, storage WAL replay, audit logging, CSV export, Webhook payloads and i18n key parity.

The 1.0.0 release also establishes the project's engineering baseline: the ten hard constraints of `AGENTS.md` (pinned `api-version`, config-driven thresholds, high-frequency event restrictions, permission/rate-limit/input validation, asynchronous storage, a fixed build command, a mandatory git backup workflow and a mandatory changelog), GPLv3 licensing, README/ROADMAP documentation, and the official icon assets.

### 1.0.0 更新一览（2026-11-01）

**新增**
- 玩家端举报入口：`/report`（别名 `rdreport`、`jb`）+ 举报 GUI。选择被举报对象、分类与备注后提交即入队；GUI 关闭时 `/report <玩家>` 自动切换聊天输入模式（`ChatInputService`，等待超时由 `security.pending-timeout-seconds` 控制，默认 120 秒，`/report cancel` 可随时取消）。
- 玩家自助子命令：`/report status [页码]` 查看本人举报列表（分页，每页条数由 `reports.text-page-size` 控制，默认 10）、`/report view <id>` 查看单条详情、`/report reply <id> <内容>` 与 `/report replies <id>` 回复/查看回复串、`/report help`。
- 分类体系：内置 7 个默认分类（`cheating`、`chat`、`grief`、`advertising`、`bug`、`inappropriate`、`other`），每个分类可独立配置显示名、图标（Bukkit Material）、命令别名、启用开关、展示优先级与默认严重度，并支持在 `config.yml` 中增删；语言文件可本地化覆盖分类名与描述。
- 管理端 `/reports`（别名 `reportdesk`、`rd`）：`menu`/`gui`/`admin` 打开管理 GUI，`list [页码]`、`search <关键字>`、`view <id>`、`claim <id>`、`release <id>`、`close <id> <裁决> [备注]`、`reopen <id>`、`tp <玩家>`、`evidence <id>`、`note <id> <内容>`、`reply` / `replies`、`delete <id>`、`purge <天数>`、`overdue`、`watch` / `unwatch` / `watching`、`stats`、`export`、`audit [页码]`、`reload`、`config list/get/set/reset`；无参数时（GUI 与管理台均启用）直接打开管理 GUI。
- 管理 GUI：54 格工作台（首页概览、筛选分页列表、举报详情、认领/关闭、证据、审计、统计、设置），底部导航栏（上页/下页/返回/刷新/关闭/首页），破坏性操作（删除、清除等）默认需二次确认（`gui.admin.confirm-destructive`），操作音效可配置。
- 举报优先级与 SLA：`urgent`/`high`/`normal`/`low` 四级优先级，各级处理时限默认 2/6/24/72 小时（`reports.priority.sla-hours`）；同一目标未处理举报达到 `repeat-offender-threshold`（默认 3 条）时自动提升至 `repeat-offender-level`（默认 `high`）；超时检查任务每 `overdue-check-minutes`（默认 15 分钟）异步运行，超期举报对在线管理员每条仅提醒一次。
- 举报回复串：玩家与管理员可在同一举报下互发回复（`reports.replies`），单条最长 300 字符、每条举报最多保留 50 条，新回复可通知对方，玩家/管理员两侧可独立开关；玩家侧支持未读回复追踪（打开详情即标记已读）。
- 关注列表（`WatchlistService`）：`/reports watch <玩家>` 关注目标（每管理员默认上限 20，`notifications.watchlist.max-targets`），被关注玩家收到新举报时立即站内提醒关注者；列表持久化为 `watchlist.json`，启动时加载并去重、按上限裁剪。
- 离线举报人通知队列（`PendingNoticeService`）：举报人离线时的状态变更（认领/关闭/重开/回复）暂存为待办通知（每人最多 20 条、默认保留 30 天，超出丢弃最旧），上线时由 `PlayerSessionListener` 触发补发。
- Discord Webhook 推送（默认关闭）：`created`/`claimed`/`closed` 事件独立开关，JDK `HttpClient` 异步 POST（5 秒连接超时、5 秒请求超时），支持自定义用户名与头像，发送失败仅记录日志、不阻塞任何流程。
- 联动处罚（`PunishmentService`，默认关闭）：管理员结案后按裁决结果执行 `punishments.resolved-commands`（成立）或 `rejected-commands`（不成立/证据不足）预设命令，占位符 `%player%` `%reason%` `%id%` `%staff%` `%verdict%` `%category%`；每条命令执行均写入审计日志；关闭状态下仅一次布尔判断即返回，不执行任何命令。
- 证据采集增强层（`EvidenceService`，总开关 `evidence.enabled` 默认 `false`）：提交举报时采集在线目标快照（位置、游戏模式、生命值、物品栏与装备最多 41 槽、最近聊天 20 条 × 256 字符、Ping，各子项独立开关）；方块破坏样本（内存环形队列，每玩家默认上限 100 条，仅 `evidence.block-break.enabled=true` 时注册 `BlockBreakEvent`）；移动轨迹采样（每玩家默认 60 个样本、最小间隔 20 tick，仅 `evidence.movement.enabled=true` 时注册 `PlayerMoveEvent`，监听器首行判断开关、关闭立即返回）；自动检测（默认关闭：统计窗口 10 秒内方块破坏达 40 次即触发，同目标 300 秒冷却，可生成系统举报（`SYSTEM` 举报人）或仅通知管理员，只报告、不处罚）；拥有 `reportdesk.bypass.evidence` 权限的玩家不被采样。
- 聊天记录多选举报（`reports.chat-report`，默认关闭）：需同时启用证据层与聊天日志采集后，在举报 GUI 中勾选目标最近的至多 5 条聊天消息作为证据随单提交。
- 异步审计日志（`AuditStorage`）：管理操作（`CLAIM`/`RELEASE`/`CLOSE`/`REOPEN`/`DELETE`/`NOTE`/`REPLY`/`PLAYER_REPLY`/`AUTO_CLOSE`/`RETENTION_PURGE`/`PUNISH`/`SYSTEM_REPORT`/`REPORT_CREATE`）全部以追加方式异步写入按天分文件的 `audit/YYYY-MM-DD`，默认保留 180 天并由维护任务自动清理；`/reports audit [页码]` 与 GUI 均支持分页查看（默认每页 10 条，按最新优先）。
- CSV 导出：`/reports export` 在数据目录 `export/` 下生成 `reports-YYYYMMDD-HHmmss.csv`（带 UTF-8 BOM，兼容 Excel 中文），导出全程在存储线程执行，不阻塞主线程。
- 游戏内配置编辑器（`ConfigEditService`，`/reports config`）：`list` 列出全部可编辑键、`get` 查看当前值、`set` 修改、`reset` 恢复默认；修改前复用 `ConfigLoader` 做完整校验——会触发截断、替换或新警告的值直接拒绝、不落盘；写回时先自动备份 `config.yml`（默认最多保留 5 份），再在存储线程原子写入，失败自动回滚旧配置并提示；`config-editor.enabled=false` 时 `list`/`get` 仍可用、`set`/`reset` 被拒绝。
- 双语支持（`i18n`）：随附 `lang/zh_CN.yml` 与 `lang/en_US.yml`（各约 1240 行），`language` 配置切换，主文件缺失时自动回退 `en_US`；全部文案经 MiniMessage 渲染，玩家输入类占位符统一通过 `Placeholder.unparsed` 注入。

**优化**
- 业务阈值配置驱动：举报冷却、每日上限、同目标重复间隔、重复窗口、同目标未处理上限、分页大小、自动关闭天数、记录保留天数、维护间隔、通知开关、证据各子开关与保留时长、审计保留、输入长度与限流间隔等全部由 `config.yml` 读取，缺失或非法值自动回退默认值并在控制台输出警告，代码路径中不存在业务魔法数。
- 主线程零存储 IO：举报记录、证据、待办通知、关注列表、配置文本的读写全部运行在单线程执行器 `ReportDesk-Storage` 上，审计写入运行在独立的 `ReportDesk-Audit` 线程；主线程的 GUI 渲染与命令处理只读内存 `ReportRepository`，打开 GUI、提交举报与受理操作均不因存储 IO 阻塞。
- WAL + 周期压缩：单条举报的保存/删除只向 `reports.log` 追加一行紧凑 JSON（高频操作 O(1)），累计 `compact-after-entries`（默认 50）条或距上次压缩达 `compact-interval-minutes`（默认 360 分钟）时自动合并进 `reports.json` 并清空日志；插件关闭时强制最终压缩（`ReportService.flush`），超时未完成的部分保留在日志中，下次启动与快照合并重放，崩溃后不丢失已确认的写入。
- 原子写入与备份轮转：`reports.json`、证据文件与 `config.yml` 一律经临时文件 + 原子 move 写入（文件系统不支持时降级为普通 move）；`reports.json` 保存前按 60 分钟间隔备份至 `backups/`（默认最多保留 5 份）。
- 报告文件带 schema 版本号（当前 v2），旧 v1 快照可直接读入，为后续版本的数据迁移预留框架。
- 低频维护任务（默认每 30 分钟、异步执行）：自动关闭超过 14 天未处理的举报（裁决 `AUTO_CLOSED`）、清除超过 90 天保留期的已结案举报及其证据文件、按保留期清理证据（30 天）与审计文件（180 天）；全部清理动作的数量写入审计日志。
- 高频事件隔离：基础举报流程只注册 `PlayerJoin`/`PlayerQuit`、聊天输入与 GUI 点击；`BlockBreakEvent`、`PlayerMoveEvent` 仅属于证据增强层且默认不注册，监听器入口一次开关判断即返回，事件内只做轻量采样入队，无文件/数据库 IO 与大范围统计。
- 限流器可测试化：`RateLimiter` 注入 `Clock` 与 `ZoneId`，冷却、每日计数、同目标间隔、重复窗口、未处理上限的规则判定全部确定性可单测，判定过程零 IO。
- i18n 防注入：玩家可控文本（举报详情、回复、备注、搜索关键字）在渲染时均以未解析占位符注入，无法通过举报内容注入 MiniMessage 标签、颜色码或可点击组件。

**修复**
- 本项目首个发布版本，无修复类条目；已知小问题（`/reports list` 筛选在带空格关键字下的解析、GUI 证据页快照缺失兜底、Webhook 失败缺少重试告警、`server-name` 改名后审计文件名的边角情况、`YamlDocument` 带引号键替换偶发破坏注释）已列入 `ROADMAP.md` 的 v1.1.0 P0 缺陷清单，将配回归测试在下个版本修复。

**配置**
- 随附带中文注释的完整默认 `config.yml`，各命名空间默认值如下（均可改，非法值自动回退）：
  - `storage`：数据目录 `data`；保存前备份开启、最多 5 份、间隔 60 分钟；WAL 压缩条数 50、时间间隔 360 分钟；关机等待异步写入完成 5 秒；数据目录变更需重启。
  - `limits`：命令冷却 1000 毫秒；举报冷却 60 秒；每日上限 10 条；同目标重复间隔 300 秒；重复窗口 600 秒；同目标未处理上限 3 条；账号最低注册时长 0 分钟（关闭）；禁止举报自己；允许举报离线目标；强制目标在线关闭；`reportdesk.bypass.limits` 可绕过全部限制。
  - `reports`：文本页 10 条、GUI 页 28 条；自动关闭开启（14 天）；保留 90 天；维护间隔 30 分钟；统计面板 Top 目标 3 名 / Top 管理员 5 名；默认分类 `other`。
  - `reports.priority`：启用；重复违规阈值 3 条提升至 `high`；超时检查间隔 15 分钟、提醒开启；SLA 时限 `urgent=2`/`high=6`/`normal=24`/`low=72` 小时。
  - `reports.replies`：启用；玩家可回复；单条 300 字符、每条举报 50 条；新回复通知开启。
  - `reports.chat-report`：默认关闭；触发分类 `chat`；单次最多勾选 5 条。
  - `gui`：GUI 与音效启用；空白槽填充；操作后不自动关闭；管理台启用且破坏性操作需二次确认。
  - `notifications`：管理员新举报通知（`reportdesk.notify` + 音符音效）；举报人通知离线排队（最多 20 条、保留 30 天）；关注列表启用（每管理员 20 目标、新举报即时提醒）；全服广播与 Webhook 默认关闭。
  - `evidence`：总开关默认关闭；`capture-on-report` 开启；证据保留 30 天；聊天 20 条 × 256 字符；采集子开关（位置/游戏模式/生命值/物品栏 41 槽/显示名 80 字符/聊天/Ping）；方块破坏与移动轨迹、自动检测默认关闭（自动检测参数：检查 5 秒、窗口 10 秒、阈值 40 次、冷却 300 秒、生成系统举报并通知管理员）。
  - `punishments`：默认关闭，成立/不成立命令列表为空。
  - `audit`：启用；保留 180 天；每页 10 条。
  - `config-editor`：启用；写前备份（最多 5 份）；单值长度上限 256。
  - `security`：管理操作限流 300 毫秒；聊天输入等待 120 秒；详情 2–200 字符；备注上限 500 字符、每举报最多 20 条备注；详情非必填；默认禁止 § 颜色码；玩家名正则 `^[A-Za-z0-9_]{3,16}$`；违禁词列表为空（可扩展）。

**安全**
- 玩家侧限流：`/report` 命令级防刷冷却 1000 毫秒（可配 0 关闭）；举报冷却 60 秒；每日 10 条上限；同目标 300 秒重复间隔；同举报人+目标 600 秒重复窗口；同目标未处理 3 条上限；账号注册时长阈值；禁止举报自己；全部限制可被 `reportdesk.bypass.limits` 权限绕过。
- 管理侧限流：`/reports` 管理操作对每位操作者按 300 毫秒节流（`security.admin-cooldown-millis`），防止 GUI 连点与脚本式批量操作。
- 输入校验：玩家名按可配正则校验；举报详情、管理员备注、回复串执行长度/空值校验；控制字符与 § 颜色码默认拒绝（`security.allow-color-codes`）；`blocked-words` 违禁词表不区分大小写过滤；搜索关键字同样受限。
- 权限体系：注册 10 个权限节点——`reportdesk.report`（默认所有人）、`reportdesk.admin` / `admin.close` / `admin.delete` / `notify` / `watch` / `evidence` / `reload` / `config`（默认 op）、`reportdesk.bypass.limits` / `bypass.evidence`（默认 false）；无对应权限的管理操作一律拒绝并提示。
- 审计覆盖：全部管理操作与自动维护动作异步写审计日志，按天分文件、按保留期自动清理，不阻塞主线程。
- 默认安全姿态：Webhook、全服广播、自动检测、联动处罚、方块破坏与移动轨迹采样默认全部关闭；自动检测只生成系统举报/通知、不执行任何处罚；处罚仅在管理员结案后按裁决执行预设命令。
- 渲染防注入：所有玩家输入在 MiniMessage 渲染前以 `Placeholder.unparsed` 注入，无法携带标签或格式码。

**文档**
- 新增 `AGENTS.md` 十项开发硬约束：`api-version` 锁定 `'1.20'`；业务阈值全部配置驱动、禁止硬编码；基础功能禁止注册高频事件；`PlayerMoveEvent` 默认不注册；增强功能默认 `false` 且关闭路径一次短路；权限/限流/输入校验/异步审计；存储异步化；固定构建命令 `./gradlew test build`；开发前/后/完成时 Git 提交备份流程；更新日志强制记录（本文件）及其模板。
- 新增 `README.md`：功能总览、指令与权限速查表、安装说明与支持版本说明；新增 `ROADMAP.md`：v1.1.0 的 P0（数据迁移框架、`Report` 字段扩展、已知缺陷清单）、P1（指派/转派、SLA 报表、双 Webhook、数据看板、审计防篡改）、P2（更多入口、富文本、轨迹回放、策略插件化、生态互通、批量操作、语言热加载）规划及「明确不做」边界（不引入数据库、不做网页面板、不做跨服同步、不自动处罚、不兼容 1.20 以下）。
- 许可证改为 GNU GPLv3（`GPL-3.0-or-later`），随附 `LICENSE`。
- 双语资源 `lang/zh_CN.yml`、`lang/en_US.yml` 随包内置，`MessagesTest` 校验键完整性；分类名/描述支持语言覆盖 `config.yml` 的 `display` 值。

**兼容性**
- 服务端：Paper 1.20 – 1.21.x；`paper-plugin.yml` 为权威描述符（`api-version: '1.20'`），另保留遗留 `plugin.yml` 供仍从该文件解析命令的旧服务器使用，并有运行时 CommandMap 兜底，别名 `rdreport`/`jb`/`reportdesk`/`rd` 均有效。
- 运行时：Java 17+（产物为 Java 17 字节码；1.20 的默认 JDK 17 与 1.21 的 JDK 21 服务端均可直接加载）。
- 构建与测试：Gradle Wrapper，固定命令 `./gradlew test build`；随附 27 个测试类（配置加载/校验/标签、YAML 文档、限流器、输入校验、举报状态机与提交管线、过滤/排序/分页、WAL 回放与原子写、审计日志、CSV 导出、Webhook 载荷、i18n、GUI 点击、插件描述符等）全部通过。
- 数据文件：`data/reports.json`（schema v2 + WAL）、`data/pending-notices.json`、`data/watchlist.json`、`data/evidence/evidence-<id>.json`、`audit/` 日志与 `backups/` 备份目录均位于插件数据目录内，拷目录即可迁移。
