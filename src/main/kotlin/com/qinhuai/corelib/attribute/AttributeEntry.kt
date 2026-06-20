package com.qinhuai.corelib.attribute

data class AttributeEntry(
    val rawToken: String,
    val def: AttributeDef?,
    val min: Double,
    val max: Double,
) {
    val isRange: Boolean get() = max != min

    fun scalar(): Double = if (isRange) (min + max) / 2.0 else min

    companion object {
        fun of(rawToken: String, min: Double, max: Double): AttributeEntry =
            AttributeEntry(rawToken, AttributeRegistry.resolve(rawToken), min, max)

        fun scalar(rawToken: String, value: Double): AttributeEntry =
            of(rawToken, value, value)
    }
}
