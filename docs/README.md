# ReportDesk Wiki

面向 Minecraft 服务器管理者的完整使用与开发文档。插件功能为"玩家举报 → 管理员受理 → 结案归档"的完整工作流，并附带可整体开关的证据增强层。

## 目录

| 文档 | 内容 |
|------|------|
| [01-overview.md](01-overview.md) | 插件定位、功能总览、与同类插件差异 |
| [02-installation.md](02-installation.md) | 安装、依赖、首次运行、目录结构 |
| [03-commands.md](03-commands.md) | 全部命令与参数详解（玩家侧 / 管理侧） |
| [04-permissions.md](04-permissions.md) | 权限节点、默认值、与 op 的关系 |
| [05-configuration.md](05-configuration.md) | config.yml 每个字段说明（含默认值与合法范围） |
| [06-enhancements.md](06-enhancements.md) | 证据增强层：快照 / 轨迹 / 自动检测 / 联动处罚 |
| [07-i18n.md](07-i18n.md) | 多语言：语言文件结构、新增语言、分类本地化 |
| [08-architecture.md](08-architecture.md) | 模块划分、数据流、线程模型、存储格式 |
| [09-development.md](09-development.md) | 构建、测试、代码风格、发布流程 |

## 快速开始

```text
1. 将 ReportDesk-1.0.0.jar 放入服务端 plugins/ 目录
2. 启动服务器，插件生成 plugins/ReportDesk/config.yml
3. 默认即可使用：玩家 /report，管理员 /reports
4. 想启用证据增强层：编辑 config.yml 的 evidence.enabled 后 /reports reload
```
