package com.dantech.dreams.support

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class FakeLessonRepository(
    seed: List<LessonModel> = sampleLessons(),
) : LessonRepository {

    private val seed: ImmutableList<LessonModel> = seed.toImmutableList()

    override fun all(): ImmutableList<LessonModel> = seed

    override fun byCategory(category: LessonCategory): ImmutableList<LessonModel> =
        seed.filter { it.category == category }.toImmutableList()

    override fun byId(id: String): LessonModel? = seed.firstOrNull { it.id == id }

    override fun validate(): List<Pair<String, String>> = emptyList()
}

fun sampleLessons(): List<LessonModel> = listOf(
    LessonModel(
        id = "test-basics-1",
        title = "Test Basics",
        category = LessonCategory.BASICS,
        complexity = 1,
        conceptIntro = "intro",
        agslSource = "half4 main(float2 fc) { return half4(1); }",
        controls = persistentListOf(
            LessonControl.FloatRange("amp", "amplitude", 0f, 1f, 0.25f),
        ),
    ),
    LessonModel(
        id = "showcase-test",
        title = "Test Showcase",
        category = LessonCategory.SHOWCASE,
        complexity = 1,
        conceptIntro = "showcase intro",
        agslSource = "half4 main(float2 fc) { return half4(1); }",
    ),
)
