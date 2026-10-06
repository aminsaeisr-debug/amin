package com.srooyesh.seedcounter

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExportPreviewAdapter(
    private val settings: AppSettings,
    private val rows: List<ExportRow>,
    private val maxRows: Int = 500
) : RecyclerView.Adapter<ExportPreviewAdapter.RowHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder = RowHolder(
        LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.WHITE)
        }
    )

    override fun onBindViewHolder(holder: RowHolder, position: Int) {
        holder.bind(ExportTableModel.values(rows[position], position + 1, settings))
    }

    override fun getItemCount(): Int = rows.size.coerceAtMost(maxRows)

    class RowHolder(private val row: LinearLayout) : RecyclerView.ViewHolder(row) {
        fun bind(values: List<String>) {
            row.removeAllViews()
            values.forEach { value ->
                row.addView(TextView(row.context).apply {
                    text = value
                    textSize = 13f
                    setTextColor(Color.rgb(32, 43, 45))
                    gravity = Gravity.CENTER
                    setPadding(20, 14, 20, 14)
                    layoutParams = LinearLayout.LayoutParams(170, ViewGroup.LayoutParams.WRAP_CONTENT)
                })
            }
        }
    }
}
