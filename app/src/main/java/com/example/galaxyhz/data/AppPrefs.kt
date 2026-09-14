package com.example.galaxyhz.data

import android.content.Context

/** Small wrapper around SharedPreferences for app-level settings. */
class AppPrefs(context: Context) {

    private val sp = context.getSharedPreferences("galaxyhz_prefs", Context.MODE_PRIVATE)

    var setupDone: Boolean
        get() = sp.getBoolean("setup_done", false)
        set(value) = sp.edit().putBoolean("setup_done", value).apply()

    var language: String
        get() = sp.getString("language", "en") ?: "en"
        set(value) = sp.edit().putString("language", value).apply()

    var warnExperimental: Boolean
        get() = sp.getBoolean("warn_experimental", true)
        set(value) = sp.edit().putBoolean("warn_experimental", value).apply()
}
