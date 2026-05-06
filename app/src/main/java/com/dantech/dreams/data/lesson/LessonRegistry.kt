package com.dantech.dreams.data.lesson

import android.graphics.RuntimeShader
import android.util.Log
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object LessonRegistry {

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
        if (all.isNotEmpty()) return
        com.dantech.dreams.data.lesson.source.basics.BasicsBootstrap.touch()
        com.dantech.dreams.data.lesson.source.patterns.PatternsBootstrap.touch()
        com.dantech.dreams.data.lesson.source.colorlab.ColorBootstrap.touch()
        com.dantech.dreams.data.lesson.source.sdf.SdfBootstrap.touch()
        com.dantech.dreams.data.lesson.source.noise.NoiseBootstrap.touch()
        com.dantech.dreams.data.lesson.source.motion.MotionBootstrap.touch()
        com.dantech.dreams.data.lesson.source.fractals.FractalsBootstrap.touch()
        com.dantech.dreams.data.lesson.source.lighting.LightingBootstrap.touch()
        com.dantech.dreams.data.lesson.source.interactive.InteractiveBootstrap.touch()
        com.dantech.dreams.data.lesson.source.posteffect.PostFxBootstrap.touch()
        com.dantech.dreams.data.lesson.source.showcase.ShowcaseBootstrap.touch()
    }

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
