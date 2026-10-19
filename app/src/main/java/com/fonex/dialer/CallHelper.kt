package com.fonex.dialer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object CallHelper {

    private fun telecom(ctx: Context) = ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    /** SIM-backed calling accounts (falls back to all call-capable accounts if none are flagged as SIM). */
    fun simAccounts(ctx: Context): List<PhoneAccountHandle> {
        return try {
            val tm = telecom(ctx)
            val all = tm.callCapablePhoneAccounts
            val sims = all.filter { h ->
                tm.getPhoneAccount(h)?.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) == true
            }
            if (sims.isNotEmpty()) sims else all
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun key(h: PhoneAccountHandle): String = h.componentName.flattenToString() + "|" + h.id

    fun describe(ctx: Context, h: PhoneAccountHandle, fallbackIndex: Int): String {
        var slot = fallbackIndex
        var carrier = ""
        try {
            carrier = telecom(ctx).getPhoneAccount(h)?.label?.toString() ?: ""
            val sm = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val info = sm.activeSubscriptionInfoList?.firstOrNull {
                it.subscriptionId.toString() == h.id || it.iccId == h.id
            }
            if (info != null) {
                slot = info.simSlotIndex
                val cn = info.carrierName?.toString()
                if (!cn.isNullOrBlank()) carrier = cn
            }
        } catch (e: SecurityException) {
            // permission missing: keep fallback label
        }
        return "SIM ${slot + 1}" + if (carrier.isNotBlank()) " · $carrier" else ""
    }

    private fun clean(number: String) = number.replace(Regex("[\\s\\-()]"), "")

    /** Entry point used by dial pad, contacts and history. Handles permission, SIM choice and placing the call. */
    fun dial(activity: Activity, raw: String) {
        val number = clean(raw)
        if (number.isEmpty()) return
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(activity, "Allow the Phone permission to place calls", Toast.LENGTH_LONG).show()
            (activity as? MainActivity)?.requestPerms()
            return
        }
        val handles = simAccounts(activity)
        if (handles.size <= 1) {
            place(activity, number, handles.firstOrNull())
            return
        }
        val preferred = handles.firstOrNull { key(it) == Prefs.defaultSim(activity) }
        if (preferred != null) {
            place(activity, number, preferred)
            return
        }
        val labels = handles.mapIndexed { i, h -> describe(activity, h, i) }.toTypedArray()
        MaterialAlertDialogBuilder(activity)
            .setTitle("Call $number with")
            .setItems(labels) { _, i -> place(activity, number, handles[i]) }
            .show()
    }

    fun place(ctx: Context, number: String, handle: PhoneAccountHandle?) {
        val extras = Bundle()
        if (handle != null) extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        try {
            telecom(ctx).placeCall(Uri.fromParts("tel", number, null), extras)
        } catch (e: SecurityException) {
            Toast.makeText(ctx, "Phone permission missing", Toast.LENGTH_LONG).show()
        }
    }
}
