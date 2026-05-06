package com.dantech.dreams.ui.feature.lessonlist

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
import com.dantech.dreams.data.lesson.LessonCategory
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCategoriesScreen(
    onCategoryClick: (LessonCategory) -> Unit,
    vm: LessonCategoriesViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lessons") }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(ui.categories, key = { it.category.name }) { item ->
                CategoryCard(
                    item = item,
                    onClick = { onCategoryClick(item.category) },
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    item: LessonCategoryItem,
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
                text = item.category.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${item.count} lessons",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
