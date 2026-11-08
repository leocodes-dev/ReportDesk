# ReportDesk 开发硬约束（必须全部满足）

> 本文件为强制规范。任何功能开发、修复、重构、配置调整、文档变更均须遵守；违反任一条视为任务未完成。

---

## 1. paper-plugin.yml 的 api-version

- `api-version` 必须为 `'1.20'`。
- 禁止改成更高版本，除非明确升级兼容策略：更新最低服务端版本、重测并更新文档。
- 验收点：文件内必须是 `api-version: '1.20'`。

---

## 2. 所有业务阈值必须放在 config.yml，代码中不得硬编码

- 包括但不限于：举报冷却秒数、每日举报上限、对同一目标重复举报间隔、举报记录保留天数、分页大小、自动关闭天数、通知开关、证据采集开关、证据保留时长。
- 验收点：代码中不得出现业务魔法数；必须读取配置并提供默认值与非法值校验。

---

## 3. 基础举报功能禁止注册高频事件

- 举报提交、GUI、记录查询、管理受理等基础功能不得依赖 `BlockBreakEvent`、`PlayerMoveEvent` 等高频事件。
- 只有「证据采集增强层」才允许注册 `BlockBreakEvent`，且只做轻量采集和入队；主线程禁止文件 IO、数据库 IO、大范围重统计，落盘和统计必须异步。
- 验收点：基础功能代码路径中不出现高频事件监听器；增强层事件内不出现阻塞式 IO。

---

## 4. PlayerMoveEvent 默认不注册

- 默认不注册 `PlayerMoveEvent`。
- 若证据采集增强层确实需要，必须默认配置为 `false`；监听器第一行判断开关，`false` 立即 `return`，不得先做任何计算。
- 验收点：默认配置下不注册；注册时开关关闭立即短路。

---

## 5. 增强层所有功能默认 false，关闭时只允许一次开关判断

- 证据采集、轨迹记录、自动检测、联动处罚等增强功能默认关闭。
- 关闭状态下入口处一次判断直接返回，不得继续读配置、走复杂逻辑或调用异步任务。
- 验收点：每个增强功能有独立开关，默认 `false`；关闭路径无额外业务逻辑。

---

## 6. 举报提交与管理操作必须有权限、限流和输入校验

- `/report` 必须防刷限流。
- 管理命令必须权限控制。
- 举报原因、目标玩家名、备注等输入必须校验长度和非法字符。
- 管理操作必须写审计日志，日志写入异步。
- 验收点：无权限不能执行管理操作；超限被拒绝；非法输入被拒绝；审计日志不阻塞主线程。

---

## 7. 举报数据持久化必须异步

- 主线程禁止文件 IO 和数据库 IO。
- 举报记录、证据记录、审计日志的写入和查询不得在主线程阻塞执行。
- GUI 打开和命令处理不得因存储 IO 卡主线程。
- 验收点：主线程代码路径中无 `File.write`、JDBC、阻塞式存储调用。

---

## 8. 构建和测试命令固定

- 固定命令：`./gradlew test build`。
- 报告结果前必须执行该命令；不能只跑 IDE 编译或跳过测试。
- 构建或测试失败时必须修复到通过再报告结果。
- 不允许隐瞒失败、不允许说「应该没问题」、不允许把失败留给用户。必须定位修复，直到 `./gradlew test build` 通过后再报告完成。

---

## 9. （最高红线）开发流程强制备份约束

- 在做任何新功能开发前、做完后、以及整个任务彻底完成时，必须强制进行 Git 提交（Commit）备份。
- **开始前**：先 `git commit` 保存当前稳定状态，以便随时回滚。
- **做完后**：测试通过后再次 `git commit`，锁定阶段性成果。
- **完成后**：确保工作树（working tree）干净，推送到远端。
- 任何批量修改脚本（如批量替换文本、加注释）执行前，必须先备份代码。

---

## 10. 变更日志强制记录（Changelog Mandatory）

### 规则

