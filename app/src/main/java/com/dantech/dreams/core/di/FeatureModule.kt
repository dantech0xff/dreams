package com.dantech.dreams.core.di

import com.dantech.dreams.ui.feature.lesson.LessonDetailViewModel
import com.dantech.dreams.ui.feature.lessonlist.LessonCategoriesViewModel
import com.dantech.dreams.ui.feature.lessonlist.LessonListViewModel
import com.dantech.dreams.ui.feature.showcase.ShowcaseListViewModel
import com.dantech.dreams.ui.feature.showcase.ShowcaseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureModule = module {
    viewModel { LessonCategoriesViewModel(get()) }
    viewModel { (categoryName: String) -> LessonListViewModel(get(), get(), categoryName) }
    viewModel { (lessonId: String) -> LessonDetailViewModel(get(), get(), lessonId) }
    viewModel { ShowcaseListViewModel(get()) }
    viewModel { (lessonId: String) -> ShowcaseViewModel(get(), lessonId) }
}
