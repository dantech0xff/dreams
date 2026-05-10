package com.dantech.dreams.data.prefs

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private val floatOverridesSerializer = MapSerializer(
    String.serializer(),
    MapSerializer(String.serializer(), Float.serializer()),
)

private val colorOverridesSerializer = MapSerializer(
    String.serializer(),
    MapSerializer(String.serializer(), Int.serializer()),
)

fun encodeOverrides(map: Map<String, Map<String, Float>>): String =
    if (map.isEmpty()) "" else json.encodeToString(floatOverridesSerializer, map)

fun decodeOverrides(raw: String): Map<String, Map<String, Float>> {
    if (raw.isBlank()) return emptyMap()
    return runCatching { json.decodeFromString(floatOverridesSerializer, raw) }.getOrDefault(emptyMap())
}

fun encodeColorOverrides(map: Map<String, Map<String, Int>>): String =
    if (map.isEmpty()) "" else json.encodeToString(colorOverridesSerializer, map)

fun decodeColorOverrides(raw: String): Map<String, Map<String, Int>> {
    if (raw.isBlank()) return emptyMap()
    return runCatching { json.decodeFromString(colorOverridesSerializer, raw) }.getOrDefault(emptyMap())
}
