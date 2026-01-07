package com.resqnav.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ResQNavApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("ResQNavApp", "Firebase init error", e)
        }

        // Set up global exception handler to log crashes properly
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("CrashHandler", "==== UNCAUGHT EXCEPTION ====")
                Log.e("CrashHandler", "Thread: ${thread.name}")
                Log.e("CrashHandler", "Error: ${throwable.message}", throwable)
                Log.e("CrashHandler", "Cause: ${throwable.cause}")

                // Log stack trace safely
                throwable.stackTrace.take(10).forEach {
                    Log.e("CrashHandler", "  at $it")
                }
            } catch (e: Exception) {
                // If logging fails, at least try to log that
                Log.e("CrashHandler", "Failed to log crash", e)
            } finally {
                // Always call the default handler to properly crash the app
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}

