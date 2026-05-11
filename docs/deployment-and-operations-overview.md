# 部署与运维总览（索引）

原文档已拆分为两版，便于 **投标管理层** 与 **实施/车间运维** 分工阅读。

| 版本 | 文件 | 受众 | 篇幅（导出 Word/A4、小四、约估） |
|------|------|------|--------------------------------|
| **投标简版（纯表格）** | [deployment-and-operations-overview-bid.md](./deployment-and-operations-overview-bid.md) | **甲方 IT 负责人、招标评审、项目经理** | **约 2～3 页** |
| **实施详版** | [deployment-and-operations-overview-implementation.md](./deployment-and-operations-overview-implementation.md) | **甲方 IT（实施/运维）、车间运维值班、集成商驻场** | **约 8～12 页** |

**说明**：页数为导出典型排版下的经验区间，实际随表格换行与附录增减浮动。

---

## 执行级文档（不改写边界，仅提供命令）

| 文档 | 用途 |
|------|------|
| [run-and-deploy.md](./run-and-deploy.md) | Compose 一键搭建、前置条件 |
| [deployment.md](./deployment.md) | 组件与快速命令 |
| [production-deployment.md](./production-deployment.md) | 监控栈、备份、systemd、CI/CD |
| [runbooks/common-operations.md](./runbooks/common-operations.md) | 日常运维与故障排查 |
| [../android/station-kiosk/README.md](../android/station-kiosk/README.md) | 一体机 APK |
