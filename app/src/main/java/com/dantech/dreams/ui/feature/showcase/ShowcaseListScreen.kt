package com.dantech.dreams.ui.feature.showcase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dantech.dreams.data.lesson.LessonModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseListScreen(
    onShowcaseClick: (String) -> Unit,
    vm: ShowcaseListViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Showcases") }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(ui.showcases, key = { it.id }) { lesson ->
                ShowcaseCard(
                    lesson = lesson,
                    onClick = { onShowcaseClick(lesson.id) },
                )
            }
        }
    }
}

@Composable
private fun ShowcaseCard(
    lesson: LessonModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = lesson.conceptIntro,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
            lesson.screenRecordingHint?.let { hint ->
                Text(
                    text = "Recording: $hint",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}
