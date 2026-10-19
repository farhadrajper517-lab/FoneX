package com.fonex.dialer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.fonex.dialer.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        _b = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = _b ?: return
        val ctx = requireContext()

        // Theme
        b.rgTheme.check(
            when (Prefs.themeMode(ctx)) {
                AppCompatDelegate.MODE_NIGHT_NO -> b.rbLight.id
                AppCompatDelegate.MODE_NIGHT_YES -> b.rbDark.id
                else -> b.rbSystem.id
            }
        )
        b.rgTheme.setOnCheckedChangeListener { _, id ->
            val mode = when (id) {
                b.rbLight.id -> AppCompatDelegate.MODE_NIGHT_NO
                b.rbDark.id -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode != Prefs.themeMode(ctx)) {
                Prefs.setThemeMode(ctx, mode)
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        // SIM selection
        val handles = CallHelper.simAccounts(ctx)
        if (handles.size < 2) {
            b.rgSim.visibility = View.GONE
            b.tvSimHint.text = if (handles.isEmpty()) {
                "No SIM information available. Allow the Phone permission."
            } else {
                "One SIM detected: " + CallHelper.describe(ctx, handles[0], 0)
            }
        } else {
            b.tvSimHint.text = "Choose which SIM FoneX uses for outgoing calls."
            val keys = handles.map { CallHelper.key(it) }
            val current = Prefs.defaultSim(ctx).takeIf { it in keys } ?: "ask"
            fun addOption(text: String, key: String) {
                val rb = RadioButton(ctx)
                rb.id = View.generateViewId()
                rb.text = text
                rb.tag = key
                b.rgSim.addView(rb)
                if (key == current) b.rgSim.check(rb.id)
            }
            addOption("Ask every time", "ask")
            handles.forEachIndexed { i, h -> addOption(CallHelper.describe(ctx, h, i), CallHelper.key(h)) }
            b.rgSim.setOnCheckedChangeListener { group, id ->
                val rb = group.findViewById<RadioButton>(id)
                if (rb != null) Prefs.setDefaultSim(ctx, rb.tag as String)
            }
        }

        b.btnSetDefault.setOnClickListener { (activity as? MainActivity)?.requestDefaultDialer() }
    }

    override fun onResume() {
        super.onResume()
        val b = _b ?: return
        val isDefault = (activity as? MainActivity)?.isDefaultDialer() == true
        b.tvDefaultStatus.text =
            if (isDefault) "FoneX is your default phone app." else "FoneX is not your default phone app yet."
        b.btnSetDefault.visibility = if (isDefault) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
