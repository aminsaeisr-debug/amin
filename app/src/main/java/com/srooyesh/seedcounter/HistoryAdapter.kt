package com.srooyesh.seedcounter

import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(
    private var items: List<HistoryRow>,
    private val onDeleteClick: (HistoryRow) -> Unit // این خط برای دکمه حذف اضافه شد
) : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val context = parent.context
        val row = LinearLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 14, 16, 14)
        }

        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 15f
        }

        val deleteBtn = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(48, 48)
            setImageResource(android.R.drawable.ic_menu_delete) // آیکون سطل زباله
            background = null
            setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_light))
        }

        row.addView(textView)
        row.addView(deleteBtn)
        return Holder(row, textView, deleteBtn)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val first = mutableListOf("${position + 1}.", item.variety)
        if (item.customer.isNotBlank()) first.add("مشتری: ${item.customer}")
        if (item.number.isNotBlank()) first.add("شماره: ${item.number}")
        
        val second = listOf(item.dateJalali.takeIf(String::isNotBlank), item.time.takeIf(String::isNotBlank))
            .filterNotNull().joinToString(" | ")
            
        holder.text.text = if (second.isBlank()) first.joinToString("  |  ") else first.joinToString("  |  ") + "\n" + second
        holder.deleteBtn.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class Holder(val row: LinearLayout, val text: TextView, val deleteBtn: ImageButton) : RecyclerView.ViewHolder(row)
}