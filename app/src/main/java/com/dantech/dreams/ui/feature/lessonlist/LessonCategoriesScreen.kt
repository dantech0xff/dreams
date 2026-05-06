package com.dantech.dreams.ui.feature.lessonlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    LazyColumn(Modifier.fillMaxSize()) {
        items(ui.categories, key = { it.category.name }) { item ->
            CategoryCard(
                item = item,
                onClick = { onCategoryClick(item.category) },
            )
        }
    }
}

@Composable
private fun CategoryCard(
    item: LessonCategoryItem,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
    ) {
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
