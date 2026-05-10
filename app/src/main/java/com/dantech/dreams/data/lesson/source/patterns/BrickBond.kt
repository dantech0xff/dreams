package com.dantech.dreams.data.lesson.source.patterns

import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonControl
import com.dantech.dreams.data.lesson.LessonModel
import com.dantech.dreams.data.lesson.LessonRegistry
import kotlinx.collections.immutable.persistentListOf

object BrickBond {
    val id = "patterns-10-brick-bond"
    private val SOURCE = """
        uniform float2 resolution;
        uniform float scale;
        uniform float mortar;
        uniform float randomness;
        float hash21(float2 p) {
            return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
        }
        half4 main(float2 fragCoord) {
            float2 uv = fragCoord / resolution;
            uv.x *= resolution.x / resolution.y;
            float2 brickUv = uv * float2(scale * 1.7, scale);
            float row = floor(brickUv.y);
            brickUv.x += (hash21(float2(row, 5.3)) - 0.5) * randomness * 0.6;
            float2 tile = floor(brickUv);
            float2 cell = fract(brickUv);
            float splitX = step(1.0 - randomness * 0.55, hash21(tile + float2(11.7, 11.7)));
            float splitY = (1.0 - splitX) * step(1.0 - randomness * 0.35, hash21(tile + float2(29.1, 29.1)));
            float2 divisions = float2(1.0 + splitX, 1.0 + splitY);
            float2 local = fract(cell * divisions);
            float2 subTile = floor(cell * divisions);
            float edgeX = min(local.x, 1.0 - local.x);
            float edgeY = min(local.y, 1.0 - local.y);
            float edge = min(edgeX, edgeY);
            float grout = 1.0 - smoothstep(mortar, mortar + 0.02, edge);
            float brickId = hash21(tile + subTile * 0.37);
            float shade = 0.15 * sin(brickId * 12.0 + row * 2.3);
            half3 mortarColor = half3(0.05, 0.055, 0.06);
            half3 brickA = half3(0.70, 0.18, 0.10);
            half3 brickB = half3(0.95, 0.42, 0.18);
            half3 brick = mix(brickA, brickB, half(0.5 + shade));
            return half4(mix(brick, mortarColor, half(grout)), 1.0);
        }
    """.trimIndent()

    init {
        LessonRegistry.register(
            LessonModel(
                id = id, title = "Brick Bond", category = LessonCategory.PATTERNS, complexity = 3,
                conceptIntro = "Hash each cell before fract() so the wall allocates stable random brick chunks.",
                learningNotes = persistentListOf(
                    "Scaling x and y differently makes rectangular cells instead of square tiles.",
                    "Each cell hash decides whether the base brick stays whole or splits into smaller chunks.",
                    "Distance to the nearest cell edge becomes a mortar mask.",
                ),
                agslSource = SOURCE,
                controls = persistentListOf(
                    LessonControl.FloatRange("Scale", "scale", 3f, 18f, 7f),
                    LessonControl.FloatRange("Mortar", "mortar", 0.01f, 0.15f, 0.05f),
                    LessonControl.FloatRange("Randomness", "randomness", 0f, 1f, 0.65f),
                ),
            )
        )
    }
}
