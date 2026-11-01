# 02 · 安装与目录结构

## 需求

| 项 | 要求 |
|---|---|
| 服务端 | Paper 1.20 – 1.21.x（`paper-plugin.yml` 中 `api-version: '1.20'`） |
| JDK | 17 及以上 |
| 依赖 | 无。Gson、Adventure、MiniMessage 均由 Paper 运行时提供 |
| 其他插件 | 无依赖、无前置 |

## 安装步骤

```text
1. 下载 ReportDesk-1.0.0.jar
2. 放入服务端 plugins/ 目录
3. 启动服务器
   插件在 plugins/ReportDesk/ 下生成：
     config.yml        全部业务配置（默认值即基础功能可用）
     lang/zh_CN.yml   中文文案
     lang/en_US.yml   英文文案
     data/            运行时数据（首次写入后生成）
4. 玩家用 /report，管理员用 /reports 即可开始
```

重启不是必须步骤：`/reports reload` 可热加载 `config.yml` 与语言文件。

## 目录结构

```text
plugins/ReportDesk/
├── config.yml                 业务配置（语言、限流、分类、增强层…）
├── lang/
│   ├── zh_CN.yml              中文文案（插件自带文案）
│   └── en_US.yml              英文文案
└── data/
    ├── reports.json           举报记录（权威数据，周期压缩产物）
    ├── reports.log            WAL 追加日志（高频写入口）
    ├── reports.json.bak/      reports.json 备份（最多 5 份，可配）
    ├── evidence/              证据快照（启用增强层后生成）
    ├── audit/                 审计日志（按天分文件：audit-YYYY-MM-DD.log）
    └── pending-notices.json   离线举报人的待补发通知
```

### 数据文件说明

| 文件 | 写时机 | 读时机 |
|---|---|---|
| `reports.log` | 每次举报创建/状态变更（追加一行） | 启动时回放 |
| `reports.json` | 启动回放 + 达到压缩阈值时全量重写 | 启动加载、查询 |
| `audit/*.log` | 管理操作（异步追加） | `/reports audit`、维护任务清理 |
| `evidence/*` | 增强层采集时（异步） | 查看证据、过期清理 |
| `pending-notices.json` | 通知收件人离线时暂存 | 上线时补发 |

## 升级

1. 用新 jar 替换 `plugins/ReportDesk/` 中的旧 jar；
2. 启动即可。旧数据文件按原路径读取，不需要迁移；
3. 若语言文件有新增 key，插件会用内置副本补齐后再回写，旧服务器不会出现"缺 key 显示 key 本身"的情况；
4. 建议在升级前对 `data/` 目录做一次性备份（插件自身也会保留最多 5 份 `reports.json` 备份）。

## 卸载

1. 停止服务器；
2. 删除 `plugins/ReportDesk/` 与 jar；
3. 玩家/管理员不需要额外操作。若希望保留举报记录做存档，先自行拷贝 `data/`。

## 常见问题

**Q：启动日志报 "api-version" 相关错误？**
A：确认服务端是 1.20 及以上的 Paper（或兼容发行版）。插件不兼容 1.20 以下版本。

**Q：`/report` 提示无权限？**
A：`reportdesk.report` 默认为所有人。检查权限管理插件是否误删了默认项。

**Q：改配置后不生效？**
A：`storage.directory` 修改必须重启；其余配置用 `/reports reload` 即可。语言文件重载同样走 `/reports reload`。

**Q：GUI 打开后点玩家头颅分类名还是中文？**
A：分类名/描述优先读 `lang/<language>.yml` 的 `categories.<id>.name/.description`，取不到才回落 config 里的 `display`。确认 `config.yml` 的 `language` 与实际语言文件匹配，并重载。
