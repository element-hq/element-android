/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.rageshake

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import im.vector.riotredesign.BuildConfig
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter

@SuppressLint("StaticFieldLeak")
object VectorUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    // key to save the crash status
    private const val PREFS_CRASH_KEY = "PREFS_CRASH_KEY"

    private var vectorVersion: String = ""
    private var matrixSdkVersion: String = ""

    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    private lateinit var context: Context

    /**
     * Activate this handler
     */
    fun activate(context: Context) {
        this.context = context

        previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    /**
     * An uncaught exception has been triggered
     *
     * @param thread    the thread
     * @param throwable the throwable
     * @return the exception description
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (context == null) {
            previousHandler?.uncaughtException(thread, throwable)
            return
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putBoolean(PREFS_CRASH_KEY, true)
        }

        val b = StringBuilder()
        val appName = "RiotX" // TODO Matrix.getApplicationName()

        b.append(appName + " Build : " + BuildConfig.VERSION_CODE + "\n")
        b.append("$appName Version : $vectorVersion\n")
        b.append("SDK Version : $matrixSdkVersion\n")
        b.append("Phone : " + Build.MODEL.trim() + " (" + Build.VERSION.INCREMENTAL + " " + Build.VERSION.RELEASE + " " + Build.VERSION.CODENAME + ")\n")

        b.append("Memory statuses \n")

        var freeSize = 0L
        var totalSize = 0L
        var usedSize = -1L
        try {
            val info = Runtime.getRuntime()
            freeSize = info.freeMemory()
            totalSize = info.totalMemory()
            usedSize = totalSize - freeSize
        } catch (e: Exception) {
            e.printStackTrace()
        }

        b.append("usedSize   " + usedSize / 1048576L + " MB\n")
        b.append("freeSize   " + freeSize / 1048576L + " MB\n")
        b.append("totalSize   " + totalSize / 1048576L + " MB\n")

        b.append("Thread: ")
        b.append(thread.name)

        /*
        val a = VectorApp.getCurrentActivity()
        if (a != null) {
            b.append(", Activity:")
            b.append(a.localClassName)
        }
        */

        b.append(", Exception: ")

        val sw = StringWriter()
        val pw = PrintWriter(sw, true)
        throwable.printStackTrace(pw)
        b.append(sw.buffer.toString())
        Timber.e("FATAL EXCEPTION " + b.toString())

        val bugDescription = b.toString()

        BugReporter.saveCrashReport(context, bugDescription)

        // Show the classical system popup
        previousHandler?.uncaughtException(thread, throwable)
    }

    // TODO Call me
    fun setVersions(vectorVersion: String, matrixSdkVersion: String) {
        this.vectorVersion = vectorVersion
        this.matrixSdkVersion = matrixSdkVersion
    }

    /**
     * Tells if the application crashed
     *
     * @return true if the application crashed
     */
    fun didAppCrash(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREFS_CRASH_KEY, false)
    }

    /**
     * Clear the crash status
     */
    fun clearAppCrashStatus(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(PREFS_CRASH_KEY)
        }
    }
}