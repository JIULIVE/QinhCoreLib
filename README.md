# QinhCoreLib

秦淮系列插件的核心底座，用于统一物品、技能、锻造、强化、脚本、经济、PDC、数据库、GUI 与桥接状态。

## 版本

- 当前版本：`1.1.0`
- 目标运行环境：Paper / Purpur / Spigot `1.21.11+`
- Java：`25+`

## 这个库负责什么

QinhCoreLib 不是单纯的工具包，而是整个生态的底座：

- 统一 `QCL` 命令与生态状态查看
- 统一桥接状态、模块状态、启动状态、配置状态
- 提供 `ItemManagerAPI`
- 提供脚本桥 `QinhScriptApi`
- 提供经济、数据库、PDC、PlaceholderAPI 入口
- 为 `QinhItems` / `QinhSkills` / `QinhForge` / `QinhStrengthen` / `QCR` 提供公共基础能力

## 主要能力

### 1. 生态状态与诊断

使用 `/qcl status` 查看：

- 平台健康码
- 模块健康
- 脚本桥
- 经济桥
- PAPI
- 数据库
- PDC
- QI / QS / QF / QSt 诊断
- 配置诊断
- 启动诊断
- API 边界
- `apiJar` 清单

### 2. 公开 API

QinhCoreLib 对外暴露的主要 API 包包括：

- `com.qinhuai.corelib.api.item`
- `com.qinhuai.corelib.api.item.module`
- `com.qinhuai.corelib.script`
- `com.qinhuai.corelib.economy`
- `com.qinhuai.corelib.database`
- `com.qinhuai.corelib.pdc`
- `com.qinhuai.corelib.placeholder`

这些 API 面会作为 `apiJar` 的核心导出边界。

### 3. 配置与运行时

- `plugin.yml` 已声明 Kotlin、GraalJS 与 Groovy 运行库
- `QinhCoreLib` 会在启动时初始化经济、脚本、物品源与模块系统
- `reload` 会重载核心配置、脚本、外部物品模块与 GUI

## 构建

```bash
mvn -q -DskipTests package
```

## 目录建议

- `src/main/kotlin/com/qinhuai/corelib/api`：对外 API
- `src/main/kotlin/com/qinhuai/corelib/debug`：诊断与状态
- `src/main/kotlin/com/qinhuai/corelib/bootstrap`：启动与模块生命周期
- `src/main/kotlin/com/qinhuai/corelib/item`：物品源与解析
- `src/main/kotlin/com/qinhuai/corelib/script`：脚本桥

## 备注

如果你正在对接其他模块，优先依赖公开 API 包，不要直接耦合内部实现包。
