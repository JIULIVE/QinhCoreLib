// 秦淮原生属性 · 伤害减免（on_damage_taken，按比例直接减伤，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值
//   ctx.get("value")     玩家伤害减免总值（0-1，0.2 = 减 20%）
// 返回：减免后的伤害。默认最多减 90%（防全免，改 CAP 调平衡）。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var reduction = ctx.get("value");
    var CAP = 0.9;
    if (reduction <= 0) return damage;
    if (reduction > CAP) reduction = CAP;
    return Math.max(0, damage * (1 - reduction));
}
