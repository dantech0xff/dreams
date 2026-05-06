package com.dantech.dreams.ui.feature.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.snap
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
import com.dantech.dreams.ui.feature.gallery.GalleryScreen
import com.dantech.dreams.ui.feature.landing.LandingScreen
import com.dantech.dreams.ui.feature.lesson.LessonDetailScreen
import com.dantech.dreams.ui.feature.lesson.LessonDetailViewModel
import com.dantech.dreams.ui.feature.showcase.ShowcaseScreen
import com.dantech.dreams.ui.feature.showcase.ShowcaseViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaygroundApp() {
    val motion = rememberAppMotionState()
    val backStack = rememberNavBackStack(Route.Landing)

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            val durationMs = motion.transitionDurationMs
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
                        fadeIn(androidx.compose.animation.core.tween(durationMs)) togetherWith
                            fadeOut(androidx.compose.animation.core.tween(durationMs))
                    }
                },
                popTransitionSpec = {
                    if (motion.reducedMotion) {
                        fadeIn(snap()) togetherWith fadeOut(snap())
                    } else {
                        fadeIn(androidx.compose.animation.core.tween(durationMs)) togetherWith
                            fadeOut(androidx.compose.animation.core.tween(durationMs))
                    }
                },
                entryProvider = entryProvider {
                    entry<Route.Landing> {
                        LandingScreen(onOpenGallery = { backStack.add(Route.Gallery) })
                    }
                    entry<Route.Gallery> {
                        GalleryScreen(onLessonClick = { id -> backStack.add(routeForLessonId(id)) })
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

