package com.qinhuai.corelib.database

import java.util.Base64

object MapCodec {

    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    private fun enc(value: String): String = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun dec(value: String): String =
        runCatching { String(decoder.decode(value), Charsets.UTF_8) }.getOrDefault("")

    fun encode(map: Map<String, String>): String =
        map.entries.joinToString("\n") { "${it.key}=${enc(it.value)}" }

    fun decode(s: String?): Map<String, String> {
        if (s.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (line in s.split("\n")) {
            if (line.isEmpty()) continue
            val i = line.indexOf('=')
            if (i < 0) continue
            out[line.substring(0, i)] = dec(line.substring(i + 1))
        }
        return out
    }

    fun encodeInt(map: Map<String, Int>): String = encode(map.mapValues { it.value.toString() })

    fun decodeInt(s: String?): Map<String, Int> =
        decode(s).mapNotNull { (k, v) -> v.toIntOrNull()?.let { k to it } }.toMap()

    fun encodeLong(map: Map<String, Long>): String = encode(map.mapValues { it.value.toString() })

    fun decodeLong(s: String?): Map<String, Long> =
        decode(s).mapNotNull { (k, v) -> v.toLongOrNull()?.let { k to it } }.toMap()

    fun encodeDouble(map: Map<String, Double>): String = encode(map.mapValues { it.value.toString() })

    fun decodeDouble(s: String?): Map<String, Double> =
        decode(s).mapNotNull { (k, v) -> v.toDoubleOrNull()?.let { k to it } }.toMap()

    fun encodeList(list: Collection<String>): String = list.joinToString("\n") { enc(it) }

    fun decodeList(s: String?): List<String> {
        if (s.isNullOrEmpty()) return emptyList()
        return s.split("\n").filter { it.isNotEmpty() }.map { dec(it) }
    }
}
