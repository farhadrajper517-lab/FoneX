package com.fonex.dialer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat

@Suppress("DEPRECATION")
object CallNotifications {
    private const val CH_INCOMING = "fonex_incoming"
    private const val CH_ONGOING = "fonex_ongoing"
    private const val ID = 1001

    private fun ensureChannels(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < 26) return
        nm.createNotificationChannel(
            NotificationChannel(CH_INCOMING, "Incoming calls", NotificationManager.IMPORTANCE_HIGH)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ONGOING, "Ongoing calls", NotificationManager.IMPORTANCE_LOW)
        )
    }

    fun update(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val call = CallManager.primary()
        if (call == null) {
            nm.cancel(ID)
            return
        }
        ensureChannels(nm)

        val number = call.details.handle?.schemeSpecificPart ?: ""
        val title = call.details.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: ContactsRepo.nameFor(ctx, number)
            ?: number.ifBlank { "Unknown" }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(ctx, 0, InCallActivity.intent(ctx), flags)
        val end = PendingIntent.getBroadcast(
            ctx, 2, Intent(ctx, CallActionReceiver::class.java).setAction(CallActionReceiver.ACTION_END), flags
        )
        val ringing = call.state == Call.STATE_RINGING

        val b = NotificationCompat.Builder(ctx, if (ringing) CH_INCOMING else CH_ONGOING)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle(title)
            .setContentText(if (ringing) "Incoming call" else "Ongoing call")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(if (ringing) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)

        if (ringing) {
            val answer = PendingIntent.getActivity(
                ctx, 1, InCallActivity.intent(ctx).putExtra(InCallActivity.EXTRA_ANSWER, true), flags
            )
            b.setFullScreenIntent(open, true)
                .addAction(0, "Decline", end)
                .addAction(0, "Answer", answer)
        } else {
            b.addAction(0, "Hang up", end)
        }

        try {
            nm.notify(ID, b.build())
        } catch (e: SecurityException) {
            // notification permission not granted
        }
    }
}
