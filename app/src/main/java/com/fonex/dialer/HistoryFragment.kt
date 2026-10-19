package com.fonex.dialer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.CallLog.Calls
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fonex.dialer.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private data class Entry(
        val id: Long, val number: String, val name: String,
        val type: Int, val date: Long, val duration: Long
    )

    private var _b: FragmentHistoryBinding? = null
    private val adapter = RowAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentHistoryBinding.inflate(inflater, container, false)
        _b = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = _b ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.empty.setOnClickListener {
            if (!hasPerm(requireContext())) (activity as? MainActivity)?.requestPerms()
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun hasPerm(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED

    private fun reload() {
        val app = requireContext().applicationContext
        Bg.run({ query(app) }) { entries -> show(entries) }
    }

    private fun query(ctx: Context): List<Entry> {
        if (!hasPerm(ctx)) return emptyList()
        val out = ArrayList<Entry>()
        try {
            val proj = arrayOf(Calls._ID, Calls.NUMBER, Calls.CACHED_NAME, Calls.TYPE, Calls.DATE, Calls.DURATION)
            ctx.contentResolver.query(Calls.CONTENT_URI, proj, null, null, "${Calls.DATE} DESC")?.use { c ->
                while (c.moveToNext() && out.size < 300) {
                    out.add(
                        Entry(
                            c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "",
                            c.getInt(3), c.getLong(4), c.getLong(5)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
        return out
    }

    private fun show(entries: List<Entry>) {
        val b = _b ?: return
        val ctx = context ?: return
        val fmt = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
        adapter.submit(entries.map { e ->
            val (label, arrow, color) = when (e.type) {
                Calls.INCOMING_TYPE -> Triple("Incoming", "↙", 0xFF2E7D32.toInt())
                Calls.OUTGOING_TYPE -> Triple("Outgoing", "↗", 0xFF1565C0.toInt())
                Calls.MISSED_TYPE -> Triple("Missed", "↙", 0xFFC62828.toInt())
                Calls.REJECTED_TYPE -> Triple("Declined", "✕", 0xFF757575.toInt())
                Calls.BLOCKED_TYPE -> Triple("Blocked", "✕", 0xFF757575.toInt())
                Calls.VOICEMAIL_TYPE -> Triple("Voicemail", "✉", 0xFF6A1B9A.toInt())
                else -> Triple("Call", "•", Color.GRAY)
            }
            Row(
                title = e.name.ifBlank { e.number.ifBlank { "Private number" } },
                subtitle = "$label · ${fmt.format(Date(e.date))}",
                avatar = arrow,
                color = color,
                trailing = if (e.duration > 0) duration(e.duration) else "",
                onClick = { if (e.number.isNotBlank()) CallHelper.dial(requireActivity(), e.number) },
                onLong = { confirmDelete(e) }
            )
        })
        b.empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        b.empty.text = if (!hasPerm(ctx)) "Call log permission needed.\nTap here to allow it." else "No recent calls"
    }

    private fun confirmDelete(e: Entry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete this entry?")
            .setPositiveButton("Delete") { _, _ ->
                val app = requireContext().applicationContext
                Bg.run({
                    try {
                        app.contentResolver.delete(Calls.CONTENT_URI, "${Calls._ID}=?", arrayOf(e.id.toString()))
                    } catch (ex: Exception) {
                        0
                    }
                }) { reload() }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duration(sec: Long): String =
        String.format(Locale.US, "%d:%02d", sec / 60, sec % 60)

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
