package com.dantech.dreams.data.prefs

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private val serializer = MapSerializer(
    String.serializer(),
    MapSerializer(String.serializer(), Float.serializer()),
)

fun encodeOverrides(map: Map<String, Map<String, Float>>): String =
    if (map.isEmpty()) "" else json.encodeToString(serializer, map)

fun decodeOverrides(raw: String): Map<String, Map<String, Float>> {
    if (raw.isBlank()) return emptyMap()
    return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyMap())
}
