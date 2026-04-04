package com.kcb.kiosk

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionHistoryAdapter(
    private val sessions: List<SessionRecord>
) : RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: TextView) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = TextView(parent.context)
        textView.setPadding(20, 15, 20, 15)
        textView.textSize = 12f
        return ViewHolder(textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        holder.textView.text = "🔑 ${session.pin} | ${session.minutes} min | ₱${String.format("%.2f", session.amount)} | ${session.date}"
    }

    override fun getItemCount() = sessions.size
}
