package com.kcb.kiosk

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var layoutLock: LinearLayout
    private lateinit var layoutUnlocked: LinearLayout
    private lateinit var tvTimer: TextView
    private lateinit var rvApps: RecyclerView
    private lateinit var etPin: EditText
    private var isUnlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        layoutLock = findViewById(R.id.layoutLock)
        layoutUnlocked = findViewById(R.id.layoutUnlocked)
        tvTimer = findViewById(R.id.tvTimer)
        rvApps = findViewById(R.id.rvApps)
        etPin = findViewById(R.id.etPin)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnAdmin = findViewById<TextView>(R.id.btnAdmin)

        // PIN Submission Logic
        btnStart.setOnClickListener {
            val enteredPin = etPin.text.toString().trim().uppercase()
            
            if (enteredPin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                btnStart.isEnabled = false
                btnStart.text = "Checking..."

                try {
                    // 1. Audit check sa Supabase
                    val rentalData = SupabaseClient.getInstance().validatePin(enteredPin)

                    if (rentalData != null) {
                        // 2. Mark as USED agad para hindi na ma-reuse (One-Time Use)
                        SupabaseClient.getInstance().usePin(enteredPin)

                        // 3. Unlock and Start Timer
                        unlockDevice(rentalData.seconds_left)
                        Toast.makeText(this@MainActivity, "Access Granted!", Toast.LENGTH_SHORT).show()
                        etPin.text.clear()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid or Expired PIN", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    btnStart.isEnabled = true
                    btnStart.text = "START RENTAL"
                }
            }
        }

        btnAdmin.setOnClickListener {
            showAdminLoginDialog()
        }

        setupAppGrid()
    }

    private fun unlockDevice(seconds: Long) {
        isUnlocked = true
        layoutLock.visibility = View.GONE
        layoutUnlocked.visibility = View.VISIBLE
        startTimer(seconds)
    }

    private fun startTimer(seconds: Long) {
        var timeLeft = seconds
        lifecycleScope.launch {
            while (timeLeft > 0 && isUnlocked) {
                val mins = timeLeft / 60
                val secs = timeLeft % 60
                tvTimer.text = String.format("%02d:%02d", mins, secs)
                delay(1000)
                timeLeft--
            }
            // Pag tapos na ang oras o ni-lock, balik sa lock screen
            lockDevice()
        }
    }

    private fun lockDevice() {
        isUnlocked = false
        layoutLock.visibility = View.VISIBLE
        layoutUnlocked.visibility = View.GONE
    }

    private fun setupAppGrid() {
        val sharedPrefs = getSharedPreferences("KCB_SETTINGS", Context.MODE_PRIVATE)
        val allowedPackages = sharedPrefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pkgAppsList = packageManager.queryIntentActivities(mainIntent, 0)

        val filteredApps = pkgAppsList.filter { 
            allowedPackages.isEmpty() || allowedPackages.contains(it.activityInfo.packageName)
        }.map { 
            AppInfo(it.loadLabel(packageManager).toString(), it.activityInfo.packageName, it.loadIcon(packageManager)) 
        }

        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = AppAdapter(filteredApps) { app ->
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            launchIntent?.let { startActivity(it) }
        }
    }

    private fun showAdminLoginDialog() {
        val adminEditText = EditText(this)
        adminEditText.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        
        AlertDialog.Builder(this)
            .setTitle("Admin Access")
            .setMessage("Enter Admin PIN")
            .setView(adminEditText)
            .setPositiveButton("Login") { _, _ ->
                if (adminEditText.text.toString() == "2026") {
                    startActivity(Intent(this, AdminActivity::class.java))
                } else {
                    Toast.makeText(this, "Wrong PIN!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- KIOSK SECURITY ---
    override fun onResume() { super.onResume(); setupAppGrid() }
    
    override fun onPause() {
        super.onPause()
        if (!isUnlocked) {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.moveTaskToFront(taskId, 0)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && !isUnlocked) {
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }
    }

    override fun onBackPressed() { /* Disabled */ }
}