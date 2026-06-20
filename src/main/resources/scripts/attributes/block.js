// 秦淮原生属性 · 格挡（演示 on_damage_taken 钩子，几率减伤，可自由修改）
// 触发钩子：on_damage_taken —— 玩家受到伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值
//   ctx.get("value")     玩家格挡率总值（0-1，0.3 = 30%）
//   ctx.get("attacker")  攻击者实体（可能为 null）
// 返回：格挡成功则减伤后的伤害，否则原伤害。
// 默认：格挡成功挡掉 50% 伤害。改 REDUCTION 调平衡（0.5 = 挡一半，1.0 = 全挡）。
function onDamageTaken() {
    var damage = ctx.get("damage");
    var rate = ctx.get("value");
    var REDUCTION = 0.5;
    if (rate > 0 && Math.random() < rate) {
        return Math.max(0, damage * (1 - REDUCTION));
    }
    return damage;
}
