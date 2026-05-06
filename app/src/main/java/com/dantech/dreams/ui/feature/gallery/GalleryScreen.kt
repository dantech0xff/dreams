package com.dantech.dreams.ui.feature.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.ui.feature.settings.SettingsSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onLessonClick: (String) -> Unit,
    vm: GalleryViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var settingsOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AGSL Playground") },
                actions = {
                    IconButton(onClick = { settingsOpen = true }) {
                        Text("⚙")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = ui.selectedTabIndex) {
                ui.categories.forEachIndexed { i, cat ->
                    Tab(
                        selected = i == ui.selectedTabIndex,
                        onClick = { vm.selectTab(i) },
                        text = { Text(cat.displayName, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            if (ui.lessons.isEmpty()) {
                Text(
                    text = "No lessons in ${ui.categories[ui.selectedTabIndex].displayName} yet.",
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn {
                    items(ui.lessons, key = { it.id }) { lesson ->
                        LessonCard(
                            lesson = lesson,
                            onClick = { onLessonClick(lesson.id) },
                            isFavorite = lesson.id in ui.favorites,
                            onToggleFavorite = { vm.toggleFavorite(lesson.id) },
                        )
                    }
                }
            }
        }
    }

    if (settingsOpen) {
        SettingsSheet(onDismiss = { settingsOpen = false })
    }
}
