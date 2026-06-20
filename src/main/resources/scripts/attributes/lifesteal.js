// 秦淮原生属性 · 生命偷取（演示 on_damage_dealt 钩子 + qcl.heal，可自由修改）
// 触发钩子：on_damage_dealt —— 玩家造成伤害时
// 可用变量：
//   ctx.get("damage")    当前伤害值（已含暴击等前序钩子结果）
//   ctx.get("value")     玩家生命偷取总值（0-1，0.1 = 偷取 10% 伤害为生命）
//   ctx.get("victim")    受击实体
//   qcl.heal(amount)     治疗自己（攻击者），自动钳制到最大生命
// 返回：不改伤害（返回原值），仅按比例治疗自己。
function onDamageDealt() {
    var damage = ctx.get("damage");
    var steal = ctx.get("value");
    if (steal > 0 && damage > 0) {
        qcl.heal(damage * steal);
    }
    return damage;
}
