package com.rameshai.debug

import android.content.Context
import android.util.Log
import java.io.File

object CrashLogger {

    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = File(context.filesDir, "ramesh_crash.log")

                val report = buildString {
                    appendLine("===== RAMESH AI CRASH =====")
                    appendLine("Time: ${System.currentTimeMillis()}")
                    appendLine("Thread: ${thread.name}")
                    appendLine("Package: ${context.packageName}")
                    appendLine()
                    appendLine("Exception:")
                    appendLine(Log.getStackTraceString(throwable))
                }

                crashFile.writeText(report)
            } catch (_: Exception) {
                // Never let the crash logger cause another crash.
            }

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
