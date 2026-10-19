package com.fonex.dialer

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("fonex", Context.MODE_PRIVATE)

    fun themeMode(c: Context): Int =
        sp(c).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(c: Context, mode: Int) {
        sp(c).edit().putInt("theme", mode).apply()
    }

    /** "ask" or a key produced by CallHelper.key(handle). */
    fun defaultSim(c: Context): String = sp(c).getString("sim", "ask") ?: "ask"

    fun setDefaultSim(c: Context, key: String) {
        sp(c).edit().putString("sim", key).apply()
    }
}
