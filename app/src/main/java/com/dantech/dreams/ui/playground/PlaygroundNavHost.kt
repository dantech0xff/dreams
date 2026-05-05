package com.dantech.dreams.ui.playground

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dantech.dreams.ui.playground.gallery.GalleryScreen
import com.dantech.dreams.ui.playground.lesson.LessonDetailScreen
import com.dantech.dreams.ui.playground.landing.LandingScreen
import com.dantech.dreams.ui.playground.showcase.ShowcaseScreen

object Routes {
    const val LANDING = "landing"
    const val GALLERY = "gallery"
    const val LESSON = "lesson/{id}"
    const val SHOWCASE = "showcase/{id}"
    fun lesson(id: String) = "lesson/$id"
    fun showcase(id: String) = "showcase/$id"
}

@Composable
fun PlaygroundApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LANDING) {
        composable(Routes.LANDING) {
            LandingScreen(
                onOpenGallery = { nav.navigate(Routes.GALLERY) },
            )
        }
        composable(Routes.GALLERY) {
            GalleryScreen(onLessonClick = { id ->
                val target = if (id.startsWith("showcase-")) Routes.showcase(id) else Routes.lesson(id)
                nav.navigate(target)
            })
        }
        composable(
            route = Routes.LESSON,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            LessonDetailScreen(lessonId = id, onBack = { nav.popBackStack() })
        }
        composable(
            route = Routes.SHOWCASE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ShowcaseScreen(lessonId = id, onBack = { nav.popBackStack() })
        }
    }
}
