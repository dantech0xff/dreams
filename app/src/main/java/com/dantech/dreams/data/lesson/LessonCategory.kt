package com.dantech.dreams.data.lesson

enum class LessonCategory(val displayName: String) {
    BASICS("Basics"),
    SDF("SDF"),
    NOISE("Noise"),
    POSTFX("Post-FX"),
    SHOWCASE("Showcase"),
    ;

    companion object {
        /** Categories shown in the Lesson tab — Showcase has its own tab and is excluded here. */
        fun lessonOnly(): List<LessonCategory> = entries.filter { it != SHOWCASE }
    }
}
