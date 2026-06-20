// 秦淮原生属性 · 招架（on_damage_taken，几率完全格挡本次伤害，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值
//   ctx.get("value")     玩家招架率总值（0-1，0.3 = 30%）
//   ctx.get("attacker")  攻击者实体（可能为 null）
// 返回：招架成功则 0（完全免伤），否则原伤害。与闪避独立结算。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var parry = ctx.get("value");
    if (parry > 0 && Math.random() < parry) {
        return 0;
    }
    return damage;
}
