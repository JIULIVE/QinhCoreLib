// 秦淮原生属性 · 闪避（演示 on_damage_taken 钩子，几率完全免伤，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值
//   ctx.get("value")     玩家闪避率总值（0-1，0.3 = 30%）
//   ctx.get("attacker")  攻击者实体（可能为 null，如摔落/火焰）
// 返回：闪避成功则 0（完全免伤），否则原伤害。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var dodge = ctx.get("value");
    if (dodge > 0 && Math.random() < dodge) {
        return 0;
    }
    return damage;
}
