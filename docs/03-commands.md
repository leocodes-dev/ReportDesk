# 03 · 命令参考

## 玩家侧：/report

别名：`rdreport`、`jb`
权限：`reportdesk.report`（默认所有人）

### 用法

```text
/report                              打开举报 GUI（选玩家 → 选分类 → 详情）
/report <玩家>                       直接对某玩家发起举报（进入选分类）
/report <玩家> <分类> [详情...]      直接提交，跳过 GUI
/report status [页码]                查看我提交的举报列表
/report view <id>                    查看我某条举报的详情
/report reply <id> <内容...>         回复我某条举报
/report replies <id> [页码]         查看某条举报的回复串
/report cancel                       取消正在进行的聊天输入
/report help                         帮助
```

### 参数说明

| 参数 | 说明 |
|---|---|
| `<玩家>` | 在线或离线玩家名（离线取决于 `limits.allow-offline-target`） |
| `<分类>` | 分类 ID 或别名，见 [config.yml · reports.categories](05-configuration.md#reportscategories) |
| `[详情...]` | 自由文本，长度受 `security.detail-min/max-length` 约束 |

### 行为细则

- **冷却与限流**：同一玩家两次提交间隔受 `limits.command-cooldown-millis`（命令层）与 `limits.cooldown-seconds`（举报层）约束；每日上限 `limits.daily-limit`；对同一目标的重复间隔 `limits.same-target-interval-seconds`。
- **重复举报**：`limits.duplicate-window-seconds` 内的"同举报人+同目标+同分类"会被判为 `DUPLICATE` 而拒绝。
- **绕过限制**：`reportdesk.bypass.limits` 权限可跳过冷却、每日上限与重复间隔。
- **自己举报自己**：`limits.block-self-report` 为 `true` 时拒绝。
- **账号年龄**：`limits.min-account-age-minutes` 限制新号举报。
- **分类禁用**：`reports.categories.<id>.enabled: false` 的分类不可提交，会提示"该分类已关闭"。

## 管理侧：/reports

别名：`reportdesk`、`rd`
总闸权限：`reportdesk.admin`（无此权限时 `/reports` 任何子命令都会被拒绝）

### 子命令一览

```text
/reports                                  打开管理 GUI（等价于 menu）
/reports menu | gui | admin               打开管理 GUI
/reports list [筛选... 页码]              文字列出举报
/reports search <关键字>                  按目标/举报人/详情搜索
/reports view <id>                        查看一条举报
/reports claim <id>                       认领
/reports release <id>                     释放
/reports close <id> <裁决> [备注...]      结案
/reports reopen <id>                      重开已结案
/reports tp <玩家名>                      传送到目标
/reports evidence <id>                    查看该举报的证据快照
/reports note <id> <内容...>              添加管理备注
/reports reply <id> <内容...>             以管理员身份回复
/reports replies <id> [页码]             查看回复串
/reports delete <id>                      删除一条举报（含证据）
/reports purge <天数>                     清除 N 天前已结案举报
/reports overdue                          列出超 SLA 未处理的举报
/reports watch <玩家>                     关注玩家
/reports unwatch <玩家>                   取消关注
/reports watching                         查看我关注的玩家
/reports stats                            统计面板
/reports export                           导出当前列表筛选为 CSV
/reports audit [页码]                     查看审计日志
/reports reload                           重载 config.yml 与语言文件
/reports config list                     列出可在线修改的配置项
/reports config get <路径>                查看单个配置值
/reports config set <路径> <值>          修改配置（需 reportdesk.config）
/reports config reset <路径>             重置为默认值
```

### 裁决（verdict）

`/reports close <id> <裁决>` 的 `<裁决>` 可写英文或中文：

| 英文 | 中文 | 含义 | 落库状态 |
|---|---|---|---|
| `resolved` / `accept` | `成立` | 举报成立 | `RESOLVED` |
| `rejected` | `不成立` | 举报不成立 | `REJECTED` |
| `duplicate` | `重复` | 与既有举报重复 | `DUPLICATE` |
| `insufficient` | `证据不足` | 证据不足，按不成立归档 | `REJECTED` + verdict=INSUFFICIENT |
| `auto-closed` | `自动关闭` | 维护任务自动结案（一般手工不用填） | `AUTO_CLOSED` |

### 权限细分

总闸 `reportdesk.admin` 之外，还有细粒度节点（见 [04-permissions](04-permissions.md)）。`reportdesk.admin` 通常由 op 或权限插件授予。

### Tab 补全

- 一级：补全子命令；
- 二级：按子命令补全（玩家名、举报 ID、裁决词、配置路径等）；
- 有权限才显示。

### 示例

```text
# 查看第 2 页未处理举报
/reports list open 2

# 结案：举报 42 成立，备注"已核实飞行截图"
/reports close 42 resolved 已核实飞行截图

# 关注惯犯
/reports watch Notch

# 查看我关注了谁
/reports watching

# 在游戏内改冷却为 120 秒
/reports config set limits.cooldown-seconds 120
```
