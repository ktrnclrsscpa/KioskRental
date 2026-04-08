package com.kcb.kiosk

import android.content.*
import android.os.*
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var supabase: SupabaseClient
    private lateinit var appRecycler: RecyclerView
    private var currentLocalSeconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabase = SupabaseClient.getInstance()
        setupUI()
        if (intent.getBooleanExtra("locked", false)) applyLockUI()
    }

    private fun setupUI() {
        // UI Code here...
        // appRecycler = RecyclerView(this)
    }

    override fun onResume() {
        super.onResume()
        loadWhitelistedApps()
    }

    fun loadWhitelistedApps() {
        val prefs = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed_packages", setOf())
        // Adapter logic here...
    }

    private fun applyLockUI() {
        // Lock logic...
    }
}