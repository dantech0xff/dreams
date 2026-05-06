package com.dantech.dreams.data.lesson

import com.dantech.dreams.domain.lesson.LessonRepository
import kotlinx.collections.immutable.ImmutableList

internal class LessonRepositoryImpl : LessonRepository {

    init {
        LessonRegistry.bootstrap()
    }

    override fun all(): ImmutableList<LessonModel> = LessonRegistry.all()

    override fun byCategory(category: LessonCategory): ImmutableList<LessonModel> =
        LessonRegistry.byCategory(category)

    override fun byId(id: String): LessonModel? = LessonRegistry.byId(id)

    override fun validate(): List<Pair<String, String>> = LessonRegistry.validateAll()

    override fun showcases(): ImmutableList<LessonModel> =
        LessonRegistry.byCategory(LessonCategory.SHOWCASE)
}
