package com.example

import android.app.Application
import android.util.Log
import com.example.core.util.AttendanceReminderHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class LaborApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        try {
            AttendanceReminderHelper.scheduleDailyReminders(this)
        } catch (e: Exception) {
            Log.e("LaborApplication", "Failed to schedule reminders: ${e.message}", e)
        }
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                try {
                    FirebaseApp.initializeApp(this)
                } catch (e: Exception) {
                    Log.w("LaborApplication", "Default initializeApp failed, using programmatic FirebaseOptions: ${e.message}")
                }
            }

            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1027179208222:android:ac3483799fc5ed6c6a580f")
                    .setApiKey("AIzaSyAMeOVp4gfkmBrOv_uMfOUuokXHQLFwFZY")
                    .setProjectId("laborbook-4c47e")
                    .setGcmSenderId("1027179208222")
                    .setStorageBucket("laborbook-4c47e.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.i("LaborApplication", "FirebaseApp successfully initialized with FirebaseOptions.")
            } else {
                Log.i("LaborApplication", "FirebaseApp already initialized.")
            }
        } catch (e: Exception) {
            Log.e("LaborApplication", "Failed to initialize FirebaseApp: ${e.message}", e)
        }
    }
}
