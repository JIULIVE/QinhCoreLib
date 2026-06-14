# 更新日志 / Changelog

本项目遵循语义化版本（Semantic Versioning）。

## [1.1.0] - 2026-06-14

### 新增 (Added)
- **ItemsAdder 物品源**：可用 `ia-<命名空间>_<id>` 或 `itemsadder:<命名空间>:<id>` 引用 ItemsAdder 自定义物品。
- **Nexo 物品源**：可用 `nexo-<id>` 或 `nexo:<id>` 引用 Nexo 自定义物品。
- 两者均通过反射桥接（`ItemsAdderBridge` / `NexoBridge`），**未安装对应插件时自动跳过**，不产生硬依赖、不影响启动。
- `ItemSourceType` 新增 `ITEMSADDER` / `NEXO` 枚举项并注册到 `ItemSourceManager`。

### 说明 (Notes)
- 所有走 `ItemSourceManager.parseItemReference` 的调用方（如 QinhItems 的 `material` 底物、`consume` 消耗、`give` 等）**自动支持**上述新物品源，子插件无需改动或重新编译。
- `plugin.yml` softdepend 增加 `ItemsAdder`、`Nexo`（仅影响加载顺序）。

## [1.0.8] - 首个公开版本

- 统一物品源体系：原版 / CraftEngine / MMOItems / NeigeItems / QinhItems / MythicMobs / CustomFishing / MagicGem。
- 统一属性管道、自定义方块桥（CraftEngine）、ModelEngine 模型桥、脚本引擎（GraalVM）、经济/占位符桥等。
