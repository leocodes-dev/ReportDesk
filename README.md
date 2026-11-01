# ReportDesk

ReportDesk 是一个面向 Minecraft 服务器的玩家举报工作台：完整的举报提交 GUI、管理受理工作流、异步持久化、审计日志，以及一个可整体开关的证据采集增强层。

## 简介

服务端把"玩家举报 → 管理员受理 → 结案归档"这条流程完整产品化：

- 玩家用图形化 GUI 选择被举报对象、分类与原因，提交即入队
- 所有阈值（冷却、每日上限、重复间隔、保留天数、分页大小、自动关闭天数、通知开关、证据采集开关、证据保留时长）都写在 `config.yml`，代码里没有任何业务魔法数
- 存储走独立的异步线程池，主线程零文件 IO、零数据库 IO
- 增强层（证据采集、轨迹记录、自动检测、联动处罚）默认全部关闭，关闭路径入口只判一次开关直接返回

## 核心特性

- **举报 GUI**：玩家端开举报工作台，选择对象/分类/备注；管理端独立界面，支持认领、关闭、重开、删除、导出
- **上下文快照**：举报瞬间可选采集玩家现场上下文——聊天日志、周围区块方块破坏、位置与移动轨迹、物品栏与装备。所有采集项独立开关、默认关闭，关闭时入口一次判断直接短路
- **录像回放（轨迹）**：增强层可选记录举报前一段时间内的移动轨迹（`evidence.movement`），结案后在 GUI 里回放；默认关闭，且监听器第一行判断开关，false 立即返回
- **管理员工作流**：`/reports` 子命令支持 claim、close、reopen、delete、purge、overdue、watch、stats、export、audit、reload、config 等
- **审计日志**：管理操作全部写异步审计日志，按天分文件，支持保留天数与自动清理
- **异步存储**：举报记录、证据、审计全部异步落盘，`reports.json` 采用 WAL（write-ahead log）+ 周期压缩，高频操作只追加一行
- **通知系统**：举报创建/关闭/回复、离线举报人队列、关注列表提醒、Webhook 推送
- **联动处罚（可选）**：结案后按裁决结果执行预设命令（需手动开启）

## 支持版本

- 服务端：Paper 1.20 – 1.21.x（`api-version: 1.20`，对 1.20 及以上的 Paper 均可加载）
- 运行 JDK：Java 17+（产物为 Java 17 字节码，1.20 时代默认 JDK 17 可直接运行；1.21+ 的 Java 21 服务端同样兼容）
- 兼容性说明：`paper-plugin.yml` 为权威描述符；另保留一份遗留 `plugin.yml`，旧服务器若仍从 `plugin.yml` 解析命令也能工作，插件同时有运行时 CommandMap 兜底

## 指令与权限

### /report（玩家侧）

别名：`rdreport`、`jb`

| 用法 | 说明 |
|------|------|
| `/report` | 打开举报 GUI（或按配置进入聊天输入模式） |
| `/report <玩家>` | 直接对某玩家发起举报 |
| `/report <玩家> <分类> [详情...]` | 指定分类与详情文本 |
| `/report status [页码]` | 查看自己提交的举报列表 |
| `/report view <id>` | 查看自己某条举报详情 |
| `/report reply <id> <内容>` | 回复自己的举报 |
| `/report replies <id>` | 查看某条举报的回复串 |
| `/report cancel` | 取消当前正在输入的举报 |
| `/report help` | 帮助 |

权限：`reportdesk.report`（默认所有人）；`reportdesk.bypass.limits`（可绕过冷却与每日上限）。

### /reports（管理侧）

别名：`reportdesk`、`rd`

| 子命令 | 说明 |
|--------|------|
| `menu` / `gui` / `admin` | 打开管理 GUI |
| `list [页码]` | 文字列出举报 |
| `search <关键字>` | 搜索举报 |
| `view <id>` | 查看某条举报 |
| `claim <id>` / `release <id>` | 认领 / 释放 |
| `close <id> <裁决> [备注]` | 关闭举报 |
| `reopen <id>` | 重开已结案举报 |
| `tp <玩家名>` | 传送到目标 |
| `evidence <id>` | 查看采集到的证据 |
| `note <id> <内容>` | 添加管理备注 |
| `reply <id> <内容>` / `replies <id>` | 回复 / 查看回复串 |
| `delete <id>` | 删除举报（含证据） |
| `purge <天数>` | 清除 N 天前已结案举报 |
| `overdue` | 查看超时未处理举报 |
| `watch <玩家>` / `unwatch` / `watching` | 关注列表 |
| `stats` | 统计面板 |
| `export` | 导出 CSV |
| `audit [页码]` | 查看审计日志 |
| `reload` | 重载配置 |
| `config list/get/set/reset` | 在游戏内修改 `config.yml` |

### 权限节点

| 节点 | 说明 | 默认 |
|------|------|------|
| `reportdesk.report` | 提交举报 | true |
| `reportdesk.admin` | 管理工作台 | op |
| `reportdesk.admin.close` | 关闭举报 | op |
| `reportdesk.admin.delete` | 删除/清除 | op |
| `reportdesk.notify` | 接收新举报通知 | op |
| `reportdesk.evidence` | 查看采集证据 | op |
| `reportdesk.reload` | 重载 | op |
| `reportdesk.config` | 游戏内改配置 | op |
| `reportdesk.watch` | 关注列表 | op |
| `reportdesk.bypass.limits` | 绕过冷却与上限 | false |
| `reportdesk.bypass.evidence` | 不被增强层采集 | false |

## 安装

1. 把 `ReportDesk-1.0.0.jar` 丢进服务端 `plugins/` 目录
2. 启动服务器，插件会在 `plugins/ReportDesk/` 下生成默认 `config.yml`
3. 按需修改配置（增强层全部默认关闭，想开哪块再开哪块）

## 许可

本项目采用 [GNU Affero General Public License v3.0](LICENSE)（AGPL-3.0）许可。网络使用被视为分发，任何修改版必须同样开源。
