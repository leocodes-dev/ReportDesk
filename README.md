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

- 服务端：Spigot / Paper / Folia 1.20+（`api-version: 1.20`，对 1.20 及以上的各核心均可加载；编译目标为 Spigot 1.20.1 API，即三核心最低公共子集）
- 运行 JDK：Java 17+（产物为 Java 17 字节码，1.20 时代默认 JDK 17 可直接运行；1.21+ 的 Java 21 服务端同样兼容）
- 兼容性说明：
  - `plugin.yml` 是 Spigot 的权威描述符，`paper-plugin.yml` 是 Paper/Folia 的权威描述符（两者 `api-version` 均为 `'1.20'`），随 jar 同发，核心各取所需
  - 插件 jar 内部打包（relocate 到 `com.reportdesk.libs.*`）Adventure/MiniMessage 与 Gson，聊天与 GUI 文本统一走 legacy 格式（`§` 颜色码），三个核心上行为一致；lang 文件中没有 legacy 对应物的 MiniMessage 标签（如渐变、点击/悬停）在 Spigot 上降级为纯文本
  - 主线程判定由 `com.reportdesk.util.PrimaryThread` 反射适配（Paper/Folia 的 `Bukkit.isPrimaryThread()`，Spigot 的 `CraftScheduler.isPrimaryThread()`），聊天输入监听使用三核心共有的 `AsyncPlayerChatEvent`
  - 运行时 CommandMap 兜底，别名 `rdreport` / `jb` / `reportdesk` / `rd` 在各核心均有效

## 指令与权限

### /report（玩家侧）

别名：`rdreport`、`jb`

| 用法 | 说明 |
|------|------|
| `/report` | 打开举报 GUI（或按配置进入聊天输入模式） |
| `/report <玩家>` | 直接对某玩家发起举报 |
| `/report <玩家> <分类> [详情...]` | 指定分类与详情文本 |
| `/report status [页码]` | 查看自己提交的举报列表 |
| `/report history [状态] [页码]` | 查看我的举报历史（状态：all / open / claimed / resolved / rejected / duplicate / auto-closed；GUI 开启时为图形界面，另支持 `/report history view <id>` 查看单条详情） |
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
| `assign <id> <管理员>` | 把未认领举报指派给指定在线管理员（目标须持有 `reportdesk.admin`） |
| `transfer <id> <管理员>` | 将已认领举报转派给其他管理员（保留原认领时间；非属主需 `reportdesk.admin.close` 覆盖） |
| `close <id> <裁决> [备注]` | 关闭举报 |
| `reopen <id>` | 重开已结案举报 |
| `tp <玩家名>` | 传送到目标 |
| `evidence <id>` | 查看采集到的证据 |
| `note <id> <内容>` | 添加管理备注 |
| `reply <id> <内容>` / `replies <id>` | 回复 / 查看回复串 |
| `delete <id>` | 删除举报（含证据） |
| `purge <天数>` | 清除 N 天前已结案举报 |
| `overdue` | 查看超时未处理举报 |
| `sla [天数]` | 按优先级/分类/管理员维度的 SLA 超期报表 |
| `smart review [天数]` | 智能优先级复盘：垃圾带精确率/召回率/标记率与分数分布（smart 层关闭时仅提示） |
| `watch <玩家>` / `unwatch` / `watching` | 关注列表 |
| `stats` | 统计面板（含人均处理量） |
| `export` | 导出 CSV |
| `audit [页码]` / `audit search <关键字>` / `audit export` | 查看审计日志 / 检索 / 导出 CSV |
| `reload` | 重载配置 |
| `config list/get/set/reset` | 在游戏内修改 `config.yml` |

### 权限节点

| 节点 | 说明 | 默认 |
|------|------|------|
| `reportdesk.report` | 提交举报 | true |
| `reportdesk.admin` | 管理工作台 | op |
| `reportdesk.admin.close` | 关闭举报 | op |
| `reportdesk.admin.assign` | 指派 / 转派举报 | op |
| `reportdesk.admin.delete` | 删除/清除 | op |
| `reportdesk.notify` | 接收新举报通知 | op |
| `reportdesk.evidence` | 查看采集证据 | op |
| `reportdesk.reload` | 重载 | op |
| `reportdesk.config` | 游戏内改配置 | op |
| `reportdesk.watch` | 关注列表 | op |
| `reportdesk.bypass.limits` | 绕过冷却与上限 | false |
| `reportdesk.bypass.evidence` | 不被增强层采集 | false |

## API 事件与生态集成

ReportDesk 把举报生命周期以 Bukkit 事件广播出来，供其它插件消费。所有事件都在**主线程同步**触发：监听器必须线程安全、不得阻塞；需要落盘或数据库 IO 时由监听器自己调度到异步线程。

| 事件 | 时机 | 可取消 |
|------|------|--------|
| `ReportPreSubmitEvent` | 玩家提交前（系统/自动检测不触发） | 是（取消即拒绝本次提交） |
| `ReportSubmittedEvent` | 创建并持久化之后 | 否 |
| `ReportStatusChangedEvent` | 任意状态迁移（认领/结案/重开/释放）之后 | 否 |
| `ReportClaimedEvent` | 管理员认领（或指派）之后 | 否 |
| `ReportClosedEvent` | 管理员结案之后 | 否 |
| `ReportEvidenceCapturedEvent` | 证据层为某条举报采集快照之后（仅当证据层开启） | 否 |

### 结案联动处罚（LuckPerms 示例）

监听 `ReportClosedEvent`，在判定成立（`Verdict.RESOLVED`）时对目标执行封禁命令，不成立（`Verdict.REJECTED`）时清除误报标记。示例插件骨架：

```java
public class ReportDeskHook implements Listener {

  @EventHandler(priority = EventPriority.NORMAL)
  public void onReportClosed(ReportClosedEvent event) {

    Report report = event.getReport();

    if (event.getVerdict() == Verdict.RESOLVED) {

      // 判定成立：临时封禁目标 7 天（需 LuckPerms 等权限插件在场）
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
              "lp user " + report.targetName() + " temp parent 7d punisher"));

    } else if (event.getVerdict() == Verdict.REJECTED) {

      // 判定不成立：解除之前挂的"疑似违规"标记
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
          Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
              "lp user " + report.targetName() + " remove parent suspect"));
    }
  }
}
```

要点：

- 事件已携带**结案后**的 `Report`（含目标名、裁决、结案管理员），无需再查库。
- 处罚命令走**异步**线程池，避免卡主线程（AGENTS 红线：主线程零 IO）。
- 需要 `reportdesk` 与 LuckPerms（或等效权限插件）同时在线；没有权限插件时上述命令静默失败，不影响 ReportDesk 本身。
- 更稳妥的做法：把 `ReportClosedEvent` 桥接到 ReportDesk 的**处罚通道 SPI**（`PunishmentChannel`），由统一开关 `punishments.enabled` 控制，避免处罚逻辑散落。

## 安装

1. 把 `ReportDesk-1.0.0.jar` 丢进服务端 `plugins/` 目录
2. 启动服务器，插件会在 `plugins/ReportDesk/` 下生成默认 `config.yml`
3. 按需修改配置（增强层全部默认关闭，想开哪块再开哪块）

## 许可

本项目采用 [GNU General Public License v3.0](LICENSE)（GPL-3.0-or-later）许可。任何修改版必须同样以 GPLv3 发布，并随二进制提供完整源码。
