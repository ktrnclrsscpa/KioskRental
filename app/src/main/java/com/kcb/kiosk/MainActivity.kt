private fun startExtendListener() {
        extendCheckJob?.cancel()
        extendCheckJob = CoroutineScope(Dispatchers.IO).launch {
            // Kunin muna natin ang initial value mula sa DB pagka-start ng listener
            var lastKnownDbSeconds = 0
            try {
                val initialResult = supabase.validatePin(currentPin!!)
                if (initialResult.isValid) {
                    lastKnownDbSeconds = initialResult.secondsLeft
                }
            } catch (e: Exception) {
                lastKnownDbSeconds = remainingSeconds
            }

            while (isActive && currentPin != null) {
                delay(3000) // 3 seconds interval para tipid sa API calls
                try {
                    val result = supabase.validatePin(currentPin!!)
                    if (result.isValid && result.secondsLeft > 0) {
                        val currentDbSeconds = result.secondsLeft

                        // LOGIC: Kung ang oras sa DB ay mas mataas kaysa sa huling record natin,
                        // ibig sabihin may manual extension na nangyari (e.g. +1 min o +5 mins).
                        if (currentDbSeconds > lastKnownDbSeconds) {
                            val addedSeconds = currentDbSeconds - lastKnownDbSeconds
                            
                            withContext(Dispatchers.Main) {
                                countDownTimer?.cancel()
                                
                                // UPDATE: Idagdag ang extra seconds sa KASALUKUYANG oras sa phone app.
                                // Hindi natin i-o-overwrite ang 'remainingSeconds' gamit ang DB value
                                // para hindi mawala yung 'current progress' ng user (e.g. yung 1:20).
                                remainingSeconds += addedSeconds 
                                
                                startCountDownTimer()
                                
                                val addedMinutes = addedSeconds / 60
                                if (addedMinutes > 0) {
                                    Toast.makeText(this@MainActivity, "✓ Extended! +$addedMinutes mins", Toast.LENGTH_LONG).show()
                                    speakAlert("Time extended by $addedMinutes minutes")
                                } else {
                                    Toast.makeText(this@MainActivity, "✓ Seconds added!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        
                        // Palaging i-update ang reference point para sa susunod na ikot ng loop
                        lastKnownDbSeconds = currentDbSeconds

                    } else if (result.secondsLeft <= 0) {
                        // Kung zero na talaga sa DB, tapusin na ang session
                        withContext(Dispatchers.Main) { endSession() }
                        break
                    }
                } catch (e: Exception) {
                    // Ignore connection errors during background check
                }
            }
        }
}
