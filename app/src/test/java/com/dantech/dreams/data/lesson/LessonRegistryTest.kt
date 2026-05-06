package com.dantech.dreams.data.lesson

import com.dantech.dreams.domain.lesson.LessonRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LessonRegistryTest {

    private lateinit var repo: LessonRepository

    @Before
    fun setUp() {
        repo = LessonRepositoryImpl()
    }

    @Test
    fun `registry is non-empty after bootstrap`() {
        assertFalse("Registry must contain lessons", repo.all().isEmpty())
    }

    @Test
    fun `every lesson has unique id`() {
        val ids = repo.all().map { it.id }
        assertEquals("Lesson ids must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `every lesson has non-blank source`() {
        repo.all().forEach {
            assertTrue("Lesson ${it.id} has blank source", it.agslSource.isNotBlank())
            assertTrue("Lesson ${it.id} title blank", it.title.isNotBlank())
            assertTrue("Lesson ${it.id} intro blank", it.conceptIntro.isNotBlank())
        }
    }

    @Test
    fun `each category has expected lesson count`() {
        assertEquals(6, repo.byCategory(LessonCategory.BASICS).size)
        assertEquals(4, repo.byCategory(LessonCategory.PATTERNS).size)
        assertEquals(4, repo.byCategory(LessonCategory.COLOR).size)
        assertEquals(6, repo.byCategory(LessonCategory.SDF).size)
        assertEquals(6, repo.byCategory(LessonCategory.NOISE).size)
        assertEquals(4, repo.byCategory(LessonCategory.MOTION).size)
        assertEquals(4, repo.byCategory(LessonCategory.FRACTALS).size)
        assertEquals(4, repo.byCategory(LessonCategory.LIGHTING).size)
        assertEquals(4, repo.byCategory(LessonCategory.INTERACTIVE).size)
        assertEquals(6, repo.byCategory(LessonCategory.POSTFX).size)
        assertEquals(5, repo.byCategory(LessonCategory.SHOWCASE).size)
    }
}
