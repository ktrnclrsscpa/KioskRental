package com.kcb.kiosk

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kcb.kiosk.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var countDownTimer: CountDownTimer? = null
    private val supabase = SupabaseClient.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Button listener para sa Start Rental
        binding.btnStartRental.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.isNotEmpty()) {
                validateAndStart(pin)
            } else {
                Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateAndStart(pin: String) {
        // I-disable muna ang button para iwas double click
        binding.btnStartRental.isEnabled = false
        
        lifecycleScope.launch {
            try {
                // 1. I-verify ang PIN sa Supabase
                val result = supabase.validatePin(pin)

                if (result != null) {
                    // 2. Kung valid, i-mark as 'used' sa database
                    supabase.usePin(pin)
                    
                    // 3. Simulan ang timer sa phone
                    startTimer(result.seconds_left)
                    
                    // 4. Itago ang lock screen UI
                    binding.layoutLock.visibility = View.GONE
                    binding.layoutTimer.visibility = View.VISIBLE
                    
                    Toast.makeText(this@MainActivity, "Rental Started!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Invalid or Expired PIN", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnStartRental.isEnabled = true
            }
        }
    }

    private fun startTimer(seconds: Long) {
        countDownTimer?.cancel()
        
        countDownTimer = object : CountDownTimer(seconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                val displayTime = String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
                binding.tvTimerDisplay.text = displayTime
            }

            override fun onFinish() {
                // Balik sa Lock Screen kapag tapos na ang oras
                binding.layoutLock.visibility = View.VISIBLE
                binding.layoutTimer.visibility = View.GONE
                binding.etPin.text.clear()
                Toast.makeText(this@MainActivity, "Time Expired!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}