package com.dantech.dreams.data.prefs

enum class ThemeMode(val storageValue: String) {
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        val DEFAULT = DARK

        fun fromStorageValue(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: DEFAULT
    }
}
