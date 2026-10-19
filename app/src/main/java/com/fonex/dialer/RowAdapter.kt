package com.fonex.dialer

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fonex.dialer.databinding.ItemRowBinding

data class Row(
    val title: String,
    val subtitle: String,
    val avatar: String,
    val color: Int,
    val trailing: String,
    val onClick: () -> Unit,
    val onLong: (() -> Unit)? = null
)

class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {
    private var rows: List<Row> = emptyList()

    fun submit(list: List<Row>) {
        rows = list
        notifyDataSetChanged()
    }

    class VH(val b: ItemRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = rows[position]
        holder.b.tvTitle.text = r.title
        holder.b.tvSub.text = r.subtitle
        holder.b.avatar.text = r.avatar
        holder.b.avatar.backgroundTintList = ColorStateList.valueOf(r.color)
        holder.b.tvTrailing.text = r.trailing
        holder.b.root.setOnClickListener { r.onClick() }
        holder.b.root.setOnLongClickListener {
            val f = r.onLong
            if (f != null) {
                f()
                true
            } else {
                false
            }
        }
    }
}
