package com.fonex.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.PhoneLookup
import androidx.core.content.ContextCompat

data class Contact(val id: Long, val name: String, val number: String)

object ContactsRepo {
    private val nameCache = HashMap<String, String?>()

    fun hasPerm(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    fun loadAll(ctx: Context): List<Contact> {
        if (!hasPerm(ctx)) return emptyList()
        synchronized(nameCache) { nameCache.clear() }
        val out = ArrayList<Contact>()
        try {
            val proj = arrayOf(Phone.CONTACT_ID, Phone.DISPLAY_NAME, Phone.NUMBER)
            ctx.contentResolver.query(
                Phone.CONTENT_URI, proj, null, null,
                "${Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    out.add(Contact(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: ""))
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out.distinctBy { it.name + "|" + it.number.filter(Char::isDigit) }
    }

    fun nameFor(ctx: Context, number: String): String? {
        if (number.isBlank() || !hasPerm(ctx)) return null
        synchronized(nameCache) {
            if (nameCache.containsKey(number)) return nameCache[number]
        }
        var name: String? = null
        try {
            val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(PhoneLookup.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) name = it.getString(0) }
        } catch (e: Exception) {
            // ignore, fall back to the raw number
        }
        synchronized(nameCache) { nameCache[number] = name }
        return name
    }
}
