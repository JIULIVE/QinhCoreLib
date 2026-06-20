package com.qinhuai.corelib.util

import com.qinhuai.corelib.lang.Lang

object TextUtils {
    
    fun formatNumber(num: Double, decimalPlaces: Int = 1): String {
        return "%.${decimalPlaces}f".format(num)
    }
    
    fun formatNumber(num: Int): String {
        return num.toString()
    }
    
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format(Lang.get("text-utils.time-hms"), hours, minutes, seconds)
            minutes > 0 -> String.format(Lang.get("text-utils.time-ms"), minutes, seconds)
            else -> String.format(Lang.get("text-utils.time-s"), seconds)
        }
    }
    
    fun formatTimeCompact(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return when {
            hours > 0 -> String.format("%dh%dm", hours, minutes)
            minutes > 0 -> String.format("%dm%ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }
    
    fun joinList(list: List<String>, separator: String = ", ", lastSeparator: String = Lang.get("text-utils.list-and-separator")): String {
        return when (list.size) {
            0 -> ""
            1 -> list[0]
            2 -> "${list[0]}$lastSeparator${list[1]}"
            else -> {
                val firstPart = list.dropLast(1).joinToString(separator)
                "$firstPart$lastSeparator${list.last()}"
            }
        }
    }
    
    fun capitalize(str: String): String {
        if (str.isEmpty()) return str
        return str.substring(0, 1).uppercase() + str.substring(1).lowercase()
    }
    
    fun toTitleCase(str: String): String {
        return str.split(" ").joinToString(" ") { capitalize(it) }
    }
    
    fun limitLength(str: String, maxLength: Int, suffix: String = "..."): String {
        return if (str.length <= maxLength) {
            str
        } else {
            str.take(maxLength - suffix.length) + suffix
        }
    }
    
    fun stripColors(str: String): String {
        return str.replace(Regex("§[0-9a-fk-or]"), "")
    }
    
    fun countOccurrences(str: String, substring: String): Int {
        var count = 0
        var index = 0
        while (index != -1) {
            index = str.indexOf(substring, index)
            if (index != -1) {
                count++
                index += substring.length
            }
        }
        return count
    }
    
    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) {
            dp[i][0] = i
        }
        for (j in 0..s2.length) {
            dp[0][j] = j
        }
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    fun findSimilar(str: String, candidates: List<String>, maxDistance: Int = 3): List<String> {
        return candidates
            .map { it to levenshteinDistance(str.lowercase(), it.lowercase()) }
            .filter { it.second <= maxDistance }
            .sortedBy { it.second }
            .map { it.first }
    }
}
