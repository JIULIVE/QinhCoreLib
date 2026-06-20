// 秦淮原生属性 · 防御减伤（仿 MythicLib 自然减伤手感，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值
//   ctx.get("value")     玩家防御总值（已分源聚合，物品/套装等相加）
//   ctx.get("attacker")  攻击者实体（可能为 null，如摔落/火焰）
// 返回：减免后的新伤害值。
// 默认曲线：100 防御 ≈ 减伤 50%，递减不封顶。改这一行即可调平衡。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var defense = ctx.get("value");
    if (defense <= 0) return damage;
    return Math.max(0, damage * (100 / (100 + defense)));
}
