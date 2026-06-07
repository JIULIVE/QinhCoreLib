# Qinh 体系 vs MMOItems vs NeigeItems 对比分析

> 说明：本对比基于当前工作区中可直接读取到的源码与 README/入口文件进行“静态审阅”。
> 由于 `QinhItems` / `QinhSkills` / `QinhStrengthen` 部分入口文件在当前目录中为空或未完整展开，本报告会把它们视为 **Qinh 生态待整合模块**，重点以 `QinhCoreLib` 与 `QinhForge` 作为当前已可见能力基线。

---

## 1. 一句话结论

- **Qinh 的方向是对的，而且更适合做“生态型平台”**：它不是单一物品插件，而是一个把 **物品源、动作系统、GUI、脚本、经济、数据库、Hologram、ModelEngine、CraftEngine、CustomCrops、MythicMobs、NeigeItems、MMOItems** 等能力统一起来的底座。
- **MMOItems 的强项** 是老牌、成熟、内容完整、对“物品构建/模板/词条/锻造/升级/装备交互”的传统 RPG 体验非常扎实。
- **NeigeItems 的强项** 是国内化动作系统、强脚本化、强表达式化、强可编排，尤其在 **Action/Condition/JS/Tree/Weight** 这类“逻辑编排能力”上很激进。
- **Qinh 当前最大的机会** 不在“复制它们”，而在于把它们的优势整合成一个 **更统一、更自由、更适合国内服主二次开发、更适合生态联动** 的平台。

如果目标是“比 NI 更强”，核心不是只在功能数量上堆，而是要做到：

1. **统一抽象更强**
2. **扩展机制更开放**
3. **跨插件联动更自然**
4. **对服主更易用，对开发者更友好，对玩家更有内容感**

---

## 2. 项目定位对比

### 2.1 Qinh 体系

从 `QinhCoreLib` 的入口可以看到，Qinh 不是单点功能插件，而是一个总中枢：

```0:0:D:[a]plugins[a].plugins\QinhCoreLib\src\main\kotlin\com\qinhuai\corelib\QinhCoreLib.kt
class QinhCoreLib : JavaPlugin() {
    override fun onEnable() {
        ServerCompat.validateServer(logger)?.let { reason ->
            logger.severe("[QinhCoreLib] $reason")
            server.pluginManager.disablePlugin(this)
            return
        }

        StartupReporter.reset()
        logger.info("§6[QinhCoreLib] §e秦淮核心库 v${pluginMeta.version} 启用中…")

        saveDefaultConfig()

        commandManager = QinhCloud.create(this)
        QCLCommands.register(commandManager)

        CustomGuiManager.init(this)
        StartupReporter.setGuiCount(CustomGuiManager.loadedGuiCount())

        moduleManager = ModuleManager()
        CoreModules.registerAll(moduleManager)
        moduleManager.loadAll()
```

它的定位更像：

- 一个 **RPG 生态运行时**
- 一个 **物品/动作/条件/脚本/GUI/经济/数据库/桥接层** 的统一平台
- 一个可供 `QinhItems / QinhSkills / QinhStrengthen / QinhForge` 共享的核心底座

### 2.2 MMOItems

MMOItems 仍然是经典的“物品系统主产品”。README 也直接说明它是官方仓库，并提供 API 依赖入口：

```0:0:D:[a]plugins[a].plugins\mmoitems-master (1)\mmoitems-master\README.md
Official repository for MMOItems

### Using MMOItems as dependency

Register the PhoenixDevelopment public repository:
...
<dependency>
    <groupId>net.Indyuce</groupId>
    <artifactId>MMOItems-API</artifactId>
    <version>6.9.5-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

它的重点仍然是：

- 物品模板
- 词条/属性
- 制作/锻造/升级/再铸
- 交互事件
- 兼容多个主流生态

### 2.3 NeigeItems

NeigeItems 更像“动作编排引擎 + 物品管理器 + NMS/Hook 层集合”。README 里公开强调：

- `ItemManager`
- `ItemPackManager`
- `ActionManager`
- `ActionContext`
- `BaseActionManager`
- 与 MythicMobs 联动

````0:0:D:[a]plugins[a].plugins\NeigeItems-Kotlin-main\NeigeItems-Kotlin-main\README.md
### 我想在我的插件里使用NI的动作系统

```java
// 继承一个BaseActionManager
public class ActionManager extends BaseActionManager {
    public ActionManager(@NotNull Plugin plugin) {
        super(plugin);
        // 加载一下NI里一些已有的js库
        // 这些库将反映在condition判断和js动作执行中
        loadJSLib("NeigeItems", "JavaScriptLib/lib.js");
    }
}
````

````

而主入口 `NeigeItems.java` 还体现出它对版本、NBT、类注入和环境兼容的高要求。

---

## 3. 架构层对比

## 3.1 Qinh 的架构特征

QinhCoreLib 的模块注册表非常清晰，说明它是按“能力域”拆分的：

