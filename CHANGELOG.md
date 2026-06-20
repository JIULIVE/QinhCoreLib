# 更新日志 / Changelog

本项目遵循语义化版本（Semantic Versioning）。

## [1.2.0] - 2026-06-21

### 新增 (Added)
- **原生属性系统（native）**：内置原生属性后端，**不装任何属性插件**也能用全套属性与伤害结算（攻击 / 暴击 / 防御 / 闪避 / 元素等）。
  - `attributes.yml`：自定义属性（大类 / 伤害类型 / 上下限 / 战力权重）+ **JS 钩子**（`on_damage_dealt` / `on_damage_taken` / `on_kill` / `on_tick` / `on_equip` / `on_unequip`），脚本放 `scripts/attributes/*.js`。
  - `elements.yml`：**元素系统**，每个元素自动生成「伤害 / 增伤 / 抗性」三属性，支持五行相生相克。
  - `attribute.backend` 可切换：`native`（默认）/ `attributeplus` / `auto`，全生态统一从此读取。
- **多语言 i18n**：新增 `Lang` 系统，所有提示文本读取 `lang/<语言>/*.yml`，缺失键自动回退 `en_US`；`config.yml` 新增 `language`。
  - 内置七种语言：`zh_cn` / `en_US` / `zh_tw` / `ru_RU` / `fr_FR` / `vi_VN` / `es_ES`。
  - 属性显示名按语言加载（`AttributeLang` 语言感知）：`lang/<语言>/attributes.yml` 译名，根 `lang/attributes.yml` 作图标 / 单位基准层。

### 说明 (Notes)
- 服主可直接编辑数据目录下的 `lang/<语言>/*.yml` 修改任意文案，无需改源码或重新编译。
- 源码不含注释；面向用户的字符串全部抽到语言文件统一管理。

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
