package com.dantech.dreams.data.lesson

import android.graphics.RuntimeShader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object LessonRegistry {

    private val all = mutableListOf<LessonModel>()

    fun register(model: LessonModel) {
        if (all.any { it.id == model.id }) {
            error("Duplicate lesson id: ${model.id}")
        }
        all += model
    }

    fun all(): ImmutableList<LessonModel> = all.toImmutableList()

    fun byCategory(category: LessonCategory): ImmutableList<LessonModel> =
        all.filter { it.category == category }.toImmutableList()

    fun byId(id: String): LessonModel? = all.firstOrNull { it.id == id }

    fun bootstrap() {
        com.dantech.dreams.shaders.basics.BasicsBootstrap.touch()
        com.dantech.dreams.shaders.sdf.SdfBootstrap.touch()
        com.dantech.dreams.shaders.noise.NoiseBootstrap.touch()
        com.dantech.dreams.shaders.posteffect.PostFxBootstrap.touch()
        com.dantech.dreams.shaders.showcase.ShowcaseBootstrap.touch()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun validateAll(): List<Pair<String, String>> {
        val failures = mutableListOf<Pair<String, String>>()
        for (lesson in all) {
            try {
                RuntimeShader(lesson.agslSource)
            } catch (t: Throwable) {
                Log.e("LessonRegistry", "Compile failed: ${lesson.id}", t)
                failures += lesson.id to (t.message ?: "unknown error")
            }
        }
        return failures
    }
}
