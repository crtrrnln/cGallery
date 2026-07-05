package com.example.cgallery.data

import android.content.Context
import android.content.Intent
import com.example.cgallery.MainActivity
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isShizukuAvailable()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    fun launchAppToInbox() {
        if (hasPermission()) {
            try {
                val command = "am start -n com.example.cgallery/com.example.cgallery.MainActivity --es TARGET_SCREEN INBOX"
                // Using reflection to access newProcess as it might be restricted in some library versions
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess", 
                    Array<String>::class.java, 
                    Array<String>::class.java, 
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", command), null, null)
                // We don't necessarily need to wait for it, we just want to fire and forget the launch
            } catch (e: Exception) {
                // Fallback to standard launch
                val intent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("TARGET_SCREEN", "INBOX")
                }
                context.startActivity(intent)
            }
        } else {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("TARGET_SCREEN", "INBOX")
            }
            context.startActivity(intent)
        }
    }
}
