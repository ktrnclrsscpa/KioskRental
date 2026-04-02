package com.kcb.kiosk

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView

class SimpleAppAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SimpleAppAdapter.ViewHolder>() {

    class ViewHolder(val button: Button) : RecyclerView.ViewHolder(button)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val button = Button(parent.context)
        button.setPadding(20, 20, 20, 20)
        return ViewHolder(button)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.button.text = app.name
        holder.button.setOnClickListener {
            onClick(app.packageName)
        }
    }

    override fun getItemCount() = apps.size
}
