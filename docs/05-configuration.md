# 05 · 配置参考（config.yml）

全部业务阈值都集中在 `config.yml`，代码不硬编码。缺失或非法的值会回退默认并在控制台告警。除 `storage.directory` 需重启外，其余均可 `/reports reload` 热加载。

## 根

| 键 | 默认 | 说明 |
|---|---|---|
| `server-name` | `'ReportDesk Server'` | 仅用于展示、审计、Webhook |
| `language` | `zh_CN` | `zh_CN` / `en_US`，决定加载哪个 `lang/<language>.yml` |

## storage

| 键 | 默认 | 说明 |
|---|---|---|
| `directory` | `data` | 数据目录（相对插件目录），**改后需重启** |
| `backup-on-save` | `true` | 每次压缩前备份 `reports.json` |
| `max-backups` | `5` | 备份保留份数 |
| `backup-interval-minutes` | `60` | 两次备份最小间隔 |
| `shutdown-timeout-seconds` | `5` | 关服等待异步写完成的上限 |
| `compact-after-entries` | `50` | WAL 累计 N 条后压缩进 `reports.json` |
| `compact-interval-minutes` | `360` | 两次压缩最大间隔；`0` 表示只按条数 |

## limits

| 键 | 默认 | 说明 |
|---|---|---|
| `command-cooldown-millis` | `1000` | `/report` 命令层最小间隔 |
| `cooldown-seconds` | `60` | 举报冷却；`0` 关闭 |
| `daily-limit` | `10` | 每日举报上限；`0` 不限 |
| `same-target-interval-seconds` | `300` | 对同一目标的最小间隔；`0` 关闭 |
| `duplicate-window-seconds` | `600` | 同举报人+目标+分类的重复窗口；`0` 关闭 |
| `max-open-per-target` | `3` | 同一目标最多未处理数；`0` 不限 |
| `min-account-age-minutes` | `0` | 举报人最低账号年龄；`0` 关闭 |
| `block-self-report` | `true` | 禁止举报自己 |
| `allow-offline-target` | `true` | 允许举报离线玩家 |
| `require-target-online` | `false` | 强制目标在线（优先于上一条） |
| `bypass-permission` | `reportdesk.bypass.limits` | 绕过限流所需权限节点 |

## reports

| 键 | 默认 | 说明 |
|---|---|---|
| `text-page-size` | `10` | 文字列表每页条数 |
| `gui-page-size` | `28` | 管理 GUI 每页条数（≤28） |
| `auto-close-enabled` | `true` | 超时未处理自动结案 |
| `auto-close-days` | `14` | 自动结案天数 |
| `retention-days` | `90` | 已结案举报保留天数；`0` 永久 |
| `maintenance-interval-minutes` | `30` | 后台维护任务间隔 |
| `stats-top-targets` | `3` | 统计展示被举报最多目标数 |
| `staff-stats-top` | `5` | 统计展示处理量最高管理员数 |
| `default-category` | `other` | 缺省分类 ID |

### reports.priority（优先级与 SLA）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 关闭后所有举报用分类默认优先级 |
| `repeat-offender-threshold` | `3` | 目标未处理数达到 N 时提升优先级；`0` 关闭 |
| `repeat-offender-level` | `high` | 提升到的级别：`low`/`normal`/`high`/`urgent` |
| `overdue-check-minutes` | `15` | 超时检查间隔 |
| `overdue-alert-enabled` | `true` | 超时后提醒在线管理员（每条只提醒一次） |
| `sla-hours.urgent` | `2` | 各优先级处理时限；`0` 不追踪 |
| `sla-hours.high` | `6` | 同上 |
| `sla-hours.normal` | `24` | 同上 |
| `sla-hours.low` | `72` | 同上 |

### reports.replies（举报对话）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关 |
| `allow-player-replies` | `true` | 玩家可否回复自己的举报 |
| `max-length` | `300` | 单条回复最大字符 |
| `max-per-report` | `50` | 每条举报最多保留回复数 |
| `notify` | `true` | 新回复时通知对方 |

### reports.chat-report（聊天多选举报）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 默认关；需同时开 `evidence.enabled`、`evidence.capture-on-report`、`evidence.capture.chat-log` |
| `category` | `chat` | 触发多选的分类 ID/别名 |
| `max-selected` | `5` | 一次最多勾选聊天条数 |

### reports.categories

键名即分类 ID，可自由增删。字段：

| 字段 | 说明 |
|---|---|
| `display` | 显示名**兜底**；`lang/<language>.yml` 的 `categories.<id>.name` 优先 |
| `icon` | Bukkit Material 名 |
| `aliases` | 命令别名列表（`/report <玩家> <别名>`） |
| `enabled` | 该分类是否可用 |
| `priority` | 越大越靠前（仅展示排序） |
| `severity` | `low`/`normal`/`high`，映射到默认优先级 |
| `description` | 描述行列表；`lang` 的 `categories.<id>.description` 优先 |

