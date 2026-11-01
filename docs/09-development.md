# 09 · 开发指南

面向维护者。使用者请读前面的章节。

## 环境

| 项 | 版本 |
|---|---|
| JDK | 17+（产物为 Java 17 字节码） |
| Gradle | 9.6.1（wrapper 已自带） |
| 服务端 API | `paper-api` 1.21.11-R0.1-SNAPSHOT（`api-version` 保持 `'1.20'`） |

## 构建与测试（固定命令）

```bash
./gradlew clean test build
```

- 必须先跑 `test`（当前 184 全绿）再跑 `build`；
- 失败必须修到全绿再报告，不允许"应该没问题"；
- 产物：`build/libs/ReportDesk-1.0.0.jar`。

## 代码风格

- **google-java-format 1.19.1**（2 空格 google 风格）；
- 提交前对改动文件跑一遍 gjf；
- 注释用英文；日志与用户文案全部走 `lang/*.yml`，不硬编码进 Java（`Verdict` 的中文解析别名除外，属输入特性）。

## 硬约束（AGENTS.md 红线，必须全部满足）

1. **`paper-plugin.yml` 的 `api-version` 必须为 `'1.20'`**（`PluginDescriptorTest` 与文件本身双验收点）。不要擅自提高；升级兼容策略需先更新最低服务端版本、重测、改文档。
2. **所有业务阈值放 `config.yml`**，代码不得硬编码（冷却/上限/保留天数/分页/自动关闭/通知开关/证据开关…）。缺失或非法值要回退默认并告警。
3. **基础功能禁止注册高频事件**（`BlockBreakEvent`/`PlayerMoveEvent` 等）；只有增强层可注册，且只做轻量采集入队。
4. **主线程零 IO**：举报、审计、证据写入全异步；GUI 与命令不得因存储卡主线程。
5. **增强层默认全关，入口一次开关判断短路**：关闭路径不读子配置、不做计算、不注册监听器。
6. **权限 + 限流 + 输入校验**：管理命令权限门，`/report` 防刷，输入校验长度与非法字符，审计异步。
7. **最高红线：任何批量修改脚本/开发步骤前必须 `git commit` 备份**；完成后保证工作树干净、提交锁定阶段成果。

## 目录速查

```text
src/main/java/com/reportdesk/   主代码（见 08-architecture）
src/main/resources/
  paper-plugin.yml              权威描述符（api-version '1.20'）
  plugin.yml                    遗留描述符（旧服务器兜底）
  config.yml                    默认配置（全中文注释，兜底值语言中性）
  lang/zh_CN.yml / en_US.yml    文案（920+ key，互为镜像）
src/test/java/                  184 单测
gradle/  build.gradle           构建
```

## 常见改动清单

| 想做什么 | 要改哪里 |
|---|---|
| 新增一条玩家/管理文案 | `MessageKeys` 加常量 → 两个 `lang/*.yml` 各加一项 → 调用处用 `messages.component(key, ...)` |
| 新增一个分类 | `config.yml` 加 `reports.categories.<id>` → 两个 `lang/*.yml` 加 `categories.<id>.name/.description` |
| 新增一个可在线编辑的配置项 | `ConfigKeys` 注册（section + path + 类型 + 读写器）→ `ConfigLoader` 读取 |
| 新增 `/reports` 子命令 | `ReportsCommand` 加 `case` → 权限门 → 本地化 key → Tab 补全 → 审计 |
| 改 `Report` 字段 | 改 record + `copy()` + 存储序列化/反序列化 + 测试全链路 |

## 发布流程

1. `git commit` 备份当前状态；
2. `./gradlew clean test build` 全绿；
3. 更新 `CHANGELOG.md` 与版本号；
4. 再 `git commit` + 推送远端；
5. 产物 jar 上传 Modrinth / Hangar：
   - loader：`paper`
   - 版本：`1.20 – 1.21.x`
   - Java：`17+`
   - 许可：`GPL-3.0-or-later`
   - 无运行时依赖

## 图标

`icon/reportdesk-icon.png` 已处理为 512×512、8-bit RGBA、仅含 `IHDR+IDAT+IEND` 的最简 Web PNG（约 57 KB），可直接上传 Hangar 平台作头像。
