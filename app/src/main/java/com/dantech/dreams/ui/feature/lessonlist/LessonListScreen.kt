package com.dantech.dreams.ui.feature.lessonlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.ui.feature.common.LessonCard
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListScreen(
    categoryName: String,
    onLessonClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: LessonListViewModel = koinViewModel { parametersOf(categoryName) },
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val title = ui.category?.displayName ?: "Lessons"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                ui.error != null -> Text(
                    text = ui.error!!,
                    modifier = Modifier.padding(24.dp),
                )

                ui.lessons.isEmpty() -> Text(
                    text = "No lessons in $title yet.",
                    modifier = Modifier.padding(24.dp),
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
}
