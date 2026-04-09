package com.kcb.kiosk

import android.content.*
import android.os.*
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
        
        // I-load ang bagong layout na may RecyclerView
        setContentView(R.layout.activity_main) 

        // Initialize Supabase Client
        supabase = SupabaseClient.getInstance()
        
        // Setup the UI components
        setupUI()
        
        if (intent.getBooleanExtra("locked", false)) {
            applyLockUI()
        }
    }

    private fun setupUI() {
        // I-bind ang RecyclerView gamit ang ID mula sa XML
        appRecycler = findViewById(R.id.appRecycler)
        
        // I-set ang LayoutManager (4 columns para sa apps)
        appRecycler.layoutManager = GridLayoutManager(this, 4)
        
        // TODO: Dito mo ikakabit ang Adapter mo para lumitaw ang mga icons
    }

    override fun onResume() {
        super.onResume()
        loadWhitelistedApps()
    }

    fun loadWhitelistedApps() {
        val prefs = getSharedPreferences("KioskPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("allowed_packages", setOf())
        
        // I-update ang listahan ng apps base sa 'saved' set
        if (saved.isNullOrEmpty()) {
            Toast.makeText(this, "No apps allowed yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLockUI() {
        Toast.makeText(this, "Kiosk Mode Active", Toast.LENGTH_SHORT).show()
    }
}