package com.dantech.dreams.ui.feature.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.core.motion.rememberAppMotionState
import com.dantech.dreams.ui.feature.lesson.LessonDetailScreen
import com.dantech.dreams.ui.feature.lesson.LessonDetailViewModel
import com.dantech.dreams.ui.feature.lessonlist.LessonCategoriesScreen
import com.dantech.dreams.ui.feature.lessonlist.LessonListScreen
import com.dantech.dreams.ui.feature.settings.SettingsScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseListScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainShell() {
    val motion = rememberAppMotionState()
    val topLevel = rememberTopLevelBackStack(Route.LessonRoot)

    val showBar by remember(topLevel) {
        derivedStateOf { isBarVisibleRoute(topLevel.backStack.lastOrNull()) }
    }

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            val durationMs = motion.transitionDurationMs
            Scaffold(
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBar,
                        enter = if (motion.reducedMotion) {
                            fadeIn(snap())
                        } else {
                            slideInVertically(tween(durationMs)) { it } + fadeIn(tween(durationMs))
                        },
                        exit = if (motion.reducedMotion) {
                            fadeOut(snap())
                        } else {
                            slideOutVertically(tween(durationMs)) { it } + fadeOut(tween(durationMs))
                        },
                    ) {
                        DreamsBottomBar(topLevel = topLevel)
                    }
                },
            ) { padding ->
                NavDisplay(
                    backStack = topLevel.backStack,
                    onBack = { topLevel.removeLast() },
                    modifier = Modifier.padding(padding),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        if (motion.reducedMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(durationMs)) togetherWith fadeOut(tween(durationMs))
                        }
                    },
                    popTransitionSpec = {
                        if (motion.reducedMotion) {
                            fadeIn(snap()) togetherWith fadeOut(snap())
                        } else {
                            fadeIn(tween(durationMs)) togetherWith fadeOut(tween(durationMs))
                        }
                    },
                    entryProvider = entryProvider {
                        entry<Route.LessonRoot> {
                            LessonCategoriesScreen(
                                onCategoryClick = { cat -> topLevel.add(Route.LessonList(cat.name)) },
                            )
                        }
                        entry<Route.LessonList> { route ->
                            LessonListScreen(
                                categoryName = route.categoryName,
                                onLessonClick = { id -> topLevel.add(routeForLessonId(id)) },
                                onBack = { topLevel.removeLast() },
                            )
                        }
                        entry<Route.LessonDetail> { route ->
                            LessonDetailScreen(
                                onBack = { topLevel.removeLast() },
                                vm = koinViewModel<LessonDetailViewModel> { parametersOf(route.lessonId) },
                            )
                        }
                        entry<Route.ShowcaseRoot> {
                            ShowcaseListScreen(
                                onShowcaseClick = { id -> topLevel.add(Route.Showcase(id)) },
                            )
                        }
                        entry<Route.Showcase> { route ->
                            ShowcaseScreen(
                                onBack = { topLevel.removeLast() },
                                vm = koinViewModel<ShowcaseViewModel> { parametersOf(route.lessonId) },
                            )
                        }
                        entry<Route.SettingsRoot> { SettingsScreen() }
                    },
                )
            }
        }
    }
}

/**
 * Bottom bar visible on every Lesson-tab destination (root/list/detail) and on each
 * tab root. Hidden only on the Showcase fullscreen route, which renders an edge-to-edge
 * shader and would otherwise have its hint banner clipped by the bar.
 */
private fun isBarVisibleRoute(route: Route?): Boolean = when (route) {
    is Route.LessonRoot, is Route.LessonList, is Route.LessonDetail,
    is Route.ShowcaseRoot, is Route.SettingsRoot -> true
    is Route.Showcase, null -> false
}
