package com.qinhuai.corelib.attribute

data class Element(
    val id: String,
    val name: String,
    val color: String,
    val restrains: List<String> = emptyList(),
) {
    val damageKey: String get() = "${id}_damage"
    val bonusKey: String get() = "${id}_bonus"
    val resistKey: String get() = "${id}_resist"
}
