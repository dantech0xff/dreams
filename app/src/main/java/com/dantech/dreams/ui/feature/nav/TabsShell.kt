package com.dantech.dreams.ui.feature.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dantech.dreams.ui.feature.lessonlist.LessonCategoriesScreen
import com.dantech.dreams.ui.feature.settings.SettingsScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseListScreen

/**
 * The 3-tab shell rendered for [Route.Main]. Owns the bottom bar; tabs are content slots
 * scoped via [rememberSaveableStateHolder] so each tab's UI state (scroll position, etc.)
 * survives switching between tabs.
 *
 * Drill-down goes through [onDrillDown] — the host pushes fullscreen routes on the outer
 * back stack, which removes this shell from composition entirely.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsShell(onDrillDown: (Route) -> Unit) {
    var currentTab by rememberSaveable { mutableStateOf(TabKey.LESSON) }
    val tabStateHolder = rememberSaveableStateHolder()

    Scaffold(
        bottomBar = {
            DreamsBottomBar(currentTab = currentTab, onTabSelected = { currentTab = it })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            tabStateHolder.SaveableStateProvider(currentTab.name) {
                when (currentTab) {
                    TabKey.LESSON -> LessonCategoriesScreen(
                        onCategoryClick = { cat -> onDrillDown(Route.LessonList(cat.name)) },
                    )
                    TabKey.SHOWCASE -> ShowcaseListScreen(
                        onShowcaseClick = { id -> onDrillDown(Route.Showcase(id)) },
                    )
                    TabKey.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
