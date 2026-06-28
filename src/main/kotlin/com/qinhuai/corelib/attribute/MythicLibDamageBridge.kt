package com.qinhuai.corelib.attribute

import org.bukkit.Bukkit
import org.bukkit.event.entity.EntityDamageEvent
import java.lang.reflect.Method

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
