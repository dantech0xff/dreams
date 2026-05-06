package com.dantech.dreams.core.motion

import android.content.Context
import android.provider.Settings

fun systemAnimatorEnabled(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) != 0f
