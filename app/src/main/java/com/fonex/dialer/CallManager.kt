package com.fonex.dialer

import android.telecom.Call
import android.telecom.InCallService
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

/** Process-wide holder for live calls delivered by FoneXInCallService. All callbacks arrive on the main thread. */
@Suppress("DEPRECATION")
object CallManager {
    val calls = CopyOnWriteArrayList<Call>()
    var service: InCallService? = null

    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = notifyChanged()
        override fun onDetailsChanged(call: Call, details: Call.Details) = notifyChanged()
    }

    fun add(call: Call) {
        calls.add(call)
        call.registerCallback(callback)
        notifyChanged()
    }

    fun remove(call: Call) {
        call.unregisterCallback(callback)
        calls.remove(call)
        notifyChanged()
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun notifyChanged() { listeners.forEach { it() } }

    /** The call the UI should focus on: ringing first, then any non-held live call, then whatever is left. */
    fun primary(): Call? =
        calls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: calls.firstOrNull { it.state != Call.STATE_HOLDING && it.state != Call.STATE_DISCONNECTED }
            ?: calls.firstOrNull()
}
