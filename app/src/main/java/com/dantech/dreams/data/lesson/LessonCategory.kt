package com.dantech.dreams.data.lesson

enum class LessonCategory(val displayName: String, val tagline: String) {
    BASICS("Basics", "Start where pixels meet math"),
    PATTERNS("Patterns", "Tiles, stripes, and repetition that hypnotize"),
    COLOR("Color", "Palettes, gradients, and tone curves"),
    SDF("SDF", "Crisp geometry sculpted from equations"),
    NOISE("Noise", "Random fields that read as plasma, lava, smoke"),
    MOTION("Motion", "Time-driven curves, easing, and oscillators"),
    FRACTALS("Fractals", "Self-similar worlds at every zoom"),
    LIGHTING("Lighting", "Lambert, specular, and fakes that sell depth"),
    INTERACTIVE("Interactive", "Touch-driven shader experiments"),
    POSTFX("Post-FX", "Bend, blur, and dissolve real Compose UI"),
    SHOWCASE("Showcase", "Cinema-mode shaders, ready for the recorder"),
    ;

    companion object {
        /** Categories shown in the Lesson tab — Showcase has its own tab and is excluded here. */
        fun lessonOnly(): List<LessonCategory> = entries.filter { it != SHOWCASE }
    }
}
