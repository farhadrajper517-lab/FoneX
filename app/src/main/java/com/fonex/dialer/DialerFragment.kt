package com.fonex.dialer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fonex.dialer.databinding.FragmentDialerBinding

class DialerFragment : Fragment() {

    private var _b: FragmentDialerBinding? = null
    private val number = StringBuilder()
    private var tone: ToneGenerator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentDialerBinding.inflate(inflater, container, false)
        _b = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = _b ?: return
        tone = try {
            ToneGenerator(AudioManager.STREAM_DTMF, 70)
        } catch (e: RuntimeException) {
            null
        }
        buildKeys(b)

        (activity as? MainActivity)?.takePendingNumber()?.let { number.append(it) }
        refresh()

        b.btnCall.setOnClickListener {
            if (number.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a number", Toast.LENGTH_SHORT).show()
            } else {
                CallHelper.dial(requireActivity(), number.toString())
            }
        }
        b.btnBackspace.setOnClickListener {
            if (number.isNotEmpty()) {
                number.deleteCharAt(number.length - 1)
                refresh()
            }
        }
        b.btnBackspace.setOnLongClickListener {
            number.clear()
            refresh()
            true
        }
    }

    private fun buildKeys(b: FragmentDialerBinding) {
        val ctx: Context = requireContext()
        val rows = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )
        val textColor = ctx.onSurfaceColor()
        val ripple = ctx.rippleRes()
        for (row in rows) {
            val line = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }
            for ((digit, letters) in row) {
                val key = TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    gravity = Gravity.CENTER
                    textSize = 28f
                    setTextColor(textColor)
                    setBackgroundResource(ripple)
                    val s = SpannableString(if (letters.isEmpty()) digit else "$digit\n$letters")
                    if (letters.isNotEmpty()) s.setSpan(RelativeSizeSpan(0.4f), 2, s.length, 0)
                    text = s
                    setOnClickListener { press(digit[0]) }
                    if (digit == "0") {
                        setOnLongClickListener {
                            press('+')
                            true
                        }
                    }
                }
                line.addView(key)
            }
            b.keys.addView(line)
        }
    }

    private fun press(c: Char) {
        number.append(c)
        val code = when (c) {
            in '0'..'9' -> c - '0'
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> -1
        }
        if (code >= 0) tone?.startTone(code, 80)
        refresh()
    }

    private fun refresh() {
        val b = _b ?: return
        b.tvNumber.text = number.toString()
        b.btnBackspace.visibility = if (number.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    override fun onDestroyView() {
        tone?.release()
        tone = null
        _b = null
        super.onDestroyView()
    }
}
