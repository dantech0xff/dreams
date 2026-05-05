package com.dantech.dreams.data.lesson

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRegistryTest {

    @Test
    fun `registry is non-empty after bootstrap`() {
        LessonRegistry.bootstrap()
        assertFalse("Registry must contain lessons", LessonRegistry.all().isEmpty())
    }

    @Test
    fun `every lesson has unique id`() {
        LessonRegistry.bootstrap()
        val all = LessonRegistry.all()
        val ids = all.map { it.id }
        assertEquals("Lesson ids must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `every lesson has non-blank source`() {
        LessonRegistry.bootstrap()
        LessonRegistry.all().forEach {
            assertTrue("Lesson ${it.id} has blank source", it.agslSource.isNotBlank())
            assertTrue("Lesson ${it.id} title blank", it.title.isNotBlank())
            assertTrue("Lesson ${it.id} intro blank", it.conceptIntro.isNotBlank())
        }
    }

    @Test
    fun `each category has expected lesson count`() {
        LessonRegistry.bootstrap()
        assertEquals(6, LessonRegistry.byCategory(LessonCategory.BASICS).size)
        assertEquals(6, LessonRegistry.byCategory(LessonCategory.SDF).size)
        assertEquals(6, LessonRegistry.byCategory(LessonCategory.NOISE).size)
        assertEquals(6, LessonRegistry.byCategory(LessonCategory.POSTFX).size)
        assertEquals(4, LessonRegistry.byCategory(LessonCategory.SHOWCASE).size)
    }
}
