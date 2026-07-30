package com.example.cgallery.data

import android.content.Context
import android.content.Intent
import android.content.ComponentName
import java.util.concurrent.TimeUnit
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

    fun launchAppToInbox(): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("TARGET_SCREEN", "INBOX")
        }

        if (hasPermission()) {
            try {
                val pkg = context.packageName
                val alias = listOf("MainActivityRed", "MainActivityBlue", "MainActivity").firstOrNull { name ->
                    try {
                        val cn = ComponentName(pkg, "$pkg.$name")
                        context.packageManager.getActivityInfo(cn, 0).enabled
                    } catch (e: Exception) { false }
                } ?: "MainActivity"
                val wakeCmd = "input keyevent KEYCODE_WAKEUP; wm dismiss-keyguard"
                val homeCmd = "input keyevent KEYCODE_HOME; sleep 0.1"
                val flags = "0x34008000" // NEW_TASK | SINGLE_TOP | CLEAR_TOP | CLEAR_TASK
                val pkgMainActivity = "$pkg/$pkg.MainActivity"
                val directCmd = "am start --user 0 -W -n $pkgMainActivity -f $flags --es TARGET_SCREEN INBOX"
                val startCmd = "cmd activity start-activity --user 0 -W -n $pkgMainActivity -f $flags --es TARGET_SCREEN INBOX"
                val aliasCmd = "am start --user 0 -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n $pkg/$pkg.$alias -f $flags --es TARGET_SCREEN INBOX"
                val pokeCmd = "monkey -p $pkg -c android.intent.category.LAUNCHER 1; sleep 0.25; am start --user 0 -W -n $pkgMainActivity -f $flags --es TARGET_SCREEN INBOX"
                val shCmd = "$wakeCmd; $homeCmd; $startCmd || $directCmd || $aliasCmd || $pokeCmd"
                
                android.util.Log.d("ShizukuLaunch", "Executing: $shCmd")
                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", shCmd), null, null) as Process
                val finished = process.waitFor(5, TimeUnit.SECONDS)
                val code = if (finished) process.exitValue() else -1
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error = process.errorStream.bufferedReader().use { it.readText() }
                android.util.Log.d("ShizukuLaunch", "Finished: code=$code, out=$output, err=$error")
                process.outputStream.close(); process.inputStream.close(); process.errorStream.close()
                return finished && code == 0
            } catch (e: Exception) {
                e.printStackTrace()
                context.startActivity(intent)
                return true
            }
        } else {
            context.startActivity(intent)
            return true
        }
    }
}

