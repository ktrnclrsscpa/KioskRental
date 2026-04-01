package com.kcb.kiosk

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: List<AppInfo>,
    private val packageManager: PackageManager
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(android.R.id.icon)
        val name: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.name
        
        try {
            val icon = packageManager.getApplicationIcon(app.packageName)
            holder.icon.setImageDrawable(icon)
        } catch (e: Exception) {
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        
        holder.itemView.setOnClickListener {
            try {
                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                if (intent != null) {
                    holder.itemView.context.startActivity(intent)
                } else {
                    Toast.makeText(holder.itemView.context, "App not installed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(holder.itemView.context, "Cannot open app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = apps.size
}