1. 每完成**任一功能变更**（新增、更新、修复、删除、重构、性能优化、配置调整、文档变更等），必须在项目根目录 `CHANGELOG.md` 追加一条更新日志；已有文件只追加、不覆盖。
2. 更新日志必须与对应变更**同批提交**：顺序固定为 `./gradlew test build` 通过 → 更新 `CHANGELOG.md` → `git commit` → 推送；不得事后补记，不得只提交代码不提交日志。
3. 版本号遵循语义化版本（SemVer）：修复为 patch、功能为 minor、破坏性变更为 major；未发布变更可先记入「未发布」区段，发版时定版。
4. 每条日志必须包含：**版本号、发布日期、中英文概述各一段、按分类的更新一览**；概述使用官方、客观、可验证的表述，禁止「应该没问题」「若干修复」等模糊措辞。
5. 任务彻底完成时，工作树必须干净，且 `CHANGELOG.md` 已包含本次全部变更；缺日志视为任务未完成。

### 更新日志模板

```markdown
## Release X.Y.Z

ReportDesk X.Y.Z 对<核心模块>进行了<优化/增强/修复>，新增<能力 A>与<能力 B>，并对<配置/权限/输入校验/异步存储>进行了加强。本版本同时修复了<问题描述>，进一步提升了插件在高并发与大型服务器场景下的稳定性与可靠性。

ReportDesk X.Y.Z delivers <improvements> to <core module>, introduces <capability A> and <capability B>, and strengthens <configuration / permissions / input validation / asynchronous storage>. This release also fixes <issue>, further improving stability and reliability under high concurrency and on large-scale servers.

### X.Y.Z 更新一览（YYYY-MM-DD）

**新增**
- ……

**优化**
- ……

**修复**
- ……

**配置**
- ……

**安全**
- ……
```

### 示例

```markdown
## Release 1.4.0

ReportDesk 1.4.0 对举报受理流程进行了系统性优化，新增举报记录分页查询与异步审计日志能力，并加强了配置校验与输入过滤。本版本同时修复了高并发场景下的若干稳定性问题，进一步提升了插件在大型服务器上的可靠性。

ReportDesk 1.4.0 delivers systematic improvements to the report handling workflow, introducing paginated report queries and asynchronous audit logging, and strengthens configuration validation and input filtering. This release also fixes several stability issues under high concurrency, further improving reliability on large-scale servers.

### 1.4.0 更新一览（YYYY-MM-DD）

**新增**
- 举报记录支持分页查询，分页大小由 `config.yml` 的 `pagination.page-size` 配置。
- 审计日志改为异步写入，管理操作不再阻塞主线程。

**优化**
- 举报冷却、每日上限、重复举报间隔统一由 `config.yml` 驱动，并提供默认值与非法值回退。
- 证据采集增强层默认关闭，关闭时入口一次开关判断即返回。

**修复**
- 修复目标玩家名含非法字符时未被拒绝的问题。
- 修复打开 GUI 时同步读取存储导致的卡顿。

**配置**
- 新增 `evidence.enabled`、`evidence.retention-days` 等配置项，默认值均为安全值。
```

### 与既有流程的衔接

- 该约束不改变既有红线：开前 commit、做完测试通过后 commit、完成时工作树干净并推送；只是把「更新 `CHANGELOG.md`」插入到「测试通过」与「commit」之间。
- 批量修改脚本执行前仍须先备份代码；脚本若涉及功能变化，同样要触发一条更新日志。
- 若某次提交纯属格式/注释且无行为变化，可归入「文档」分类，但仍需记录，不得跳过。

---

## 固定执行顺序（每次任务）

1. `git commit` 保存当前稳定状态（开始前）。
2. 执行变更（涉及批量脚本前先备份代码）。
3. 运行 `./gradlew test build`，失败则定位修复直到通过。
4. 更新 `CHANGELOG.md`（按本文件第 10 节模板）。
5. `git commit` 锁定阶段性成果（做完后）。
6. 确认工作树干净并推送到远端；缺日志或工作树不干净视为任务未完成。