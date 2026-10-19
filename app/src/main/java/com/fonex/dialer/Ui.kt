package com.fonex.dialer

import android.content.Context
import android.util.TypedValue
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

fun Context.rippleRes(): Int {
    val t = TypedValue()
    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, t, true)
    return t.resourceId
}

fun Context.onSurfaceColor(): Int {
    val t = TypedValue()
    theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, t, true)
    return t.data
}

val AVATAR_COLORS = intArrayOf(
    0xFF00796B.toInt(), 0xFF1565C0.toInt(), 0xFF6A1B9A.toInt(),
    0xFFEF6C00.toInt(), 0xFFAD1457.toInt(), 0xFF455A64.toInt()
)

/** Tiny background-work helper: run off the main thread, deliver result on the main thread. */
object Bg {
    private val exec = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())

    fun <T> run(work: () -> T, done: (T) -> Unit) {
        exec.execute {
            val result = work()
            main.post { done(result) }
        }
    }
}
