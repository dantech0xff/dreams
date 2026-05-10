package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object HerringboneTiles {
    val id = "patterns-08-herringbone-tiles"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float scale;
        uniform float width;
        uniform float offset;
        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * resolution) / resolution.y * scale;
            float2 tile = floor(uv);
            float2 cell = fract(uv) - 0.5;
            float parity = mod(tile.x + tile.y, 2.0);
            float2 p = parity < 1.0 ? float2(cell.x, cell.y) : float2(-cell.x, cell.y);
            p.x += (parity - 0.5) * offset;
            float stripe = abs(fract((p.x + p.y) * 3.0) - 0.5);
            float line = 1.0 - smoothstep(width, width + 0.035, stripe);
            float joint = max(
                1.0 - smoothstep(0.46, 0.49, abs(cell.x)),
                1.0 - smoothstep(0.46, 0.49, abs(cell.y))
            );
            half3 grout = half3(0.04, 0.05, 0.08);
            half3 woodA = half3(0.92, 0.58, 0.25);
            half3 woodB = half3(0.50, 0.22, 0.12);
            half3 wood = mix(woodB, woodA, half(line));
            return half4(mix(wood, grout, half(joint)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Herringbone Tiles", category = LessonCategory.PATTERNS, complexity = 3,
                conceptIntro = "Alternate mirrored tile coordinates so diagonal stripes meet as a herringbone zig-zag.",
                learningNotes = persistentListOf(
                    "mod(tile.x + tile.y, 2) splits the grid into alternating orientations.",
                    "Mirroring one coordinate flips the diagonal grain without new geometry.",
                    "A second edge mask draws grout lines over the procedural wood stripes.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Scale", "scale", 3f, 18f, 8f),
                    LessonControl.FloatRange("Width", "width", 0.04f, 0.35f, 0.16f),
                    LessonControl.FloatRange("Offset", "offset", -0.5f, 0.5f, 0.18f),
                ),
                screenRecordingHint = "Sweep scale 4 -> 14 to make the tile rhythm tighten.",
            )
        )
    }
}
