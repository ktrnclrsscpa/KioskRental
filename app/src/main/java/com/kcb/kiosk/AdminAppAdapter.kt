package com.kcb.kiosk

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminAppAdapter(
    private val appList: List<ResolveInfo>,
    private val selectedApps: MutableSet<String>,
    private val pm: PackageManager
) : RecyclerView.Adapter<AdminAppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imgAppIcon)
        val name: TextView = view.findViewById(R.id.txtAppName)
        val checkBox: CheckBox = view.findViewById(R.id.cbApp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        val pkgName = app.activityInfo.packageName
        
        holder.name.text = app.loadLabel(pm).toString()
        holder.icon.setImageDrawable(app.loadIcon(pm))

        // Mahalaga ito para hindi magloko ang checkbox sa scrolling
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedApps.contains(pkgName)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(pkgName)
            } else {
                selectedApps.remove(pkgName)
            }
        }
    }

    override fun getItemCount(): Int = appList.size

    fun getSelectedApps(): Set<String> = selectedApps
}