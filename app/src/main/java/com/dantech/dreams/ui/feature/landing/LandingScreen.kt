package com.dantech.dreams.ui.feature.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.lesson.source.showcase.AuroraRibbons
import com.dantech.dreams.ui.feature.common.AgslBrushCanvas
import com.dantech.dreams.ui.feature.common.rememberShaderTime
import com.dantech.dreams.ui.theme.Tokens
import org.koin.androidx.compose.koinViewModel

@Composable
fun LandingScreen(
    onOpenGallery: () -> Unit,
    vm: LandingViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        val timeState = rememberShaderTime(AuroraRibbons.SOURCE)
        AgslBrushCanvas(
            shaderSrc = AuroraRibbons.SOURCE,
            modifier = Modifier.fillMaxSize(),
            setUniforms = { shader -> shader.setFloatUniform("time", timeState.value) },
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "AGSL Playground",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Learn Android shaders by example",
                style = MaterialTheme.typography.titleMedium,
                color = Tokens.translucentLightOnDark,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onOpenGallery) {
                Text("Open Gallery")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.setAboutOpen(true) }) {
                Text("About AGSL", color = Color.White)
            }
        }
        if (ui.aboutOpen) {
            AboutAgslSheet(onDismiss = { vm.setAboutOpen(false) })
        }
    }
}
