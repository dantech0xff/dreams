package com.dantech.dreams.ui.playground.landing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutAgslSheet(onDismiss: () -> Unit) {
    rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("About AGSL", style = MaterialTheme.typography.titleLarge)
            Text(
                "AGSL (Android Graphics Shading Language) is a Skia-flavored variant of GLSL " +
                    "that runs as a Compose ShaderBrush or RenderEffect on Android 13+.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text("References", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 16.dp))
            Text("• developer.android.com/develop/ui/views/graphics/agsl")
            Text("• thebookofshaders.com")
            Text("• iquilezles.org/articles/")
        }
    }
}
