package com.kcb.kiosk

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 50, 30, 30)
        }
        
        val title = TextView(this).apply {
            text = "📱 INSTALLED APPS"
            textSize = 22f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)
        
        // Get all installed apps
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appList = mutableListOf<String>()
        
        for (app in packages) {
            if (packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                val appName = packageManager.getApplicationLabel(app).toString()
                appList.add("📱 $appName\n   ${app.packageName}")
            }
        }
        
        appList.sort()
        
        val textView = TextView(this).apply {
            text = appList.joinToString("\n\n")
            textSize = 12f
        }
        layout.addView(textView)
        
        setContentView(layout)
    }
}
