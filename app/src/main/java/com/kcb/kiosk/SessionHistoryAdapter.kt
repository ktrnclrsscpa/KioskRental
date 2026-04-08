package com.kcb.kiosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class SessionRecord(
    val pin: String,
    val minutes: Int,
    val amount: Double,
    val timestamp: String
)

class SessionHistoryAdapter(private val history: List<SessionRecord>) :
    RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val detailText: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int): ViewHolder {
        val v = LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(h: ViewHolder, pos: Int) {
        val item = history[pos]
        h.detailText.text = "PIN: ${item.pin} | ${item.minutes} mins | ₱${item.amount}"
    }

    override fun getItemCount() = history.size
}