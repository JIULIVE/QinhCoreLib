// 秦淮原生属性 · 暴击（演示 on_damage_dealt 钩子，公式可自由修改）
// 触发钩子：on_damage_dealt —— 玩家造成伤害时
// 可用变量：
//   ctx.get("damage")            当前伤害值
//   ctx.get("value")             玩家暴击率总值（0-1，0.3 = 30%）
//   ctx.get("critical_damage")   玩家暴击伤害总值（0.5 = 额外 +50%，物品可加）
//   ctx.get("victim")            受击实体
// 返回：暴击则放大后的伤害，否则原值。
// 公式：暴击倍率 = 1.5(基础) + 暴击伤害属性。改这两行即可调平衡。
function onDamageDealt() {
    var damage = ctx.get("damage");
    var rate = ctx.get("value");
    if (rate <= 0 || Math.random() >= rate) {
        return damage;
    }
    var bonus = ctx.get("critical_damage");
    if (!bonus || bonus < 0) bonus = 0;
    var multiplier = 1.5 + bonus;
    return damage * multiplier;
}
