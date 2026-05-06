package com.dantech.dreams.ui.feature.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dantech.dreams.core.motion.LocalSharedTransitionScope
import com.dantech.dreams.core.motion.rememberAppMotionState
import com.dantech.dreams.ui.feature.lesson.LessonDetailScreen
import com.dantech.dreams.ui.feature.lesson.LessonDetailViewModel
import com.dantech.dreams.ui.feature.lessonlist.LessonListScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Single root [NavDisplay] backed by one [Route] back stack. [Route.Main] hosts the 3-tab
 * shell and lives at the bottom; every other Route is a true fullscreen page pushed on
 * top — the tab Scaffold/bar is no longer in the composition tree at all when drilled in.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainShell() {
    val motion = rememberAppMotionState()
    val backStack = rememberNavBackStack(Route.Main)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    if (motion.reducedMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(tween(motion.transitionDurationMs)) togetherWith fadeOut(tween(motion.transitionDurationMs))
                    }
                },
                popTransitionSpec = {
                    if (motion.reducedMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(tween(motion.transitionDurationMs)) togetherWith fadeOut(tween(motion.transitionDurationMs))
                    }
                },
                entryProvider = entryProvider {
                    entry<Route.Main> {
                        TabsShell(onDrillDown = { backStack.add(it) })
                    }
                    entry<Route.LessonList> { route ->
                        LessonListScreen(
                            categoryName = route.categoryName,
                            onLessonClick = { id -> backStack.add(routeForLessonId(id)) },
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Route.LessonDetail> { route ->
                        LessonDetailScreen(
                            onBack = { backStack.removeLastOrNull() },
                            vm = koinViewModel<LessonDetailViewModel> { parametersOf(route.lessonId) },
                        )
                    }
                    entry<Route.Showcase> { route ->
                        ShowcaseScreen(
                            onBack = { backStack.removeLastOrNull() },
                            vm = koinViewModel<ShowcaseViewModel> { parametersOf(route.lessonId) },
                        )
                    }
                },
            )
        }
    }
}
