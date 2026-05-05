package com.dantech.dreams.ui.playground.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dantech.dreams.data.lesson.LessonCategory
import com.dantech.dreams.data.lesson.LessonRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onLessonClick: (String) -> Unit) {
    val categories = LessonCategory.values().toList()
    var selected by remember { mutableIntStateOf(0) }
    val current = categories[selected]
    val lessons = remember(current) { LessonRegistry.byCategory(current) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AGSL Playground") })
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selected) {
                categories.forEachIndexed { i, cat ->
                    Tab(
                        selected = i == selected,
                        onClick = { selected = i },
                        text = { Text(cat.displayName, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
            if (lessons.isEmpty()) {
                Text(
                    text = "No lessons in ${current.displayName} yet.",
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn {
                    items(lessons, key = { it.id }) { lesson ->
                        LessonCard(lesson = lesson, onClick = { onLessonClick(lesson.id) })
                    }
                }
            }
        }
    }
}
