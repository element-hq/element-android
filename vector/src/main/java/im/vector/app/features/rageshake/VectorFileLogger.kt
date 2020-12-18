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

package im.vector.app.features.rageshake

import android.content.Context
import android.util.Log
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

private const val SIZE_20MB = 20 * 1024 * 1024
private const val SIZE_50MB = 50 * 1024 * 1024

@Singleton
class VectorFileLogger @Inject constructor(val context: Context, private val vectorPreferences: VectorPreferences) : Timber.Tree() {

    private val maxLogSizeByte: Int
    private val logRotationCount: Int

    init {
        if (vectorPreferences.labAllowedExtendedLogging()) {
            maxLogSizeByte = SIZE_50MB
            logRotationCount = 15
        } else {
            maxLogSizeByte = SIZE_20MB
            logRotationCount = 7
        }
    }

    private val sLogger = Logger.getLogger("im.vector.app")
    private var sFileHandler: FileHandler? = null
    private var sCacheDirectory: File? = null
    private var sFileName = "elementLogs"

    private val prioPrefixes = mapOf(
            Log.VERBOSE to "V/ ",
            Log.DEBUG to "D/ ",
            Log.INFO to "I/ ",
            Log.WARN to "W/ ",
            Log.ERROR to "E/ ",
            Log.ASSERT to "WTF/ "
    )

    init {
        val logsDirectoryFile = context.cacheDir.absolutePath + "/logs"
        setLogDirectory(File(logsDirectoryFile))
        try {
            if (sCacheDirectory != null) {
                sFileHandler = FileHandler(sCacheDirectory!!.absolutePath + "/" + sFileName + ".%g.txt", maxLogSizeByte, logRotationCount)
                sFileHandler?.formatter = LogFormatter()
                sLogger.useParentHandlers = false
                sLogger.level = Level.ALL
                sFileHandler?.let { sLogger.addHandler(it) }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Failed to initialize FileLogger")
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        GlobalScope.launch(Dispatchers.IO) {
            if (sFileHandler == null) return@launch
            if (skipLog(priority)) return@launch
            if (t != null) {
                logToFile(t)
            }
            logToFile(prioPrefixes[priority] ?: "$priority ", tag ?: "Tag", message)
        }
    }

    private fun skipLog(priority: Int): Boolean {
        return if (vectorPreferences.labAllowedExtendedLogging()) {
            false
        } else {
            // Exclude verbose logs
            priority < Log.DEBUG
        }
    }

    /**
     * Set the directory to put log files.
     *
     * @param cacheDir The directory, usually [android.content.ContextWrapper.getCacheDir]
     */
    private fun setLogDirectory(cacheDir: File) {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        sCacheDirectory = cacheDir
    }

    /**
     * Adds our own log files to the provided list of files.
     *
     * @param files The list of files to add to.
     * @return The same list with more files added.
     */
    fun getLogFiles(): List<File> {
        val files = ArrayList<File>()

        try {
            // reported by GA
            if (null != sFileHandler) {
                sFileHandler!!.flush()
                val absPath = sCacheDirectory?.absolutePath ?: return emptyList()

                for (i in 0..logRotationCount) {
                    val filepath = "$absPath/$sFileName.$i.txt"
                    val file = File(filepath)
                    if (file.exists()) {
                        files.add(file)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "## addLogFiles() failed")
        }

        return files
    }

    class LogFormatter : Formatter() {

        override fun format(r: LogRecord): String {
            if (!mIsTimeZoneSet) {
                DATE_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
                mIsTimeZoneSet = true
            }

            val thrown = r.thrown
            if (thrown != null) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                sw.write(r.message)
                sw.write(LINE_SEPARATOR)
                thrown.printStackTrace(pw)
                pw.flush()
                return sw.toString()
            } else {
                val b = StringBuilder()
                val date = DATE_FORMAT.format(Date(r.millis))
                b.append(date)
                b.append("Z ")
                b.append(r.message)
                b.append(LINE_SEPARATOR)
                return b.toString()
            }
        }

        companion object {
            private val LINE_SEPARATOR = System.getProperty("line.separator") ?: "\n"

            //            private val DATE_FORMAT = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
            private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss*SSSZZZZ", Locale.US)

            private var mIsTimeZoneSet = false
        }
    }

    /**
     * Log an Throwable
     *
     * @param throwable the throwable to log
     */
    private fun logToFile(throwable: Throwable?) {
        if (null == sCacheDirectory || throwable == null) {
            return
        }

        val errors = StringWriter()
        throwable.printStackTrace(PrintWriter(errors))

        sLogger.info(errors.toString())
    }

    private fun logToFile(level: String, tag: String, content: String) {
        if (null == sCacheDirectory) {
            return
        }
        val b = StringBuilder()
        b.append(Thread.currentThread().id)
        b.append(" ")
        b.append(level)
        b.append("/")
        b.append(tag)
        b.append(": ")
        b.append(content)
        sLogger.info(b.toString())
    }
}
