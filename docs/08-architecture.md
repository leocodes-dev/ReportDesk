# 08 · 架构与数据流

## 模块划分

```text
com.reportdesk
├── ReportDeskPlugin.java        入口：生命周期、服务装配、categoryDisplay()
├── command/                     命令层（薄）
│   ├── ReportCommand.java       /report 玩家侧
│   ├── ReportsCommand.java      /reports 管理侧（30+ 子命令）
│   └── CommandRegistrar.java    注册 + Tab 补全
├── config/
│   ├── ConfigLoader.java        读 config.yml → PluginConfig record 树
│   ├── ConfigKey(s).java        可在线编辑的配置项注册表
│   ├── ConfigEditService*       （service/）游戏内改 config.yml
│   └── YamlDocument.java        逐行替换/插入，保留注释
├── core/                        纯逻辑（无 Bukkit 依赖，全部可单测）
│   ├── Report / ReportCategory / Verdict / ReportStatus
│   ├── ReportRepository         ConcurrentHashMap + AtomicLong 发号
│   ├── ReportSubmissionService  提交校验（纯核心，无 I/O）
│   ├── RateLimiter / InputValidator / PriorityPolicy
│   ├── CategoryRegistry         分类 + 别名索引
│   └── AuditEntry / Evidence / MoveSample / ChatSample ...
├── service/                     胶水层（Bukkit 交互）
│   ├── ReportService            提交/认领/结案/重开/删除
│   ├── ReportViewService        列表/详情/统计渲染
│   ├── EvidenceService          快照采集 + 有界队列
│   ├── AutoDetectService        阈值检测（ScheduledTask）
│   ├── PunishmentService        结案后 dispatch 命令
│   ├── NotificationService      站内/离线队列/关注/Webhook 编排
│   ├── WebhookService           异步 HTTP（10s 超时，非阻塞）
│   ├── WatchlistService / PendingNoticeService / MaintenanceService
│   └── ConfigEditService        原子写 config.yml（失败自动 revert）
├── storage/
│   ├── FileStorage              reports.json + WAL（reports.log）
│   ├── AuditStorage             审计按天分文件（异步追加）
│   └── CsvExporter              导出
├── gui/
│   ├── GuiService               玩家举报 GUI（选玩家/分类/确认）
│   ├── AdminGuiService          管理 GUI（列表/详情/结案/证据）
│   └── ConfigGuiSupport         配置编辑 GUI
├── listener/
│   ├── PlayerSessionListener    登录/登出（暂存通知补发、清会话）
│   ├── ChatInputListener        聊天输入模式（举报详情）
│   └── 增强层（默认不注册）
│       ├── ChatEvidenceListener
│       ├── BlockBreakEvidenceListener   首行判 evidence 总开关
│       └── MovementEvidenceListener     首行判开关，false 立即 return
├── i18n/
│   ├── Messages                 三层合并 + MiniMessage 渲染 + 占位符 unparsed
│   └── MessageKeys              900+ key 常量（单一来源）
└── webhook/                     WebhookPayload（Gson 序列化）
```

## 线程模型

| 线程 | 职责 | 约束 |
|---|---|---|
| 主线程（Bukkit） | 命令/GUI/事件 | **零文件 IO、零数据库 IO** |
| 存储线程池（单线程） | `reports.json`/WAL/审计/证据落盘 | 串行，避免写冲突 |
| 维护任务 | 自动关闭、SLA 检查、清理过期数据 | `reports.maintenance-interval-minutes` 周期 |
| Webhook | `sendAsync`，10s 超时 | 不阻塞任何关键路径 |

**关键不变量**：

1. 主线程任何代码路径**不**出现 `File.write` / 阻塞存储调用；
2. 增强层高频事件（`BlockBreakEvent`、`PlayerMoveEvent`）**默认不注册**；注册后监听器第一行判开关，false 立即 return；
3. 每条举报写入：先追加 `reports.log`（WAL，一行 JSON），累计 `compact-after-entries` 条或到达 `compact-interval-minutes` 才全量重写 `reports.json`；
4. 关服时 `shutdown-timeout-seconds` 内等待异步写完成。

## 数据流（一次举报的完整生命周期）

```text
玩家 /report
  → ReportCommand（命令层，校验权限/冷却/输入）
  → ReportService.submit（胶水层，组装 SubmitRequest）
  → ReportSubmissionService.submit（纯核心，校验 + 构造 Report，无 I/O）
  → 成功：
       ├ 写 WAL（异步追加 reports.log 一行）
       ├ 触发通知（NotificationService：在线管理员 / 离线队列 / 关注 / Webhook）
       ├ 增强层：异步采集证据快照（若开）
       └ 记录审计（异步）
  → 失败：
       返回 (reason, placeholders)，由命令/GUI 层用本地化 key 渲染错误
```

## 存储格式

- **`reports.json`**：`Map<id, Report>` 全量（周期压缩产物）；
- **`reports.log`**：每行一条 JSON（`{"op":"put|update|delete","report":{...}}`），启动时回放 + 崩溃恢复；
- **`evidence/`**：按举报 id 一个文件（快照 + 采样）；
- **`audit/audit-YYYY-MM-DD.log`**：按天分文件；
- **`pending-notices.json`**：离线举报人暂存。

## 不变量与防御

- **`Report` record 20 字段**，`copy()` 逐字段构造；改字段必须全链路同步；
- **`YamlDocument`**：写入前 `verify` 解析后的值与预期一致，失败不写；
- **`ConfigEditService`**：`AtomicBoolean` 防并发写，写失败自动 `revert` 到备份；
- **`FileStorage`**：WAL + 压缩 + 备份三重兜底；
- **权限**：`ReportsCommand.onCommand` 首行校验 `reportdesk.admin`，各子命令再 `require(sender, perm)`（29 处）；
- **输入**：`InputValidator` 统一校验玩家名（正则）、详情长度/禁词、非法字符；

## 测试

- **184 单测**（纯 JVM，无 Bukkit 依赖），`./gradlew test` 全绿；
- 重点覆盖：`Messages`（三层合并 + key 全量）、`ReportRepository`、`ReportSubmissionService`、`FileStorage`（WAL 回放）、`YamlDocument`（注释保留）、`RateLimiter`、`Report`（copy 全字段）；
- 验收红线：`PluginDescriptorTest` 断言 `paper-plugin.yml` 的 `api-version: '1.20'`。

## 构建

- Gradle 9.6.1 + Java 17 字节码；
- 无运行时依赖（Gson / Adventure / MiniMessage 随 Paper 提供）；
- 产物 `build/libs/ReportDesk-1.0.0.jar`；
- 代码风格：google-java-format 1.19.1（2 空格 google 风格）。
