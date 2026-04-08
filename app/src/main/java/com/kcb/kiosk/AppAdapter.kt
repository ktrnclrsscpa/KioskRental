package com.kcb.kiosk

import android.content.pm.PackageManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(private val apps: List<AppInfo>, private val pm: PackageManager) : 
    RecyclerView.Adapter<AppAdapter.Holder>() {

    class Holder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout) {
        val icon = ImageView(layout.context).apply { 
            layoutParams = LinearLayout.LayoutParams(160, 160) // Size ng Icon
        }
        val name = TextView(layout.context).apply { 
            gravity = Gravity.CENTER; textSize = 12f; setPadding(0, 10, 0, 20)
        }
        init { 
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.addView(icon)
            layout.addView(name)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val layout = LinearLayout(parent.context).apply { layoutParams = lp }
        return Holder(layout)
    }

    override fun getItemCount() = apps.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val app = apps[position]
        holder.name.text = app.name
        try {
            holder.icon.setImageDrawable(pm.getApplicationIcon(app.packageName))
        } catch (e: Exception) {
            // Default icon kung may error
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }

        holder.layout.setOnClickListener {
            val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                holder.layout.context.startActivity(launchIntent)
            } else {
                Toast.makeText(holder.layout.context, "Cannot open app", Toast.LENGTH_SHORT).show()
            }
        }
    }
    }
