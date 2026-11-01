# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-10-31

### Initial Release (首个正式版发布)

- 完整举报 GUI：玩家提交、管理受理、认领/关闭/重开/删除全流程
- 异步持久化：WAL 写入日志 + 周期压缩，主线程零文件 IO
- 可选增强层：上下文快照（聊天/方块/轨迹/物品栏）、自动检测、联动处罚，全部默认关闭
- 审计日志：管理操作全量记录，按天分文件
- 通知系统：站内通知、离线队列、关注列表提醒、Webhook 推送
- 游戏内配置编辑：`/reports config` 直接改 `config.yml`
- 支持 Paper 1.20 – 1.21.x，Java 17+ 运行
- 许可：GNU Affero General Public License v3.0（AGPL-3.0）
