package com.qinhuai.corelib.attribute

import org.bukkit.Bukkit
import org.bukkit.event.entity.EntityDamageEvent
import java.lang.reflect.Method

/**
 * 反射查询 MythicLib：判断一次伤害事件是否为「技能/法术伤害」(ML DamageType SKILL 或 MAGIC)。
 *
 * 用途：MM 技能伤害若以 cause=ENTITY_ATTACK 落地，会被 CoreLib 物理近战管线误当普通近战处理
 * (套物理增伤/暴击)。此桥让 CoreLib 据 ML 的伤害类型把技能伤害从物理管线隔离出去。
 * ML 对原版近战也会登记，但类型是 WEAPON/UNARMED，故只认 SKILL/MAGIC 不会误伤真近战。
 * MythicLib 缺失或任何反射失败 → 返回 false（退回现状，零回归）。
 */
object MythicLibDamageBridge {

    private var resolved = false
    private var available = false
    private var mlInstance: Any? = null
    private var mGetDamage: Method? = null
    private var mFindAttack: Method? = null
    private var mAttackGetDamage: Method? = null
    private var mMetaGetByType: Method? = null
    private var skillType: Any? = null
    private var magicType: Any? = null

    private fun ensure() {
        if (resolved) return
        resolved = true
        if (Bukkit.getPluginManager().getPlugin("MythicLib") == null) return
        try {
            val mlClass = Class.forName("io.lumine.mythic.lib.MythicLib")
            mlInstance = runCatching { mlClass.getField("plugin").get(null) }.getOrNull()
                ?: runCatching { mlClass.getMethod("inst").invoke(null) }.getOrNull()
                ?: return
            mGetDamage = mlClass.getMethod("getDamage")
            val dmgManagerClass = Class.forName("io.lumine.mythic.lib.manager.DamageManager")
            mFindAttack = dmgManagerClass.getMethod("findAttack", EntityDamageEvent::class.java)
            val attackMetaClass = Class.forName("io.lumine.mythic.lib.damage.AttackMetadata")
            mAttackGetDamage = attackMetaClass.getMethod("getDamage")
            val dmgTypeClass = Class.forName("io.lumine.mythic.lib.damage.DamageType")
            val dmgMetaClass = Class.forName("io.lumine.mythic.lib.damage.DamageMetadata")
            mMetaGetByType = dmgMetaClass.getMethod("getDamage", dmgTypeClass)
            val valueOf = dmgTypeClass.getMethod("valueOf", String::class.java)
            skillType = valueOf.invoke(null, "SKILL")
            magicType = valueOf.invoke(null, "MAGIC")
            available = true
        } catch (ex: Throwable) {
            available = false
        }
    }

    fun isSkillDamage(event: EntityDamageEvent): Boolean {
        ensure()
        if (!available) return false
        return try {
            val manager = mGetDamage!!.invoke(mlInstance) ?: return false
            val attack = mFindAttack!!.invoke(manager, event) ?: return false
            val meta = mAttackGetDamage!!.invoke(attack) ?: return false
            val skill = (mMetaGetByType!!.invoke(meta, skillType) as? Number)?.toDouble() ?: 0.0
            val magic = (mMetaGetByType!!.invoke(meta, magicType) as? Number)?.toDouble() ?: 0.0
            skill > 0.0 || magic > 0.0
        } catch (ex: Throwable) {
            false
        }
    }
}