```0:0:D:\[a]plugins\[a].plugins\QinhCoreLib\src\main\kotlin\com\qinhuai\corelib\CoreModules.kt
object CoreModules {
    fun registerAll(moduleManager: ModuleManager) {
        moduleManager.register(ConfigModule)
        moduleManager.register(DatabaseModule)
        moduleManager.register(ReflectionModule)
        moduleManager.register(ModelEngineModule)
        moduleManager.register(CustomCropsModule)
        moduleManager.register(CraftEngineModule)
        moduleManager.register(MythicMobsModule)
        moduleManager.register(NeigeItemsModule)
        moduleManager.register(MMOItemsModule)
        moduleManager.register(GuiModule)
        moduleManager.register(ActionModule)
        moduleManager.register(PdcModule)
        moduleManager.register(ItemModule)
        moduleManager.register(HologramModule)
        moduleManager.register(SchedulerModule)
        moduleManager.register(ConditionModule)
        moduleManager.register(ExpressionModule)
        moduleManager.register(EconomyModule)
        moduleManager.register(ScriptModule)
        moduleManager.register(CustomBlockModule)
        moduleManager.register(AssemblyModule)
    }
}
````

这说明 Qinh 的方向不是单线程，而是：

- **模块化平台**
- **服务定位**
- **桥接优先**
- **统一抽象优先**

### 3.2 MMOItems 的架构特征

MMOItems 的代码结构更像成熟产品的分层库：

- `API`
- `crafting`
- `trigger`
- `event`
- `player`
- `recipe`
- `item`
- `interaction`
- `util`
- `command`
- `world`

这种结构体现了它在“物品玩法”上很细，成熟度高，内部领域模型丰富。

### 3.3 NeigeItems 的架构特征

NeigeItems 的结构几乎是“动作编译器 + 动作运行时 + 低层平台兼容”三层：

- `action`
- `evaluator`
- `command`
- `annotation`
- `hooker`
- `calculate`
- `scan`
- `libs`
- `NMS`

这意味着它很强的不是“几个固定功能”，而是 **动态编排与执行**。

---

## 4. 功能维度对比

### 4.1 物品系统

#### MMOItems

优势：

- 传统 RPG 物品体系完整
- 物品模板、词条、升级、重铸、装备、武器交互成熟
- API 体系很完整，且历史久、生态大

#### NeigeItems

优势：

- `ItemManager` / `ItemPackManager` / `ItemUtils` 风格更适合国内开发者快速嵌入
- 更强调“包”和“动作联动”
- 动作系统与物品系统耦合更深

#### Qinh 当前

QinhCoreLib 里已经做了“统一物品桥”思路：

```0:0:D:[a]plugins[a].plugins\QinhCoreLib\src\main\kotlin\com\qinhuai\corelib\api\item\ItemManagerAPI.kt
class ItemManagerAPI private constructor() {
    private val byAlias = linkedMapOf<String, RegisteredModule>()

    fun register(plugin: org.bukkit.plugin.Plugin, module: ItemModule, vararg aliases: String) {
        registerByOwner(plugin.name, module, *aliases)
    }

    fun getHookItem(ref: String, player: Player?, amount: Int = 1): ItemStack? {
        val parsed = ItemReferenceParser.parse(ref) ?: return null
        val stack = resolveStack(parsed, player) ?: return null
        stack.amount = amount.coerceAtLeast(1)
        return stack
    }
```

这比“只做某一种来源”的路线更先进，因为它允许：

- 统一引用格式
- 多后端物品接入
- 支持 `material / type / mm / mi / qinhitems / qinhforge` 等统一解析

**差距判断**：

- 对比 MMOItems，Qinh 目前更像“聚合器”，还不是“内容最深的单体物品体系”。
- 对比 NI，Qinh 在“统一接入能力”上更有潜力，但动作深度、动作编排语言与生态成熟度还需要补。

---

### 4.2 动作系统 / 条件系统 / 脚本系统

#### NeigeItems

这是它的最强项之一。README 就明确展示了：

- `ActionManager`
- `compile(...)`
- `ActionContext`
- `eval(context)`
- JS 库加载

而源码树里有大量 evaluator/action 变体，说明它的动作系统很细，很像一个 DSL 引擎。

#### Qinh

QinhCoreLib 已有：

- `action`
- `condition`
- `expression`
- `script`
- `customgui` 的动作执行
- `QinhScriptBridge` / `GraalJavaScriptEngine`

这说明 Qinh 已经开始向“统一脚本运行时 + 条件/动作”发展。

**但是当前差距**：

1. Qinh 的动作系统还未从源码层看出像 NI 那样“超高粒度的动作类型矩阵”。
2. Qinh 的脚本/表达式/动作需要进一步统一成一个更强的 DSL 入口。
3. Qinh 若想超越 NI，需要让 **动作不仅能执行，还能被可视化编辑、可复用、可组合、可调试、可导出导入**。

---

### 4.3 GUI 与交互体验

QinhCoreLib 的 `customgui` 目录很大，说明已经在构建较强的 GUI 框架：

- `CustomGui`
- `DynamicGui`
- `GuiPaginationEntry`
- `PlaceholderManager`
- `CustomGuiListener`
- `GuiDataProvider`
- `GuiItemRenderer`

这在“服主可用性”上是重大优势。

MMOItems 的 GUI 主要服务于物品与工艺界面；NI 的 GUI 更多服务于其动作与编辑/交互体系。

**Qinh 的机会**：

- 让 GUI 成为“所有系统的统一入口”
- 物品、锻造、技能、强化、活动、商店、任务都能用同一套 GUI 语言驱动

这会比单纯的“物品界面”更像生态平台。

---

### 4.4 经济、数据库、占位符、调度、PDC、反射

QinhCoreLib 具备明显的平台底座特征：

- `DatabaseManager`
- `EconomyBridge`
- `PdcService`
- `PlaceholderAPI` 对接
- `TaskScheduler`
- `ReflectionBridge`

这些是“生态插件”的必需品，不是点缀。

对比而言：

- MMOItems 偏物品内容核心
- NI 偏逻辑执行与物品管理
- **Qinh 的定位更接近综合平台**

这意味着 Qinh 可以在未来做到：

- 跨插件数据统一存储
- 玩家进度/词条/装备/冷却/任务状态统一持久化
- 统一消息、占位符、脚本调用链

---

## 5. 联动能力对比

### 5.1 Qinh 的桥接策略很强

QinhCoreLib 明确内置了多桥：

- `MMOItemsBridge`
- `NeigeItemsBridge`
- `MythicMobsBridge`
- `ModelEngineBridge`
- `CraftEngineBridge`
- `CustomCropsBridge`
- `CustomFishingBridge`
- `MagicGemBridge`

并且在 `ItemModule` 卸载时会统一清理：

```0:0:D:[a]plugins[a].plugins\QinhCoreLib\src\main\kotlin\com\qinhuai\corelib\CoreModules.kt
object ItemModule : AbstractModule("Item") {
    override val priority: Int = 50
    override fun load() {
        ItemSourceBootstrap.registerAll()
        ItemManagerBootstrap.onItemSourcesReady()
    }

