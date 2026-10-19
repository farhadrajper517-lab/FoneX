package com.fonex.dialer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fonex.dialer.databinding.ActivityInCallBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

@Suppress("DEPRECATION")
class InCallActivity : AppCompatActivity() {

    private lateinit var b: ActivityInCallBinding
    private val handler = Handler(Looper.getMainLooper())
    private val listener: () -> Unit = { render() }
    private val ticker = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000)
        }
    }
    private var closing = false
    private var selecting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        b = ActivityInCallBinding.inflate(layoutInflater)
        setContentView(b.root)
        buildKeypad()

        b.btnAnswer.setOnClickListener {
            CallManager.primary()?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
        b.btnEnd.setOnClickListener {
            val c = CallManager.primary()
            if (c != null) {
                if (c.state == Call.STATE_RINGING) c.reject(false, null) else c.disconnect()
            }
        }
        b.btnMute.setOnClickListener {
            val s = CallManager.service
            if (s != null) s.setMuted(s.callAudioState?.isMuted != true)
        }
        b.btnSpeaker.setOnClickListener {
            val s = CallManager.service
            if (s != null) {
                val onSpeaker = s.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
                s.setAudioRoute(
                    if (onSpeaker) CallAudioState.ROUTE_WIRED_OR_EARPIECE else CallAudioState.ROUTE_SPEAKER
                )
            }
        }
        b.btnHold.setOnClickListener {
            val c = CallManager.primary()
            if (c != null) {
                if (c.state == Call.STATE_HOLDING) c.unhold() else c.hold()
            }
        }
        b.btnKeypad.setOnClickListener {
            b.keypad.visibility = if (b.keypad.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        handleAnswerExtra(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        closing = false
        handleAnswerExtra(intent)
        render()
    }

    private fun handleAnswerExtra(i: Intent) {
        if (i.getBooleanExtra(EXTRA_ANSWER, false)) {
            i.removeExtra(EXTRA_ANSWER)
            val c = CallManager.primary()
            if (c != null && c.state == Call.STATE_RINGING) c.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    override fun onStart() {
        super.onStart()
        CallManager.addListener(listener)
        handler.post(ticker)
    }

    override fun onStop() {
        CallManager.removeListener(listener)
        handler.removeCallbacksAndMessages(null)
        super.onStop()
    }

    private fun buildKeypad() {
        for (r in listOf("123", "456", "789", "*0#")) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52))
            }
            for (ch in r) {
                val key = TextView(this).apply {
                    text = ch.toString()
                    textSize = 24f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    setTextColor(onSurfaceColor())
                    setBackgroundResource(rippleRes())
                    setOnClickListener { sendDtmf(ch) }
                }
                row.addView(key)
            }
            b.keypad.addView(row)
        }
    }

    private fun sendDtmf(ch: Char) {
        val c = CallManager.primary() ?: return
        c.playDtmfTone(ch)
        handler.postDelayed({ c.stopDtmfTone() }, 160)
    }

    private fun render() {
        val call = CallManager.primary()
        if (call == null) {
            b.tvState.text = "Call ended"
            if (!closing) {
                closing = true
                handler.postDelayed({
                    if (CallManager.primary() == null) finish() else closing = false
                }, 700)
            }
            return
        }

        val number = call.details.handle?.schemeSpecificPart ?: ""
        val name = call.details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: ContactsRepo.nameFor(this, number)
        b.tvName.text = name ?: number.ifBlank { "Unknown" }
        b.tvNumber.text = if (name != null) number else ""

        val state = call.state
        b.tvState.text = when (state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING -> "Calling…"
            Call.STATE_CONNECTING -> "Connecting…"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "Choose SIM"
            Call.STATE_HOLDING -> "On hold"
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> "Call ended"
            Call.STATE_ACTIVE -> {
                val start = call.details.connectTimeMillis
                val secs = if (start > 0) (System.currentTimeMillis() - start) / 1000 else 0L
                formatDuration(secs)
            }
            else -> ""
        }

        val others = CallManager.calls.size - 1
        b.tvOther.text = if (others > 0) "+$others other call" + (if (others > 1) "s" else "") else ""

        val ringing = state == Call.STATE_RINGING
        b.btnAnswer.visibility = if (ringing) View.VISIBLE else View.GONE
        b.gap.visibility = if (ringing) View.VISIBLE else View.GONE
        b.optionsRow.visibility = if (ringing) View.GONE else View.VISIBLE
        if (ringing) b.keypad.visibility = View.GONE

        val audio = CallManager.service?.callAudioState
        b.btnMute.text = if (audio?.isMuted == true) "Unmute" else "Mute"
        b.btnSpeaker.text = if (audio?.route == CallAudioState.ROUTE_SPEAKER) "Earpiece" else "Speaker"
        b.btnHold.text = if (state == Call.STATE_HOLDING) "Resume" else "Hold"

        if (state == Call.STATE_SELECT_PHONE_ACCOUNT && !selecting) {
            selecting = true
            askSim(call)
        }
    }

    private fun askSim(call: Call) {
        val handles = CallHelper.simAccounts(this)
        if (handles.isEmpty()) {
            selecting = false
            call.disconnect()
            return
        }
        val labels = handles.mapIndexed { i, h -> CallHelper.describe(this, h, i) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("Call with")
            .setCancelable(false)
            .setItems(labels) { _, i ->
                selecting = false
                call.phoneAccountSelected(handles[i], false)
            }
            .setNegativeButton("Cancel") { _, _ ->
                selecting = false
                call.disconnect()
            }
            .show()
    }

    private fun formatDuration(totalSecs: Long): String {
        val h = totalSecs / 3600
        val m = (totalSecs % 3600) / 60
        val s = totalSecs % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    companion object {
        const val EXTRA_ANSWER = "fonex_answer"

        fun intent(ctx: Context): Intent =
            Intent(ctx, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
