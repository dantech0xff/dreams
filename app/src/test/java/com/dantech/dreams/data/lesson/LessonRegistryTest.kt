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
        assertEquals(1, repo.byCategory(LessonCategory.SHOWCASE).size)
    }

    @Test
    fun `basics lessons stay in learning order`() {
        val ids = repo.byCategory(LessonCategory.BASICS).map { it.id }
        assertEquals(
            listOf(
                "basics-01-solid",
                "basics-02-animated-color",
                "basics-03-linear-gradient",
                "basics-04-radial-gradient",
                "basics-05-polar-coords",
                "basics-06-vignette",
            ),
            ids,
        )
    }

    @Test
    fun `basics lessons include learning notes`() {
        repo.byCategory(LessonCategory.BASICS).forEach { lesson ->
            assertEquals("Lesson ${lesson.id} should have 3 learning notes", 3, lesson.learningNotes.size)
            lesson.learningNotes.forEach { note ->
                assertTrue("Lesson ${lesson.id} has blank learning note", note.isNotBlank())
            }
        }
    }

    @Test
    fun `lesson controls target declared uniforms`() {
        repo.all().forEach { lesson ->
            lesson.controls.forEach { control ->
                val declared = when (control) {
                    is LessonControl.FloatRange -> lesson.hasFloatUniform(control.uniformName)
                    is LessonControl.ColorPicker -> lesson.hasColorUniform(control.uniformName)
                }
                assertTrue(
                    "Lesson ${lesson.id} control ${control.uniformName} has no matching uniform",
                    declared,
                )
            }
        }
    }

    private fun LessonModel.hasFloatUniform(name: String): Boolean =
        Regex("""uniform\s+float\s+${Regex.escape(name)}\s*;""").containsMatchIn(agslSource)

    private fun LessonModel.hasColorUniform(name: String): Boolean =
        Regex("""(?:layout\s*\(\s*color\s*\)\s+)?uniform\s+half4\s+${Regex.escape(name)}\s*;""")
            .containsMatchIn(agslSource)
}