    override fun unload() {
        ItemManagerBootstrap.unloadAll()
        ItemSourceBootstrap.unregisterAll()
        com.qinhuai.corelib.magicgem.MagicGemBridge.clear()
        com.qinhuai.corelib.customfishing.CustomFishingManager.clear()
        com.qinhuai.corelib.mythicmobs.MythicMobsManager.clear()
    }
}
```

这说明 Qinh 在设计上把“联动”当成一等公民。

### 5.2 MMOItems 的联动

MMOItems 的联动范围广，但更多是为了让自己的物品体系服务于其他系统。
它的接口丰富，但通常围绕“MMOItems 物品本体”展开。

### 5.3 NeigeItems 的联动

NI 的联动能力强在：

- 动作系统可嵌入其他系统
- MythicMobs 联动
- NMS/版本适配
- 物品包与动作编排

但它更像“动作中枢”，生态统一层并没有 Qinh 当前这么明确。

---

## 6. 服主、开发者、玩家三个视角的差距分析

### 6.1 对服主

#### MMOItems

优点：成熟、稳定、内容多、教程多。
缺点：复杂，配置成本高，重度依赖老用户经验。

#### NeigeItems

优点：灵活、脚本化、可玩性高。
缺点：学习成本高，动作体系过强时容易失控。

#### Qinh

潜力：

- 如果把物品、技能、强化、锻造统一成一套生态流程，会比单独插件体验更顺。
- 如果有更好的 GUI 和文档，国内服主会更喜欢。

当前缺口：

- 还需要“开箱即用”的范式
- 需要更强的默认模板/示例/教学资源
- 需要更强的配置校验与错误提示

---

### 6.2 对开发者

#### MMOItems

API 可靠，但偏传统。

#### NeigeItems

开发者自由度高，但系统复杂，且需要理解其动作与 evaluator 模型。

#### Qinh

Qinh 的优势在于它已经具备“平台化 API”的雏形：

- `ItemManagerAPI`
- `ItemModule`
- `ItemSource`
- `ScriptBridge`
- `CustomGui`
- `EconomyBridge`

这非常适合国内开发者二次开发，因为它更像“基础设施”而不是“单功能插件”。

当前缺口：

- 还需要统一的 SDK 风格文档
- 更清晰的扩展点生命周期
- 更少的实现细节泄露到 API 层
- 更强的示例工程与模块模板

---

### 6.3 对玩家

玩家不关心框架，只关心：

- 好不好玩
- 是否顺手
- 体验是否丝滑
- 是否有成长感和收集感
- 是否有内容联动感

Qinh 若要赢，必须把：

- 物品系统
- 强化系统
- 技能系统
- 锻造系统
- 经济系统
- 任务/副本/活动

真正串成一个“有连贯成长路径”的 RPG 生态。

---

## 7. Qinh 当前相对 NI / MMOItems 的主要差距

### 7.1 相对 MMOItems 的差距

1. **传统 RPG 物品内容沉淀不足**
   - MMOItems 在装备、词条、套装、升级、再铸、掉落、交互方面沉淀多年。

2. **玩法标准化程度更高**
   - MMOItems 的每个概念都非常固定，社区教程极多。

3. **生态兼容历史更长**
   - 服主对它的预期非常明确。

### 7.2 相对 NeigeItems 的差距

1. **动作 DSL 的粒度与成熟度**
   - NI 的 action / evaluator / condition / JS 编排非常深入。

2. **编辑与编译链更完整**
   - NI 很强调 compile -> context -> eval 的运行模型。

3. **版本与底层兼容手段更激进**
   - NI 在 NMS、注入、兼容性方面的工程投入很重。

4. **对国内用户的心智占位更强**
   - NI 已经有一套国内用户认知。

---

## 8. Qinh 已经拥有的优势

这是最重要的部分：Qinh 不是从零开始。

### 8.1 统一物品引用层

Qinh 已经在做 `ItemManagerAPI` 这种统一物品入口，这非常有前途。

### 8.2 模块化平台

`CoreModules.registerAll(...)` 的设计说明 Qinh 天然适合生态扩展。

### 8.3 多桥接共存

同时兼容 MMOItems / NeigeItems / MythicMobs / ModelEngine / CraftEngine / CustomCrops / CustomFishing / MagicGem，是很强的战略点。

### 8.4 自带脚本与表达式能力

`GraalJS` + `ExpressionEngine` + `ConditionSystem` + `ActionSystem` 已经在向“可编排游戏逻辑平台”靠近。

### 8.5 GUI 框架具备平台属性

不是简单菜单，而是能承载动态分页、条件、占位符、动作回调的界面层。

---

## 9. 如果要“超越 NI”，Qinh 应该怎么做

### 9.1 产品层战略

把 Qinh 定义成：

> 一个面向国内外服主与开发者的 RPG 生态操作系统，而不是单个物品插件。

要有三层产品线：

1. **QinhCoreLib**：底座
2. **QinhItems / QinhSkills / QinhStrengthen / QinhForge**：业务插件
3. **QinhStudio / 配置工具 / 可视化编辑器**：生产力工具

### 9.2 功能层重点

#### 必做 1：统一 DSL

把 action / condition / expression / script / trigger 统一成一套“可解释、可编排、可调试”的语言。

#### 必做 2：可视化编辑

服主不想天天手写复杂配置。
需要：

- GUI 配置编辑
- 模板导入导出
- 调试预览
- 校验提示

#### 必做 3：统一物品协议

让所有物品来源都能通过统一引用格式工作。
例如：

- `mi:sword_x`
- `mm:dragon_blade`
- `qi:ancient_relic`
- `vanilla:diamond_sword`

#### 必做 4：统一数据模型

装备、强化、技能、锻造、任务、掉落、商店、公会，都应该以统一的 player profile / item meta / progression state 管理。

#### 必做 5：插件间联动编排

把联动写成标准化模块，而不是每个插件各写各的 Hook。

### 9.3 体验层重点

- 默认模板必须好用
- 错误信息必须中文友好、英文友好
- 配置热重载要稳定
- 文档要有“从 0 到 1 的完整案例”
- 给服主一套“开箱即用”的 RPG 生态

---

## 10. Qinh 发展路线建议

### 第一阶段：追平

目标：让 Qinh 在基础能力上至少和 NI、MMOItems 的常用能力对齐。

- 物品引用统一
- 动作系统稳定
- GUI 体系完善
- 经济/占位符/数据库/脚本联通
- MythicMobs/ModelEngine 等桥接稳定

### 第二阶段：超越

目标：在“跨系统统一能力”和“可视化编排能力”上超越 NI。

- 统一 action DSL
- 引入编辑器/调试器
- 统一任务/副本/技能/强化/锻造的底层 state
- 对外输出更强 API 与 SDK

### 第三阶段：生态化

目标：成为国内外服主和开发者都愿意围绕它做内容的平台。

- 插件市场
- 模块市场
- 配置模板市场
- 社区案例库
- 国外社区英文文档同步

---

## 11. 最终判断

### 谁更强？

- **MMOItems**：传统 RPG 物品体系更成熟
- **NeigeItems**：动作编排、灵活性、国内辨识度很强
- **Qinh**：如果把现有模块继续完善，**最有机会成为“更大的平台型生态”**

### Qinh 的最强定位

不是“又一个 MMOItems”，也不是“又一个 NeigeItems”，而是：

> 一个把物品、强化、技能、锻造、脚本、GUI、经济、数据库、模型、方块、作物、钓鱼、MythicMobs 联动全部整合起来的 RPG 生态底座。

### 关键建议一句话版

**不要只追求功能列表更长，要追求“统一抽象更强、扩展更自由、联动更自然、编辑更简单、内容更好玩”。**

---

## 12. Qinh 缺失功能清单 v1

> 这一节是“研发 TODO 版”。目标不是评价已有代码，而是直接指出：Qinh 目前还缺什么、还没做什么、下一步应该补什么。

### 12.1 `QinhCoreLib` 缺失项

#### 1) 模块治理能力不足
- 目前偏注册式，缺少明确依赖图
- 缺少模块健康状态与诊断结果
- 缺少按依赖顺序的拓扑加载/卸载
- 缺少失败模块隔离和回滚机制
- 缺少 reload diff / warm reload / hot reload 分层

#### 2) 动作系统还不是 DSL
- 缺少动作编译器
- 缺少动作树、分支、循环、中断、跳转
- 缺少动作 schema / 参数校验
- 缺少动作执行 trace
- 缺少动作可视化编辑与调试

#### 3) 条件系统太薄
- 缺少条件参数定义
- 缺少条件错误定位
- 缺少条件优先级与缓存
- 缺少表达式级别的条件语法
- 缺少条件调试输出

#### 4) 脚本体系还不够产品化
- 缺少脚本依赖图
- 缺少脚本热更新事件
- 缺少脚本沙箱/权限策略
- 缺少脚本性能统计
- 缺少脚本调用链追踪
- 缺少脚本与配置的统一引用规范

#### 5) 物品桥接还没完成统一协议
- 缺少统一 item spec
- 缺少统一 item meta schema
- 缺少跨来源参数规范
- 缺少 resolve/build/finalize 生命周期
- 缺少解析失败诊断信息

#### 6) 调试与可观测性不足
- 缺少统一 debug 模式
- 缺少 trace 日志
- 缺少运行状态面板
- 缺少内建性能采样和瓶颈分析
- 缺少系统级 health report

### 12.2 `QinhItems` 缺失项

#### 1) 物品内容层不完整
- 缺少完整模板体系
- 缺少词条/属性/套装标准
- 缺少稀有度与成长曲线
- 缺少掉落组与来源组标准
- 缺少装备生命周期规则

#### 2) 编辑器闭环不足
- 缺少真正的可视化物品编辑器
- 缺少模板导入/导出
- 缺少字段校验与错误定位
- 缺少批量迁移工具
- 缺少预览与试算

#### 3) 生态标准没收拢
- 缺少与 QinhSkills / QinhStrengthen / QinhForge 的统一 state
- 缺少统一物品成长协议
- 缺少玩家装备状态模型

### 12.3 `QinhSkills` 缺失项

#### 1) 技能状态机不完整
- 缺少前摇/后摇/打断/霸体/公共冷却
- 缺少施法阶段可配置化
- 缺少技能状态迁移模型
- 缺少技能层级与连携树

#### 2) 技能定义不够统一
- 缺少技能 schema
- 缺少技能参数标准
- 缺少技能结果标准化返回
- 缺少技能执行追踪

#### 3) 技能 UI 与编辑器不足
- 缺少可视化技能编辑器
- 缺少技能树展示
- 缺少技能配置校验
- 缺少技能调试回放

### 12.4 `QinhStrengthen` 缺失项

#### 1) 强化链条不完整
- 缺少等级曲线
- 缺少保底机制
- 缺少失败降级/破碎/保护机制
- 缺少幸运/祝福/诅咒等变量
- 缺少材料品质对结果的影响模型

#### 2) 强化结果模型不足
- 缺少统一强化结果对象
- 缺少副作用与概率系统
- 缺少强化日志与审计
- 缺少可插拔规则引擎

#### 3) 强化体验还不够“产品化”
- 缺少动画/过程反馈
- 缺少强化预览
- 缺少风险提示
- 缺少批量强化与自动化策略

### 12.5 `QinhForge` 缺失项

#### 1) 配方体系太基础
- 缺少配方类型扩展
- 缺少分组/权重/链式配方
- 缺少条件配方
- 缺少配方版本管理

#### 2) 结果规则不足
- 缺少成功率/保底/爆率/特殊产出
- 缺少属性继承/洗练/重铸
- 缺少多输出规则
- 缺少失败分支与副产品

#### 3) 锻造交互不足
- 缺少完整的站点状态机
- 缺少过程 UI 和进度反馈
- 缺少审计与日志
- 缺少锻造链路回放

### 12.6 平台级共同缺失

- 缺少统一玩家数据模型
- 缺少统一的版本迁移机制
- 缺少统一的配置 schema
- 缺少统一的错误码体系
- 缺少统一的调试与 profiling
- 缺少可视化编辑器矩阵
- 缺少插件市场/模板市场/模块市场
- 缺少面向服主的“默认最佳实践”模板

---

## 13. Qinh 超越 NI 的技术路线图

> 这一节回答“怎么赢”，不是“哪里不够”。核心原则是：**先统一，再增强；先可调试，再可扩展；先做平台能力，再做炫技功能。**

### 13.1 第一阶段：打地基

目标是让 Qinh 具备稳定的生态底座。

#### 必须完成
1. 统一物品协议
2. 统一动作/条件/脚本引用格式
3. 统一玩家进度和物品状态模型
4. 统一模块生命周期和健康状态
5. 统一 debug/trace 输出

#### 交付结果
- 服主能稳定部署
- 开发者能稳定接入
- 模块间不会频繁互相污染
- 出问题能快速定位

### 13.2 第二阶段：做强编排层

目标是超越 NI 最强项——动作编排。

#### 必须完成
1. 动作 DSL 化
2. 条件树化
3. 脚本与动作统一上下文
4. 引入可视化编排器
5. 引入动作回放和调试器

#### 交付结果
- 不是“写配置”，而是“搭逻辑”
- 服主能自己拼复杂玩法
- 开发者能复用逻辑块
- 玩家能感受到内容深度

### 13.3 第三阶段：做强内容层

目标是超越 MMOItems 的传统 RPG 内容体验。

#### 必须完成
1. 物品模板体系
2. 词条/套装/稀有度/成长线
3. 强化与锻造完整闭环
4. 技能与装备联动
5. 任务/副本/掉落/经济统一

#### 交付结果
- 生态不只是“能用”，而是“好玩”
- 玩家会愿意长期刷、养成、交易、研究

### 13.4 第四阶段：做生态平台

目标是让 Qinh 变成“一个系列”，而不是“一个插件包”。

#### 必须完成
1. 官方模板库
2. 插件/模块市场
3. 示例工程库
4. 英文/中文双语文档
5. 社区案例与最佳实践库

#### 交付结果
- 服主更容易上手
- 开发者更愿意贡献模块
- 玩家能在更多服务器看到 Qinh 生态

---

## 14. 逐文件对照表（Qinh vs MI vs NI）

> 这一节把当前已经看到的核心源码，直接映射到竞争对手的对应能力。

### 14.1 核心底座

| Qinh 文件/模块 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `QinhCoreLib.kt` | 核心启动器 | MMOItems 主入口 | NI 主入口 | 已有平台底座，但还偏框架 |
| `CoreModules.kt` | 模块注册 | MMOItems 内部分层 | NI 内部分层 | 模块化思路正确，治理不足 |
| `ModuleManager.kt` | 模块生命周期 | 近似 MI 的内部管理 | 近似 NI 的加载管理 | 缺依赖图/健康态 |
| `StartupReporter.kt` | 启动报告 | 对标少量日志模块 | 对标启动输出 | 有，但可观测性不够 |

### 14.2 物品与来源

| Qinh 文件/模块 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `ItemManagerAPI.kt` | 统一物品入口 | MMOItems API | NI ItemManager | 方向先进，但协议不够完整 |
| `ItemSourceManager.kt` | 物品来源管理 | MMOItems 来源体系 | NI 物品包体系 | 有聚合能力，缺统一标准 |
| `ItemReferenceParser.kt` | 引用解析 | 对标物品 ID 解析 | 对标 item ref 解析 | 还需更强诊断和语法 |
| `MMOItemsItemSource.kt` | MMOItems 接入 | 直接对标 | 间接对标 | 接入层已存在 |
| `NeigeItemsItemSource.kt` | NI 接入 | 直接对标 | 直接对标 | 说明平台思路正确 |

### 14.3 动作/条件/脚本

| Qinh 文件/模块 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `ActionSystem.kt` | 动作系统 | MI 事件/触发 | NI ActionManager | 还只是骨架 |
| `ConditionSystem.kt` | 条件系统 | MI 条件/触发 | NI Condition | 还缺 schema 和调试 |
| `QinhScriptBridge.kt` | 脚本执行 | MI 部分脚本功能 | NI JS 动作 | 具备桥接，缺产品化 |
| `ExpressionEngine.kt` | 表达式引擎 | MI 属性表达 | NI evaluator | 可继续扩展 |

### 14.4 GUI 与交互

| Qinh 文件/模块 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `CustomGuiManager.kt` | GUI 管理 | MI 工艺/商店界面 | NI 编辑/交互界面 | 有平台潜力 |
| `DynamicGui.kt` | 动态 GUI | MI 动态面板 | NI 动态配置 | 很适合做编辑器底座 |
| `PlaceholderManager.kt` | 占位符 | MI 数值展示 | NI 条件显示 | 有，但可继续统一 |

### 14.5 经济/数据库/基础设施

| Qinh 文件/模块 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `DatabaseManager.kt` | 数据存储 | MI 持久化 | NI 持久化 | 有基础，但需统一数据模型 |
| `EconomyBridge.kt` | 经济对接 | MI 经济联动 | NI 资源消耗 | 已具备平台价值 |
| `PdcService.kt` | 元数据 | MI 物品数据 | NI 物品数据 | 需统一 schema |
| `TaskScheduler.kt` | 调度 | MI 任务处理 | NI 定时逻辑 | 基础存在 |

### 14.6 业务插件

| Qinh 项目 | 作用 | 对标 MI | 对标 NI | 当前状态 |
|---|---|---|---|---|
| `QinhItems` | 物品系统 | MMOItems | NI Items | 最需要继续补全 |
| `QinhSkills` | 技能系统 | MMOItems 技能/触发 | NI 动作/技能 | 有雏形，需状态机与编辑器 |
| `QinhStrengthen` | 强化系统 | MMOItems 强化 | NI 词条/动作联动 | 入口有了，规则不够深 |
| `QinhForge` | 锻造系统 | MMOItems Forge | NI 配方/动作链 | 可用，但还不够强 |

### 14.7 结论性对照

- **MI 强在“完整传统 RPG 内容”**
- **NI 强在“动作/脚本/编排”**
- **Qinh 强在“统一平台化方向”**

但 Qinh 现在最关键的短板是：

1. 统一协议不够完整
2. 动作与条件还不是 DSL
3. 技能、强化、锻造还没形成完整闭环
4. 调试与编辑器还不够产品化
5. 生态文档与模板还不够丰富

---

## 15. 最后的研发结论

如果从源码级视角看，Qinh 的胜负手不是“多写几个功能”，而是：

- **把底座做成可治理的平台**
- **把动作/条件/脚本做成统一 DSL**
- **把物品/技能/强化/锻造做成统一数据模型**
- **把 GUI 做成生产力工具**
- **把文档与模板做成生态入口**

一旦这些补齐，Qinh 就不只是“对标”MI 和 NI，而是有机会做成一种新的 RPG 插件范式。



---

## 12. 附：本次审阅中最值得保留的 Qinh 设计点

1. `CoreModules.registerAll(...)` 模块化注册
2. `ItemManagerAPI` 统一物品入口
3. `ItemSourceManager` 多来源物品解析
4. `CustomGuiManager` 动态 GUI 体系
5. `QinhScriptBridge` 与脚本执行层
6. `EconomyBridge`、`DatabaseManager` 等基础设施
7. 多插件桥接统一管理
8. 启动时的环境检查与报告机制

---

我继续做了源码级深挖，结论很明确：Qinh 已经有平台雏形，但很多关键能力还是“框架化存在、产品化不足”。
也就是说，Qinh 不是没方向，而是还缺一层真正能打的“完成度”。

我先给你一个简明结论，然后列出按源码看出来的不足和没做完的地方。

先说总判断
你们现在最强的部分
已经有 CoreLib 平台底座
有 统一物品入口 ItemManagerAPI
有 脚本桥接 QinhScriptBridge
有 GUI 系统
有 跨插件桥接
有 QinhItems / QinhSkills / QinhStrengthen / QinhForge 的业务分层意识
但目前最大的问题
Qinh 的很多核心系统属于：

接口有了
调用链有了
模块名有了
但 深度、约束、编辑器、调试、完整生命周期、标准化 DSL 还不够
所以它现在更像：

“一个已经搭好骨架的平台项目”，
而不是
“一个已经能稳定交付给服主长期运营的生态产品”。

一、源码级发现的核心不足

1. QinhCoreLib 的模块系统还偏“注册式”，不够“治理式”
   你现在的模块管理
   class ModuleManager {
   private val modules = ConcurrentHashMap<String, Module>()
   private val loadedModules = mutableListOf<Module>()
   fun loadAll() {
   val sortedModules = modules.values.sortedBy { it.priority }
   sortedModules.forEach { module ->
   try {
   module.load()
   loadedModules.add(module)
   } catch (e: Exception) {
   ...
   }
   }
   loadedModules.forEach { module ->
   try {
   module.enable()
   } catch (e: Exception) {
   ...
   }
   }
   }
   }
   问题
   只有 load / enable / disable / unload
   没有：
   依赖声明
   自动拓扑排序
   缺失依赖提示
   模块健康状态
   模块热重载粒度控制
   回滚机制
   失败隔离策略
   这意味着什么
   如果以后 Qinh 生态大了，模块一多，就会出现：

先后顺序不稳
重载后状态残留
某模块失败影响整条链
开发者不知道模块到底为什么没生效
对比 NI / MMOItems
NI 和 MMOItems 都不是简单注册式，而是有更强的内部功能闭环
Qinh 目前底座更灵活，但治理能力还不够强
建议
你要补的是：

模块依赖声明
生命周期状态机
healthy / degraded / failed / disabled 状态
模块健康检测报告
reload diff 机制 2. ActionSystem 过于简化，还不是 NI 那种“动作语言”
当前源码
interface Action {
val id: String
fun execute(context: ActionContext)
}
class ActionPipeline {
private val actions = mutableListOf<Action>()

    fun execute(context: ActionContext) {
        actions.forEach { action ->
            action.execute(context)
        }
    }

}
问题
这目前只是：

动作对象
顺序执行
少量变量存取
但还没有：

条件分支
条件短路
多返回类型
嵌套动作树
循环
重试
并行
中断
跳转
标签
参数解析器
动作编译器
动作反序列化
动作调试器
对比 NI
NI 的动作系统是非常强的，它不是单纯 execute()，而是：

编译
运行上下文
多种 evaluator
JS 动作
Tree / Weight / Parse / Raw 类型
条件与动作深度耦合
结论
Qinh 现在有动作系统雏形，但没有“动作 DSL 化”。

建议
你需要把动作层升级为：

compile -> validate -> optimize -> execute
支持 YAML / JSON / Groovy / JS / DSL
支持可视化动作树
支持动作调试输出 3. ConditionSystem 也还是骨架级
interface Condition {
val id: String
fun evaluate(context: ConditionContext): Boolean
}
问题
目前只是：

条件接口
条件注册
AND/OR/NOT 组合
但缺少：

条件参数 schema
条件类型推导
条件错误定位
条件短路调试
条件的可视化表达
条件优先级
条件缓存
条件表达式解析器
这会造成什么
服主写复杂逻辑时会很痛苦，开发者也很难排查：

为什么条件没命中
是哪一项失败
变量有没有传对
条件字段名是否错了4. QinhScriptBridge 有桥，但还不够“产品级脚本平台”
fun execute(reference: String, context: ScriptContext): ScriptExecutionResult {
if (!isAvailable()) {
return ScriptExecutionResult.fail("JavaScript 不可用（需 Paper/Purpur libraries 加载 GraalJS）")
}
val call = ScriptRefParser.parse(reference, config.defaultFunction)
if (call.logicalPath.isBlank()) {
return ScriptExecutionResult.fail("脚本路径为空")
}
val source = repository.find(call.logicalPath)
?: return ScriptExecutionResult.fail("找不到脚本: ${call.logicalPath}")
return engine!!.execute(source, call.functionName, context)
}
现有优点
支持命名空间脚本
支持插件脚本目录
支持默认函数
支持脚本执行结果对象
不足
没有脚本热更新事件链
没有脚本依赖分析
没有脚本沙箱能力展示
没有脚本权限控制体系
没有脚本调用图
没有脚本性能统计
没有脚本错误回溯到业务配置
没有脚本 IDE 友好约定
对比 NI
NI 的 JS 系统更“嵌入动作体系”，你这边更偏“独立脚本桥”。

建议
如果要超过 NI，脚本必须变成：

行为可调试
错误可定位
函数可发现
命名空间可管理
与动作/条件统一编排5. ItemManagerAPI 方向对，但统一标准还不完整
fun getHookItem(ref: String, player: Player?, amount: Int = 1): ItemStack? {
val parsed = ItemReferenceParser.parse(ref) ?: return null
val stack = resolveStack(parsed, player) ?: return null
stack.amount = amount.coerceAtLeast(1)
return stack
}
现在的优点
已经有统一引用格式
支持多个来源
支持模块注册
允许参数化构建
但问题是
引用协议还不够完整
来源间参数格式不统一
未见强类型元数据协议
未见物品解析失败的诊断信息体系
未见统一的 item spec schema
未见 item capability 机制
与 MMOItems / NI 的差异
它们通常都有比较明确的物品对象模型。
Qinh 目前更像“统一入口”，但“统一物品模型”还不够强。

结论
Qinh 当前最该补的是：

ItemSpec
ItemMetaSchema
ItemCapability
ItemSource contract
parse -> validate -> build -> decorate -> finalize 流程6. QinhForge 功能已经有了，但锻造深度还明显不足
fun craft(player: Player, stationId: String, recipeId: String): ForgeResult {
...
if (recipe.cost > 0.0 && !EconomyBridge.has(player, recipe.cost, null, null)) {
return ForgeResult(ForgeResultCode.NO_MONEY)
}
...
val outputs = buildOutputs(recipe)
当前已经有
站点
配方
材料判断
经济扣费
输出
事件
完成动作
但缺少的很关键
强化失败分支
成功率/保底/祝福/幸运/诅咒
分层锻造链
可插拔材料规则
可插拔产物规则
物品属性继承规则
词条洗练
成功动画/过程 UI
配方条件系统
配方权重
配方分组
锻造日志/审计
锻造结果可视化
对比 MMOItems
MMOItems 的传统锻造/升级链条通常更完整。
QinhForge 现在像“可用”，但还没到“内容中台”。

你最该补的
配方类型扩展
结果规则引擎
保底系统
失败惩罚系统
强化词条继承系统
锻造 GUI 交互深度7. QinhItems 还没完全形成“真正的物品产品”
从入口看：

ItemSourceManager.register(QinhItemsItemSource())
ItemManagerAPI.instance.register(this, QinhItemsItemModule, "qinhitems", "qi", "QinhItems")
val loaded = QinhItemRegistry.reload()
...
val actions = ActionTableRegistry.reload()
QinhSkillsLinker.requestHandlerRegistration(force = true)
当前优点
有注册逻辑
有动作表
有编辑器痕迹
有资源包目录
有技能联动
有 gem 联动
有 demo bridge
但从源码入口看，明显还欠这些：
完整物品模板体系
物品属性与词条规范
套装系统
装备成长链
稀有度/品质系统
掉落组系统
多套来源物品整合规范
数据迁移工具
配置校验工具
编辑器真正的可视化闭环
结论
QinhItems 现在更像：

“Qinh 生态里的物品核心模块原型”，
还不是一个对标 MMOItems 的完整商业级产品。

8. QinhSkills 已经比单纯脚本强，但技能域还不够大
   skillRouter = SkillRouter(SkillMap())
   profileStore = PlayerProfileStore(this)
   val comboTracker = ComboTracker()
   val scriptRunner = ScriptRunner()
   castService = SkillCastService(this, profileStore, skillRouter, CastGate(comboTracker), scriptRunner)
   优点
   有技能路由
   有玩家档案
   有连招追踪
   有脚本执行
   有动作条
   有 placeholder
   有 Mythic 适配
   有 QinhItems 联动
   不足
   技能定义层没看到足够强的 schema
   技能冷却、资源、前摇、后摇、打断、霸体等结构不够明确
   技能组合树/连携树不够明显
   技能 UI/编辑器不够完整
   技能数据版本迁移未见强体系
   技能效果不是统一的 effect model
   对比 NI 的动作/技能联动
   NI 在动作表达上更细，你这边还偏“技能运行管线”。

建议
QinhSkills 要想成为强项，必须补：

技能定义 schema
技能状态机
资源消耗模型
前后摇/冷却/公共冷却
施法打断规则
技能连携系统
技能可视化编辑器9. QinhStrengthen 目前只是“强化入口”，不是强化体系
StrengthenBackendRegistry.reload(config)
QiBridge.probe()
QiBridge.registerHandler(this, StrengthenOpenHandler)
现有能力
默认强化方案
GUI
站点/配方概念
后端注册
QI 桥接
还没做深的地方
强化等级曲线
失败保护
祝福/幸运/诅咒
分段强化机制
材料消耗策略
强化结果可视化
强化日志追踪
强化副作用
强化词条重铸
破碎/降级策略
结论
现在是入口，不是体系。

二、从源码看，Qinh 还没做完的“产品级能力”
下面这些不是“代码片段不足”，而是整条产品线还没完成。

1. 统一数据模型没完全建立
   目前不同模块各自有自己的：

recipe
action
skill
item
forge
profile
但缺少跨域统一的：

PlayerProgress
ItemSpec
SkillSpec
ForgeSpec
EffectSpec
RequirementSpec
如果不统一，后面生态一大就很难维护。

2. 版本迁移体系不完整
   现在能看到重载、注册、清理，但没看到：

配置版本号
数据升级脚本
旧配置自动迁移
兼容层策略
deprecated 字段映射
这是长期运营插件最容易翻车的地方。

3. 调试与诊断工具不足
   你们有不少系统，但没看到足够强的：

debug mode
trace log
condition 命中链
action 执行链
skill cast 过程链
forge 结算链
item resolve 链
对服主来说，好不好排错非常关键。

4. 编辑器/可视化工具不足
   从源码看，Qinh 已经有 GUI 基础，但还没有形成真正的：

物品编辑器
技能编辑器
配方编辑器
条件编辑器
动作编辑器
脚本绑定编辑器
这是 Qinh 真正能超过 NI 的关键之一。

5. 文档与标准缺口
   源码很强不等于生态强。
   你们还需要：

统一 API 文档
配置规范文档
示例模板
错误码说明
迁移指南
开发者接入指南
三、如果按“要超越 NI”来排优先级，我建议你们先补这些
第一优先级
统一数据模型
统一物品协议
统一动作/条件 DSL
调试日志链
配置校验与报错体系
第二优先级
技能状态机
强化体系完整化
锻造结果规则引擎
物品模板/词条/套装
脚本与动作完全联动
第三优先级
可视化编辑器
模板市场
迁移工具
性能监控
插件生态 SDK
四、我给你的最直接判断
如果只看现在源码：

Qinh 的架构野心已经超过了很多单体插件
但 完成度还不够
最危险的不是“没有功能”，而是“功能之间还没完全统一”
也就是说，Qinh 现在最大问题不是缺一个功能，而是缺：

一个真正把所有功能串起来的“统一中枢语言”。

这个中枢语言可以是：

统一 item spec
统一 action DSL
统一 condition schema
统一 skill pipeline
统一 forge recipe model
只要这个统一层做成了，Qinh 就不是在追 NI，而是在换赛道。