内置 7 类：`cheating`、`chat`、`grief`、`advertising`、`bug`、`inappropriate`、`other`。新增分类后：语言文件加对应 `categories.<新id>.name/.description` 即自动本地化，不加则回落 `display`。

## gui

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 关后只能用命令 |
| `fill-empty-slots` | `true` | 空白槽填占位 |
| `close-on-action` | `false` | 管理操作后自动关 GUI |
| `sounds.enabled` | `true` | GUI 音效 |
| `sounds.click` | `ui.button.click` | 点击音 |
| `sounds.success` | `entity.player.levelup` | 成功音 |
| `sounds.error` | `entity.villager.no` | 失败音 |
| `admin.enabled` | `true` | 管理 GUI 开关 |
| `admin.confirm-destructive` | `true` | 破坏性操作 GUI 内二次确认 |

## notifications

### staff

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 新举报提醒在线管理员 |
| `permission` | `reportdesk.notify` | 收到提醒所需权限 |
| `sound-enabled` | `true` | 提醒音效 |
| `sound` | `block.note_block.pling` | 音效名 |

### reporter

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 状态变化通知举报人 |
| `queue-offline` | `true` | 离线时暂存、上线补发 |
| `max-queued` | `20` | 每人最多暂存条数 |
| `queue-retention-days` | `30` | 暂存保留天数；`0` 永久 |

### watchlist

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 关注功能 |
| `max-targets` | `20` | 每人最多关注数 |
| `notify-on-report` | `true` | 被关注者被举报时即时提醒 |

### broadcast

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 全服广播新举报（默认关防刷屏） |

### webhook

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | Discord Webhook（异步发送） |
| `url` | `''` | Webhook 地址 |
| `timeout-seconds` | `5` | 请求超时 |
| `username` | `ReportDesk` | 显示名 |
| `avatar-url` | `''` | 头像 |
| `events.created` / `claimed` / `closed` | `true` | 各事件是否推送 |

## evidence（增强层，默认全关）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | **总开关**；关则下面全部不生效 |
| `capture-on-report` | `true` | 提交时采集目标快照（受总开关约束） |
| `retention-days` | `30` | 证据保留天数；`0` 永久 |
| `chat-log-size` | `20` | 每人保留聊天条数 |
| `chat-max-length` | `256` | 单条聊天最大字符 |

### evidence.capture

| 键 | 默认 |
|---|---|
| `location` / `game-mode` / `health` / `inventory` | `true` |
| `inventory-max-items` | `41` |
| `display-name-max-length` | `80` |
| `chat-log` | `true` |
| `ping` | `true` |

### evidence.block-break

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 启用后才注册 `BlockBreakEvent` |
| `max-per-player` | `100` | 每人在内存样本上限 |

### evidence.movement

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 启用后才注册 `PlayerMoveEvent` |
| `min-interval-ticks` | `20` | 采样最小间隔 |
| `max-samples` | `60` | 每人轨迹样本上限 |

### evidence.auto-detect

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 只生成报告/通知，不处罚 |
| `check-interval-seconds` | `5` | 检测间隔 |
| `window-seconds` | `10` | 统计窗口 |
| `block-break-threshold` | `40` | 窗口内破坏方块数触发阈值 |
| `cooldown-seconds` | `300` | 同目标两次自动报告最小间隔 |
| `create-report` | `true` | 是否生成系统举报；`false` 只通知 |
| `notify-staff` | `true` | 是否通知在线管理员 |

## punishments（联动处罚，默认关）

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 关则不执行任何命令 |
| `resolved-commands` | `[]` | 成立时执行的命令（不带前导 `/`），可用占位符 `%player% %reason% %id% %staff% %verdict% %category%` |
| `rejected-commands` | `[]` | 不成立时执行的命令 |

## audit

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 管理操作审计（异步写） |
| `retention-days` | `180` | 审计保留天数 |
| `page-size` | `10` | `/reports audit` 每页条数 |

## config-editor

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 游戏内 `/reports config set` 总开关；关后仍可 list/get |
| `backup-on-save` | `true` | 写前备份 config.yml |
| `max-backups` | `5` | config.yml 备份数 |
| `max-value-length` | `256` | 单值最大字符，防误粘超长 |

## security

| 键 | 默认 | 说明 |
|---|---|---|
| `admin-cooldown-millis` | `300` | 管理操作限流 |
| `pending-timeout-seconds` | `120` | GUI 聊天输入等待超时 |
| `detail-min-length` | `2` | 举报详情最短 |
| `detail-max-length` | `200` | 举报详情最长 |
| `note-max-length` | `500` | 管理备注最长 |
| `max-notes-per-report` | `20` | 每条举报最多备注数 |
| `detail-required` | `false` | 是否强制填详情 |
| `allow-color-codes` | `false` | 是否允许 `§` 颜色码 |
| `player-name-pattern` | `^[A-Za-z0-9_]{3,16}$` | 玩家名校验正则 |
| `blocked-words` | `[]` | 详情禁词（不区分大小写） |
