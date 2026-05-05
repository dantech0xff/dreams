package com.dantech.dreams

import android.app.Application
import com.dantech.dreams.data.lesson.LessonRegistry

class DreamsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LessonRegistry.bootstrap()
        if (BuildConfig.DEBUG) {
            val failures = LessonRegistry.validateAll()
            failures.forEach { (id, msg) ->
                android.util.Log.e("LessonRegistry", "$id => $msg")
            }
        }
    }
}
