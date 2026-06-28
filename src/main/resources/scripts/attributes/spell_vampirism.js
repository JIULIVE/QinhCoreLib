// 秦淮原生属性 · 法术吸血（on_magic_damage_dealt，造成法术/技能伤害时按比例治疗自己，可自由修改）
// 与生命偷取 lifesteal 对称：lifesteal 走 on_damage_dealt(物理近战)，本脚本走 on_magic_damage_dealt
// （纯技能伤害的 MAGIC 事件 + 近战命中附带的元素伤害）。只在法术伤害链触发，不会和物理吸血串味。
// 可用变量：
//   ctx.get("damage")    本次法术/技能伤害值
//   ctx.get("value")     玩家法术吸血总值（0-1，0.1 = 法术伤害的 10% 转为生命）
//   ctx.get("victim")    受击实体
//   qcl.heal(amount)     治疗自己（攻击者），自动钳制到最大生命
// 返回：不改伤害（返回原值），仅按比例治疗自己。
function onMagicDamageDealt() {
    var damage = ctx.get("damage");
    var steal = ctx.get("value");
    if (steal > 0 && damage > 0) {
        qcl.heal(damage * steal);
    }
    return damage;
}
