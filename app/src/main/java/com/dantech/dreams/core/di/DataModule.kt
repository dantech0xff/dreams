package com.dantech.dreams.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.dantech.dreams.data.lesson.LessonRepositoryImpl
import com.dantech.dreams.data.prefs.UserPrefsRepository
import com.dantech.dreams.data.prefs.UserPrefsRepositoryImpl
import com.dantech.dreams.domain.lesson.LessonRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val PREFS_FILE = "dreams_prefs"

val dataModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.create(
            produceFile = { androidContext().preferencesDataStoreFile(PREFS_FILE) },
        )
    }
    single<LessonRepository> { LessonRepositoryImpl() }
    single<UserPrefsRepository> { UserPrefsRepositoryImpl(get()) }
}
