package com.qinhuai.corelib.util

import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player

object EffectUtils {
    
    fun playSound(loc: Location, sound: Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        loc.world?.playSound(loc, sound, volume, pitch)
    }
    
    fun playSoundForPlayer(player: Player, loc: Location, sound: Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        player.playSound(loc, sound, volume, pitch)
    }
    
    fun playSoundForAll(world: World, loc: Location, sound: Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        world.playSound(loc, sound, volume, pitch)
    }
    
    fun spawnParticle(
        loc: Location,
        particle: Particle,
        count: Int = 1,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0,
        extra: Double = 0.0
    ) {
        loc.world?.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra)
    }
    
    fun spawnParticleForPlayer(
        player: Player,
        loc: Location,
        particle: Particle,
        count: Int = 1,
        offsetX: Double = 0.0,
        offsetY: Double = 0.0,
        offsetZ: Double = 0.0,
        extra: Double = 0.0
    ) {
        player.spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, extra)
    }
    
    fun spawnColoredDust(
        loc: Location,
        color: Color,
        count: Int = 10,
        offsetX: Double = 0.3,
        offsetY: Double = 0.3,
        offsetZ: Double = 0.3,
        size: Float = 1.0f
    ) {
        val dustOptions = Particle.DustOptions(color, size)
        loc.world?.spawnParticle(ServerCompat.particle("DUST", "REDSTONE"), loc, count, offsetX, offsetY, offsetZ, dustOptions)
    }
    
    fun spawnColoredDustForPlayer(
        player: Player,
        loc: Location,
        color: Color,
        count: Int = 10,
        offsetX: Double = 0.3,
        offsetY: Double = 0.3,
        offsetZ: Double = 0.3,
        size: Float = 1.0f
    ) {
        val dustOptions = Particle.DustOptions(color, size)
        player.spawnParticle(ServerCompat.particle("DUST", "REDSTONE"), loc, count, offsetX, offsetY, offsetZ, dustOptions)
    }
    
    fun spawnCircle(
        center: Location,
        particle: Particle,
        radius: Double = 1.0,
        points: Int = 20,
        count: Int = 1
    ) {
        for (i in 0 until points) {
            val angle = 2 * Math.PI * i / points
            val x = center.x + radius * Math.cos(angle)
            val z = center.z + radius * Math.sin(angle)
            val loc = Location(center.world, x, center.y, z)
            spawnParticle(loc, particle, count)
        }
    }
    
    fun spawnHelix(
        center: Location,
        particle: Particle,
        radius: Double = 1.0,
        height: Double = 2.0,
        turns: Int = 2,
        points: Int = 40,
        count: Int = 1
    ) {
        for (i in 0 until points) {
            val y = center.y + (height * i / points)
            val angle = 2 * Math.PI * turns * i / points
            val x = center.x + radius * Math.cos(angle)
            val z = center.z + radius * Math.sin(angle)
            val loc = Location(center.world, x, y, z)
            spawnParticle(loc, particle, count)
        }
    }
    
    fun spawnLine(
        start: Location,
        end: Location,
        particle: Particle,
        points: Int = 10,
        count: Int = 1
    ) {
        val world = start.world ?: return
        val vector = end.toVector().subtract(start.toVector())
        val step = vector.multiply(1.0 / points)
        
        for (i in 0..points) {
            val loc = start.clone().add(step.clone().multiply(i))
            spawnParticle(loc, particle, count)
        }
    }
    
    object Presets {
        fun success(loc: Location) {
            playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            spawnParticle(loc, ServerCompat.particle("HAPPY_VILLAGER", "VILLAGER_HAPPY"), 10, 0.3, 0.3, 0.3, 0.05)
        }
        
        fun error(loc: Location) {
            playSound(loc, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            spawnColoredDust(loc, Color.RED, 10, 0.3, 0.3, 0.3)
        }
        
        fun warning(loc: Location) {
            playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f)
            spawnColoredDust(loc, Color.ORANGE, 10, 0.3, 0.3, 0.3)
        }
        
        fun info(loc: Location) {
            playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f)
            spawnColoredDust(loc, Color.AQUA, 10, 0.3, 0.3, 0.3)
        }
        
        fun click(loc: Location) {
            playSound(loc, Sound.UI_BUTTON_CLICK, 1.0f, 1.0f)
        }
        
        fun plant(loc: Location) {
            playSound(loc, Sound.ITEM_CROP_PLANT, 1.0f, 1.0f)
            spawnParticle(loc.clone().add(0.5, 0.5, 0.5), ServerCompat.particle("HAPPY_VILLAGER", "VILLAGER_HAPPY"), 10, 0.3, 0.3, 0.3, 0.05)
        }
        
        fun harvest(loc: Location) {
            playSound(loc, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.0f)
            spawnParticle(loc.clone().add(0.5, 0.5, 0.5), ServerCompat.particle("HAPPY_VILLAGER", "VILLAGER_HAPPY"), 10, 0.3, 0.3, 0.3, 0.05)
        }
        
        fun breakBlock(loc: Location) {
            playSound(loc, Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f)
        }
    }
}
