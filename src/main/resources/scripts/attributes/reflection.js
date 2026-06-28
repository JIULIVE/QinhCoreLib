// 秦淮原生属性 · 伤害反弹（on_damage_taken，把受到伤害的一部分反弹给攻击者，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")           当前伤害值
//   ctx.get("value")            玩家伤害反弹总值（0-1，0.2 = 反弹 20% 伤害）
//   ctx.get("attacker")         攻击者实体（可能为 null，如摔落/火焰）
//   qcl.damage(entity, amount)  对实体造成伤害
// 返回：不改自身受到的伤害（返回原值），仅把一部分反弹给攻击者。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var reflect = ctx.get("value");
    var attacker = ctx.get("attacker");
    if (reflect > 0 && damage > 0 && attacker) {
        qcl.damage(attacker, damage * reflect);
    }
    return damage;
}
