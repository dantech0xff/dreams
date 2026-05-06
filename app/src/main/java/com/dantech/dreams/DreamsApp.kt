package com.dantech.dreams

import android.app.Application
import android.util.Log
import com.dantech.dreams.core.di.appModule
import com.dantech.dreams.core.di.dataModule
import com.dantech.dreams.core.di.featureModule
import com.dantech.dreams.domain.lesson.LessonRepository
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

open class DreamsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
            androidContext(this@DreamsApp)
            modules(koinModules())
        }
        if (BuildConfig.DEBUG) {
            val repo: LessonRepository = get()
            repo.validate().forEach { (id, msg) ->
                Log.e("LessonRepo", "$id => $msg")
            }
        }
    }

    protected open fun koinModules() = listOf(appModule, dataModule, featureModule)
}
