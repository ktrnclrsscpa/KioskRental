package com.kcb.kiosk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val apps: List<AppInfo>,
    private val selectedPackages: MutableSet<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(android.R.id.text1)
        val checkbox: CheckBox = itemView.findViewById(android.R.id.checkbox)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.name
        holder.checkbox.isChecked = selectedPackages.contains(app.packageName)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedPackages.add(app.packageName)
            } else {
                selectedPackages.remove(app.packageName)
            }
            onSelectionChanged(selectedPackages)
        }
        holder.itemView.setOnClickListener {
            holder.checkbox.isChecked = !holder.checkbox.isChecked
        }
    }
    
    override fun getItemCount() = apps.size
    
    fun updateSelection(selected: List<String>) {
        selectedPackages.clear()
        selectedPackages.addAll(selected)
        notifyDataSetChanged()
    }
}
