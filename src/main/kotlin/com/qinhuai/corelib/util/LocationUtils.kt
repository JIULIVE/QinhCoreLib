package com.qinhuai.corelib.util

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.util.Vector
import kotlin.math.sqrt

object LocationUtils {
    
    fun serialize(loc: Location): String {
        return "${loc.world?.name ?: "world"},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"
    }
    
    fun deserialize(s: String): Location? {
        val parts = s.split(",")
        if (parts.size < 4) return null
        val world = org.bukkit.Bukkit.getWorld(parts[0]) ?: return null
        return Location(
            world,
            parts[1].toDouble(),
            parts[2].toDouble(),
            parts[3].toDouble(),
            parts.getOrNull(4)?.toFloat() ?: 0f,
            parts.getOrNull(5)?.toFloat() ?: 0f
        )
    }
    
    fun serializeList(locs: List<Location>): String {
        return locs.joinToString(";") { serialize(it) }
    }
    
    fun deserializeList(s: String): List<Location> {
        if (s.isEmpty()) return emptyList()
        return s.split(";").mapNotNull { deserialize(it) }
    }
    
    fun getBlockLocation(loc: Location): Location {
        return Location(
            loc.world,
            loc.blockX.toDouble(),
            loc.blockY.toDouble(),
            loc.blockZ.toDouble()
        )
    }
    
    fun getCenterLocation(loc: Location): Location {
        return getBlockLocation(loc).add(0.5, 0.0, 0.5)
    }
    
    fun distanceSquared(loc1: Location, loc2: Location): Double {
        if (loc1.world != loc2.world) return Double.MAX_VALUE
        val dx = loc1.x - loc2.x
        val dy = loc1.y - loc2.y
        val dz = loc1.z - loc2.z
        return dx * dx + dy * dy + dz * dz
    }
    
    fun distance(loc1: Location, loc2: Location): Double {
        return sqrt(distanceSquared(loc1, loc2))
    }
    
    fun isSameBlock(loc1: Location, loc2: Location): Boolean {
        return loc1.world == loc2.world &&
               loc1.blockX == loc2.blockX &&
               loc1.blockY == loc2.blockY &&
               loc1.blockZ == loc2.blockZ
    }
    
    fun getDirection(from: Location, to: Location): Vector {
        return to.toVector().subtract(from.toVector()).normalize()
    }
    
    fun lookAt(from: Location, to: Location): Location {
        val result = from.clone()
        val direction = getDirection(from, to)
        result.yaw = getYaw(direction)
        result.pitch = getPitch(direction)
        return result
    }
    
    private fun getYaw(vector: Vector): Float {
        var yaw = Math.toDegrees(Math.atan2(-vector.x, vector.z)).toFloat()
        if (yaw < 0) yaw += 360.0f
        return yaw
    }
    
    private fun getPitch(vector: Vector): Float {
        return Math.toDegrees(Math.asin(-vector.y)).toFloat()
    }
    
    fun getNearbyLocations(center: Location, radius: Int): List<Location> {
        val locations = mutableListOf<Location>()
        val world = center.world ?: return locations
        
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    locations.add(Location(world, center.x + x, center.y + y, center.z + z))
                }
            }
        }
        return locations
    }
    
    fun getNearbyBlocks(center: Location, radius: Int): List<Location> {
        return getNearbyLocations(center, radius).map { getBlockLocation(it) }.distinct()
    }
    
    fun isInRange(loc: Location, center: Location, range: Double): Boolean {
        if (loc.world != center.world) return false
        return distanceSquared(loc, center) <= range * range
    }
    
    fun getChunkKey(loc: Location): String {
        return "${loc.world?.name}:${loc.chunk.x}:${loc.chunk.z}"
    }
}
