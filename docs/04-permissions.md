# 04 · 权限节点

## 总闸

`reportdesk.admin` 是管理侧的**总闸**：`/reports` 的入口先检查它，没有就直接拒绝，后续任何子命令都不会执行。各子命令再按需检查细粒度节点。

## 完整节点表

| 节点 | 说明 | 默认 |
|------|------|------|
| `reportdesk.report` | 玩家可提交举报（`/report`） | `true`（所有人） |
| `reportdesk.bypass.limits` | 绕过冷却、每日上限、同目标重复间隔 | `false` |
| `reportdesk.bypass.evidence` | 该玩家不被增强层证据采集 | `false` |
| `reportdesk.admin` | 管理总闸（`/reports` 所有子命令） | `op` |
| `reportdesk.admin.close` | 结案 / 重开 | `op` |
| `reportdesk.admin.delete` | 删除单条、purge 清理 | `op` |
| `reportdesk.notify` | 接收新举报的站内通知 | `op` |
| `reportdesk.evidence` | 查看证据快照 | `op` |
| `reportdesk.reload` | `/reports reload` | `op` |
| `reportdesk.config` | `/reports config set/reset` | `op` |
| `reportdesk.watch` | 关注列表（watch / unwatch / watching） | `op` |

> `op` 指"默认只有 op 拥有"；用权限管理插件（LuckPerms 等）可自行改默认与分级。

## 权限检查行为

- **无权限时不会执行操作**，而是发一条 `没有权限` 提示（本地化）；
- GUI 管理界面在进入前也会校验 `reportdesk.admin`，无权限时 GUI 打不开；
- `reportdesk.notify` 只影响"是否收到新举报提醒"，不影响受理操作本身；
- `reportdesk.evidence` 关闭时 `/reports evidence <id>` 与 GUI 证据页不可用；
- 关注列表的"被关注玩家被举报时即时通知"由 `notifications.watchlist` 配置控制，与权限无关。

## 与 op 的关系

Paper 默认 `op` 拥有所有节点（除非 `plugin.yml` 显式把某节点 `default: false`）。本插件的描述符把这些节点声明为 `default: op` 或 `default: true`，所以**纯净 op 不需要任何额外配置**就能跑完整流程。用 LuckPerms 之类插件时，给管理员组批量授予上表节点即可，不必给全服 op。

## 安全建议

1. 生产服不要给玩家组 `reportdesk.admin`；`reportdesk.report` 保留给玩家即可。
2. `reportdesk.admin.delete` 与 `reportdesk.config` 属于高危节点，建议只给少数人。
3. `reportdesk.bypass.limits` 可发给活跃举报型玩家，避免被冷却卡住。
4. 管理操作全部进审计日志（`audit.enabled` 默认开），删除/改配置可回溯。
