package com.kcb.kiosk

import android.content.pm.PackageManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(private val apps: List<AppInfo>, private val pm: PackageManager) : 
    RecyclerView.Adapter<AppAdapter.Holder>() {

    class Holder(val l: LinearLayout) : RecyclerView.ViewHolder(l) {
        val i = ImageView(l.context).apply { layoutParams = LinearLayout.LayoutParams(120, 120) }
        val n = TextView(l.context).apply { gravity = Gravity.CENTER; textSize = 11f }
        init { l.orientation = LinearLayout.VERTICAL; l.gravity = Gravity.CENTER; l.addView(i); l.addView(n) }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = Holder(LinearLayout(p.context))
    override fun getItemCount() = apps.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val a = apps[pos]
        h.n.text = a.name
        h.i.setImageDrawable(pm.getApplicationIcon(a.packageName))
        h.l.setOnClickListener {
            val it = pm.getLaunchIntentForPackage(a.packageName)
            if (it != null) h.l.context.startActivity(it)
        }
    }
}