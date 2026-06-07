package com.qinhuai.corelib.hologram

import com.qinhuai.corelib.QinhCoreLib
import com.qinhuai.corelib.util.TextUtil
import com.qinhuai.corelib.scheduler.TaskScheduler

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.scheduler.BukkitTask
import java.util.*

class Hologram(
    val id: String,
    val location: Location,
    private val lines: MutableList<String> = mutableListOf(),
    var entity: Entity? = null
) {
    private val armorStands = mutableListOf<ArmorStand>()
    var passengerEntity: Entity? = null
        private set
    var passengerOffsetY: Double = 1.8
    var isVisible: Boolean = true
        private set
    private var repairTask: BukkitTask? = null
    
    fun startRepairTask() {
        stopRepairTask()
        repairTask = TaskScheduler.runSyncRepeating(20L, 20L) {
            ensurePassengerAttachment()
        }
    }
    
    fun stopRepairTask() {
        repairTask?.cancel()
        repairTask = null
    }
    
    fun setLine(index: Int, text: String) {
        if (index >= lines.size) {
            lines.add(text)
        } else {
            lines[index] = text
        }
        update()
    }
    
    fun addLine(text: String) {
        lines.add(text)
        update()
    }
    
    fun removeLine(index: Int) {
        if (index < lines.size) {
            lines.removeAt(index)
            update()
        }
    }
    
    fun getLines(): List<String> = lines.toList()
    
    fun clearLines() {
        lines.clear()
        update()
    }
    
    fun setPassenger(entity: Entity, offsetY: Double = 1.8) {
        passengerEntity = entity
        passengerOffsetY = offsetY
        update()
        startRepairTask()
    }
    
    fun removePassenger() {
        stopRepairTask()
        passengerEntity?.let { entity ->
            armorStands.forEach { stand ->
                if (stand.vehicle == entity) {
                    entity.removePassenger(stand)
                }
            }
        }
        passengerEntity = null
    }
    
    private fun ensurePassengerAttachment() {
        val entity = passengerEntity ?: return
        if (!entity.isValid) {
            removePassenger()
            return
        }
        
        armorStands.forEach { stand ->
            if (!stand.isValid) {
                update()
                return
            }
            
            if (stand.vehicle != entity) {
                try {
                    entity.addPassenger(stand)
                } catch (e: Exception) {
                }
            }
        }
    }
    
    private fun attachToEntity() {
        val entity = passengerEntity ?: return
        armorStands.forEach { stand ->
            if (stand.isValid && stand.vehicle != entity) {
                try {
                    entity.addPassenger(stand)
                } catch (e: Exception) {
                }
            }
        }
    }
    
    fun show() {
        isVisible = true
        armorStands.forEach { it.isVisible = true }
    }
    
    fun hide() {
        isVisible = false
        armorStands.forEach { it.isVisible = false }
    }
    
    fun update() {
        deleteArmorStands()
        spawnArmorStands()
        if (passengerEntity != null) {
            attachToEntity()
        }
    }
    
    fun teleport(loc: Location) {
        location.world = loc.world
        location.x = loc.x
        location.y = loc.y
        location.z = loc.z
        location.yaw = loc.yaw
        location.pitch = loc.pitch
        
        if (passengerEntity == null) {
            armorStands.forEachIndexed { index, stand ->
                stand.teleport(getLineLocation(index))
            }
        }
    }
    
    private fun getLineLocation(index: Int): Location {
        return location.clone().add(0.0, -index * 0.25, 0.0)
    }
    
    private fun spawnArmorStands() {
        val world = location.world ?: return
        
        lines.forEachIndexed { index, text ->
            val loc = if (passengerEntity != null) {
                passengerEntity!!.location.clone().add(0.0, passengerOffsetY - index * 0.25, 0.0)
            } else {
                getLineLocation(index)
            }
            
            val stand = world.spawn(loc, ArmorStand::class.java) {
                it.isVisible = isVisible
                it.isCustomNameVisible = true
                it.customName(TextUtil.toComponent(text))
                it.isMarker = true
                it.isSmall = false
                it.setGravity(false)
                it.isCollidable = false
                it.isInvulnerable = true
                it.setBasePlate(false)
                it.setArms(false)
            }
            
            armorStands.add(stand)
        }
    }
    
    private fun deleteArmorStands() {
        stopRepairTask()
        armorStands.forEach { it.remove() }
        armorStands.clear()
    }
    
    fun delete() {
        deleteArmorStands()
        passengerEntity = null
    }
}

object HologramManager {
    private val holograms = mutableMapOf<String, Hologram>()
    
    fun create(id: String, location: Location, vararg lines: String): Hologram {
        val hologram = Hologram(id, location, lines.toMutableList())
        holograms[id] = hologram
        hologram.update()
        return hologram
    }
    
    fun createAsPassenger(
        id: String,
        entity: Entity,
        offsetY: Double = 1.8,
        vararg lines: String
    ): Hologram {
        val hologram = Hologram(id, entity.location.clone(), lines.toMutableList())
        holograms[id] = hologram
        hologram.setPassenger(entity, offsetY)
        hologram.update()
        return hologram
    }
    
    fun get(id: String): Hologram? = holograms[id]
    
    fun remove(id: String) {
        holograms.remove(id)?.delete()
    }
    
    fun removeAll() {
        holograms.values.forEach { it.delete() }
        holograms.clear()
    }
    
    fun getAll(): Collection<Hologram> = holograms.values
    
    fun showTemporary(
        location: Location,
        text: String,
        durationTicks: Long = 40L
    ) {
        val id = UUID.randomUUID().toString()
        val hologram = create(id, location, text)
        
        com.qinhuai.corelib.scheduler.TaskScheduler.runSyncLater(durationTicks) {
            remove(id)
        }
    }
    
    fun showPlayerBubble(
        player: org.bukkit.entity.Player,
        text: String,
        fadeIn: Int = 10,
        stay: Int = 60,
        fadeOut: Int = 10
    ) {
        TextUtil.showColoredTitle(player, text, fadeIn, stay, fadeOut)
    }
}
