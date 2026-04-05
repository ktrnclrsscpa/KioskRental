package com.kcb.kiosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SessionHistoryAdapter(private val sessions: List<SessionRecord>) : 
    RecyclerView.Adapter<SessionHistoryAdapter.SessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        
        // Check if this is an extension
        val isExtension = session.pin.endsWith("_EXT")
        val cleanPin = session.pin.replace("_EXT", "")
        val prefix = if (isExtension) "🔁 EXTENSION: " else "🎮 SESSION: "
        
        holder.text1.text = "$prefix$cleanPin | ${session.minutes} min | ₱${String.format("%.2f", session.amount)}"
        holder.text2.text = session.date
    }

    override fun getItemCount(): Int = sessions.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text1: TextView = itemView.findViewById(android.R.id.text1)
        val text2: TextView = itemView.findViewById(android.R.id.text2)
    }
    }
