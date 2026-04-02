package com.kcb.kiosk

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleAppAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleAppAdapter.ViewHolder>() {

    class ViewHolder(itemView: TextView) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context)
        textView.setPadding(50, 30, 50, 30)
        textView.textSize = 16f
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.textView.text = app.name
        holder.itemView.setOnClickListener {
            onClick(app.packageName)
        }
    }

    override fun getItemCount() = apps.size
}
