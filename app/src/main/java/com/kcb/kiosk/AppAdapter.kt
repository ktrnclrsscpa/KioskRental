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
            layoutParams = LinearLayout.LayoutParams(130, 130).apply { setMargins(0, 0, 0, 10) }
        }
        val name = TextView(layout.context).apply {
            gravity = Gravity.CENTER
            textSize = 12f
            maxLines = 1
        }
        init {
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.setPadding(20, 30, 20, 30)
            layout.addView(icon); layout.addView(name)
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = Holder(LinearLayout(p.context))
    override fun getItemCount() = apps.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val app = apps[pos]
        h.name.text = app.name
        try {
            h.icon.setImageDrawable(pm.getApplicationIcon(app.packageName))
        } catch (e: Exception) {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        h.layout.setOnClickListener {
            val intent = pm.getLaunchIntentForPackage(app.packageName)
            if (intent != null) h.layout.context.startActivity(intent)
            else Toast.makeText(h.layout.context, "Cannot open app", Toast.LENGTH_SHORT).show()
        }
    }
    }
