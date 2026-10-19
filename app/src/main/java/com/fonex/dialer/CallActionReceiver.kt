package com.fonex.dialer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telecom.Call

@Suppress("DEPRECATION")
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_END) return
        val call = CallManager.primary() ?: return
        if (call.state == Call.STATE_RINGING) call.reject(false, null) else call.disconnect()
    }

    companion object {
        const val ACTION_END = "com.fonex.dialer.ACTION_END"
    }
}
