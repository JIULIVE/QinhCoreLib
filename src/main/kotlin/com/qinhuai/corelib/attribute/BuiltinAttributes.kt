package com.qinhuai.corelib.attribute

import com.qinhuai.corelib.lang.Lang

object BuiltinAttributes {

    private const val DZ = "qinhcorelib:attributes/"

    fun all(): List<AttributeDef> = listOf(
        AttributeDef("attack_damage", "攻击力", category = "基础", itemAttribute = "attack_damage", combatPower = 5.0),
        AttributeDef("attack_speed", "攻击速度", category = "基础", itemAttribute = "attack_speed", combatPower = 3.0),

        AttributeDef("physical_damage", "物理增伤", category = "物理", damageType = DamageType.PHYSICAL, combatPower = 50.0),
        AttributeDef("physical_crit_rate", "物理暴击率", category = "物理", max = 1.0, combatPower = 80.0, message = Lang.get("builtin-attributes.physical-crit")),
        AttributeDef("physical_crit_damage", "物理爆伤", category = "物理", combatPower = 30.0),
        AttributeDef("armor_penetration", "护甲穿透", category = "物理"),
        AttributeDef("projectile_damage", "投掷物增伤", category = "物理", combatPower = 40.0),

        AttributeDef("magic_attack", "法术伤害", category = "法术", combatPower = 5.0),
        AttributeDef("magic_damage", "法术增伤", category = "法术", damageType = DamageType.MAGIC, combatPower = 50.0),
        AttributeDef("magic_crit_rate", "法术暴击率", category = "法术", max = 1.0, combatPower = 80.0, message = Lang.get("builtin-attributes.magic-crit")),
        AttributeDef("magic_crit_damage", "法术爆伤", category = "法术", combatPower = 30.0),
        AttributeDef("magic_penetration", "法术穿透", category = "法术"),

        AttributeDef("skill_attack", "技能伤害", category = "技能", combatPower = 5.0),
        AttributeDef("skill_damage", "技能增伤", category = "技能", damageType = DamageType.SKILL, combatPower = 50.0),
        AttributeDef("skill_crit_rate", "技能暴击率", category = "技能", max = 1.0, combatPower = 80.0, message = Lang.get("builtin-attributes.skill-crit")),
        AttributeDef("skill_crit_damage", "技能爆伤", category = "技能", combatPower = 30.0),

        AttributeDef("true_attack", "真实伤害", category = "真实", combatPower = 7.0),
        AttributeDef("true_damage", "真伤增伤", category = "真实", damageType = DamageType.TRUE, combatPower = 50.0),

        AttributeDef("pvp_damage", "PVP伤害", category = "环境"),
        AttributeDef("pve_damage", "PVE伤害", category = "环境"),
        AttributeDef("pvp_defense", "PVP防御", category = "环境"),
        AttributeDef("pve_defense", "PVE防御", category = "环境"),

        AttributeDef("defense", "防御", category = "减伤", mitigation = true, combatPower = 2.0, hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}defense.js:onDamageTaken")),
        AttributeDef("damage_reduction", "伤害减免", category = "减伤", mitigation = true, max = 0.9, combatPower = 60.0, hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}damage_reduction.js:onDamageTaken")),
        AttributeDef("dodge", "闪避率", category = "减伤", max = 1.0, combatPower = 60.0, message = Lang.get("builtin-attributes.dodge"), hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}dodge.js:onDamageTaken")),
        AttributeDef("block_rate", "格挡率", category = "减伤", max = 1.0, combatPower = 40.0, hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}block.js:onDamageTaken")),
        AttributeDef("parry", "招架率", category = "减伤", max = 1.0, combatPower = 50.0, hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}parry.js:onDamageTaken")),
        AttributeDef("crit_resist", "暴击抗性", category = "减伤", max = 1.0),
        AttributeDef("armor", "护甲值", category = "减伤", vanillaKey = "armor"),
        AttributeDef("armor_toughness", "护甲韧性", category = "减伤", vanillaKey = "armor_toughness"),
        AttributeDef("knockback_resistance", "击退抗性", category = "减伤", vanillaKey = "knockback_resistance"),

        AttributeDef("health", "生命", category = "资源", vanillaKey = "max_health", combatPower = 0.5),
        AttributeDef("health_regen", "生命恢复", category = "资源"),
        AttributeDef("max_mana", "最大法力", category = "资源"),
        AttributeDef("mana_regen", "法力恢复", category = "资源"),
        AttributeDef("max_stamina", "最大耐力", category = "资源"),
        AttributeDef("stamina_regen", "耐力恢复", category = "资源"),

        AttributeDef("movement_speed", "移动速度", category = "通用", vanillaKey = "movement_speed"),
        AttributeDef("max_absorption", "最大吸收", category = "通用", vanillaKey = "max_absorption"),
        AttributeDef("luck", "幸运", category = "通用", vanillaKey = "luck"),

        AttributeDef("scale", "体型缩放", category = "原版1.21", vanillaKey = "scale"),
        AttributeDef("step_height", "步高", category = "原版1.21", vanillaKey = "step_height"),
        AttributeDef("gravity", "重力", category = "原版1.21", vanillaKey = "gravity"),
        AttributeDef("jump_strength", "跳跃力", category = "原版1.21", vanillaKey = "jump_strength"),
        AttributeDef("safe_fall_distance", "安全坠落距离", category = "原版1.21", vanillaKey = "safe_fall_distance"),
        AttributeDef("fall_damage_multiplier", "坠落伤害倍率", category = "原版1.21", vanillaKey = "fall_damage_multiplier"),
        AttributeDef("burning_time", "燃烧时长", category = "原版1.21", vanillaKey = "burning_time"),
        AttributeDef("explosion_knockback_resistance", "爆炸击退抗性", category = "原版1.21", vanillaKey = "explosion_knockback_resistance"),
        AttributeDef("movement_efficiency", "地形移动效率", category = "原版1.21", vanillaKey = "movement_efficiency"),
        AttributeDef("water_movement_efficiency", "水中移动效率", category = "原版1.21", vanillaKey = "water_movement_efficiency"),
        AttributeDef("oxygen_bonus", "氧气加成", category = "原版1.21", vanillaKey = "oxygen_bonus"),
        AttributeDef("block_interaction_range", "方块交互距离", category = "原版1.21", vanillaKey = "block_interaction_range"),
        AttributeDef("entity_interaction_range", "实体交互距离", category = "原版1.21", vanillaKey = "entity_interaction_range"),
        AttributeDef("block_break_speed", "挖掘速度", category = "原版1.21", vanillaKey = "block_break_speed"),
        AttributeDef("mining_efficiency", "挖掘效率", category = "原版1.21", vanillaKey = "mining_efficiency"),
        AttributeDef("submerged_mining_speed", "水下挖掘速度", category = "原版1.21", vanillaKey = "submerged_mining_speed"),
        AttributeDef("sneaking_speed", "潜行速度", category = "原版1.21", vanillaKey = "sneaking_speed"),
        AttributeDef("sweeping_damage_ratio", "横扫伤害比例", category = "原版1.21", vanillaKey = "sweeping_damage_ratio"),

        AttributeDef("lifesteal", "生命偷取", category = "杂项", order = 100, hooks = mapOf(AttributeHooks.ON_DAMAGE_DEALT to "${DZ}lifesteal.js:onDamageDealt")),
        AttributeDef("spell_vampirism", "法术吸血", category = "杂项", order = 100, hooks = mapOf(AttributeHooks.ON_MAGIC_DAMAGE_DEALT to "${DZ}spell_vampirism.js:onMagicDamageDealt")),
        AttributeDef("reflection", "伤害反弹", category = "杂项", order = 100, hooks = mapOf(AttributeHooks.ON_DAMAGE_TAKEN to "${DZ}reflection.js:onDamageTaken")),
        AttributeDef("cooldown_reduction", "冷却缩减", category = "杂项"),
        AttributeDef("exp_bonus", "经验加成", category = "杂项"),
        AttributeDef("loot_bonus", "掉落加成", category = "杂项"),
        AttributeDef("money_bonus", "金币加成", category = "杂项"),
        AttributeDef("attack_knockback", "攻击击退", category = "杂项", vanillaKey = "attack_knockback"),
    )
}
