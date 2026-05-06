package com.dantech.dreams.core.di

import com.dantech.dreams.ui.feature.gallery.GalleryViewModel
import com.dantech.dreams.ui.feature.landing.LandingViewModel
import com.dantech.dreams.ui.feature.lesson.LessonDetailViewModel
import com.dantech.dreams.ui.feature.showcase.ShowcaseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureModule = module {
    viewModel { LandingViewModel() }
    viewModel { GalleryViewModel(get(), get(), get()) }
    viewModel { (lessonId: String) -> LessonDetailViewModel(get(), get(), lessonId) }
    viewModel { (lessonId: String) -> ShowcaseViewModel(get(), lessonId) }
}
