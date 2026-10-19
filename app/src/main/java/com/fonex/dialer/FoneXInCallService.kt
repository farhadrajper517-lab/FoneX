package com.fonex.dialer

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService

@Suppress("DEPRECATION")
class FoneXInCallService : InCallService() {

    private val changeListener: () -> Unit = { CallNotifications.update(this) }

    override fun onCreate() {
        super.onCreate()
        CallManager.service = this
        CallManager.addListener(changeListener)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.add(call)
        // Incoming calls surface through the full-screen notification; outgoing calls open the UI directly.
        if (call.state != Call.STATE_RINGING) {
            startActivity(InCallActivity.intent(this))
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.remove(call)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.notifyChanged()
    }

    override fun onDestroy() {
        CallManager.removeListener(changeListener)
        if (CallManager.service === this) CallManager.service = null
        CallNotifications.update(this)
        super.onDestroy()
    }
}
