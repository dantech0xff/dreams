package com.dantech.dreams.domain.lesson

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonModel
import kotlinx.collections.immutable.ImmutableList

interface LessonRepository {
    fun all(): ImmutableList<LessonModel>
    fun byCategory(category: LessonCategory): ImmutableList<LessonModel>
    fun byId(id: String): LessonModel?
    fun validate(): List<Pair<String, String>>
}
