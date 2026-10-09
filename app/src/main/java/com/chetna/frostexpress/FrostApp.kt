package com.chetna.frostexpress

import android.app.Application
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Saves the stack trace of a crash to a file so the next launch can show it instead of failing silently. */
class FrostApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // decode the game sprites in the background so the first frame does not wait for them
        Thread { try { SpriteBank.get(this) } catch (_: Throwable) {} }.start()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                val sw = StringWriter()
                error.printStackTrace(PrintWriter(sw))
                File(filesDir, CRASH_FILE).writeText(
                    "Thread: ${thread.name}\n" +
                        "Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n\n" +
                        sw.toString()
                )
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
