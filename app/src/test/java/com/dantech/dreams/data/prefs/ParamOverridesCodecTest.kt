package com.dantech.dreams.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParamOverridesCodecTest {

    @Test
    fun `empty map encodes to empty string`() {
        assertEquals("", encodeOverrides(emptyMap()))
    }

    @Test
    fun `blank input decodes to empty map`() {
        assertTrue(decodeOverrides("").isEmpty())
        assertTrue(decodeOverrides("   ").isEmpty())
    }

    @Test
    fun `roundtrip preserves nested map`() {
        val original = mapOf(
            "lesson-a" to mapOf("u" to 0.5f, "v" to 1.25f),
            "lesson-b" to mapOf("alpha" to 0.0f),
        )
        val encoded = encodeOverrides(original)
        val decoded = decodeOverrides(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `malformed input never throws`() {
        assertTrue(decodeOverrides("{not valid json").isEmpty())
        assertTrue(decodeOverrides("[]").isEmpty())
    }
}
