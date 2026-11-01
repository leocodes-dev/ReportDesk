# 06 · 证据增强层

增强层是**可选**的：全部默认关闭，关闭时不注册高频事件、不做额外主线程计算、入口一次开关判断直接返回。想开哪块开哪块。

## 总开关

```yaml
evidence:
  enabled: false        # ← 增强层总闸
```

`enabled: false` 时，下面所有子项（`capture-on-report`、`block-break`、`movement`、`auto-detect`）一律不生效，也**不会注册** `BlockBreakEvent` / `PlayerMoveEvent` 监听器。

## 各子功能

### 1. 现场快照（capture-on-report）

提交举报瞬间，对在线目标采集一份上下文快照：位置、游戏模式、血量、物品栏、装备、聊天历史、ping。

- 需要：`evidence.enabled: true` + `evidence.capture-on-report: true` + 各 `capture.*` 子项
- 采集是**异步**的，不会卡提交动作
- 快照随举报存档，`/reports evidence <id>` 或 GUI 证据页查看
- 证据保留天数 `evidence.retention-days`，过期自动清理

**适合**：偷窃、破坏、作弊取证。

### 2. 方块破坏采样（block-break）

仅当 `evidence.block-break.enabled: true` 时注册 `BlockBreakEvent`，在内存里给每个玩家保留最近的破坏样本（`max-per-player`），供取证与自动检测使用。

- 只做**轻量入队**，不在事件里做 IO
- 关闭时监听器不注册，零事件开销

**适合**：griefing / 破坏类取证。

### 3. 移动轨迹（movement）

`evidence.movement.enabled: true` 时注册 `PlayerMoveEvent`，按 `min-interval-ticks` 采样，每人保留 `max-samples` 个轨迹点。结案后 GUI 里可回放。

- **监听器第一行判断开关，false 立即 return**，不做任何计算
- 采样只入有界队列，异步处理

**适合**：飞行、穿墙、瞬移类作弊取证。

### 4. 自动检测（auto-detect）

`evidence.auto-detect.enabled: true` 时，每隔 `check-interval-seconds` 统计一次，若某玩家在 `window-seconds` 内破坏方块数 ≥ `block-break-threshold`，则触发：

- 可选生成一条**系统举报**（`create-report: true`）
- 可选通知在线管理员（`notify-staff: true`）
- **不会自动处罚**，处罚必须走 `punishments`（默认关）
- 同一目标两次自动报告最小间隔 `cooldown-seconds`

**适合**：挂机破坏、批量 griefing 的自动预警。

### 5. 联动处罚（punishments，独立开关）

`punishments.enabled: true` 时，举报**结案**后按裁决结果执行预设命令：

- `resolved-commands`：举报成立时
- `rejected-commands`：举报不成立时

可用占位符：`%player%` `%reason%` `%id%` `%staff%` `%verdict%` `%category%`。

**安全建议**：处罚命令用权限管理插件先做 dry-run，避免误伤； `%reason%` 会带入举报详情，注意内容注入（详情本身已做长度与非法字符校验）。

## 开关顺序（AGENTS 约定）

每个增强功能入口都是"**先判总开关，再判子开关**"，且关闭路径一次判断直接返回。例如 `BlockBreakEvidenceListener.onBlockBreak` 第一行就是 `if (!evidence().enabled() || !blockBreak().enabled()) return;`。

## 默认配置（开箱即用）

```yaml
evidence:
  enabled: false          # 全部关
punishments:
  enabled: false          # 全部关
```

建议的渐进启用顺序：

1. 先开 `evidence.enabled: true` + `capture-on-report: true`，只拿快照（无高频事件）；
2. 需要取证再按需开 `block-break` / `movement`；
3. 要自动预警才开 `auto-detect`；
4. 最后才考虑 `punishments`，且命令务必人工审核过。
