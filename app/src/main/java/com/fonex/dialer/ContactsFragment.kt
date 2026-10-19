package com.fonex.dialer

import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fonex.dialer.databinding.FragmentContactsBinding

class ContactsFragment : Fragment() {

    private var _b: FragmentContactsBinding? = null
    private val adapter = RowAdapter()
    private var all: List<Contact> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentContactsBinding.inflate(inflater, container, false)
        _b = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val b = _b ?: return
        b.list.layoutManager = LinearLayoutManager(requireContext())
        b.list.adapter = adapter
        b.search.doAfterTextChanged { show() }
        b.fabAdd.setOnClickListener {
            startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI))
        }
        b.empty.setOnClickListener {
            if (!ContactsRepo.hasPerm(requireContext())) (activity as? MainActivity)?.requestPerms()
        }
    }

    override fun onResume() {
        super.onResume()
        val app = requireContext().applicationContext
        Bg.run({ ContactsRepo.loadAll(app) }) { list ->
            all = list
            show()
        }
    }

    private fun show() {
        val b = _b ?: return
        val ctx = context ?: return
        val q = b.search.text?.toString()?.trim().orEmpty()
        val digits = q.filter { it.isDigit() }
        val filtered = if (q.isEmpty()) all else all.filter {
            it.name.contains(q, ignoreCase = true) ||
                (digits.isNotEmpty() && it.number.filter(Char::isDigit).contains(digits))
        }
        adapter.submit(filtered.map { c ->
            Row(
                title = c.name.ifBlank { c.number },
                subtitle = c.number,
                avatar = c.name.firstOrNull()?.uppercase() ?: "#",
                color = AVATAR_COLORS[(c.name.hashCode() and 0x7fffffff) % AVATAR_COLORS.size],
                trailing = "",
                onClick = { CallHelper.dial(requireActivity(), c.number) },
                onLong = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, c.id)
                        )
                    )
                }
            )
        })
        b.empty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        b.empty.text = when {
            !ContactsRepo.hasPerm(ctx) -> "Contacts permission needed.\nTap here to allow it."
            q.isEmpty() -> "No contacts yet"
            else -> "No matches"
        }
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}
