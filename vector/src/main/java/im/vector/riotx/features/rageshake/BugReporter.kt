/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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

package im.vector.riotx.features.rageshake

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Build
import android.text.TextUtils
import android.view.View
import im.vector.matrix.android.api.Matrix
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.toOnOff
import im.vector.riotx.core.utils.getDeviceLocale
import im.vector.riotx.features.settings.VectorLocale
import im.vector.riotx.features.themes.ThemeUtils
import im.vector.riotx.features.version.VersionProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.*
import java.net.HttpURLConnection
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BugReporter creates and sends the bug reports.
 */
@Singleton
class BugReporter @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                      private val versionProvider: VersionProvider,
                                      private val vectorFileLogger: VectorFileLogger) {
    var inMultiWindowMode = false

    companion object {
        // filenames
        private const val LOG_CAT_ERROR_FILENAME = "logcatError.log"
        private const val LOG_CAT_FILENAME = "logcat.log"
        private const val LOG_CAT_SCREENSHOT_FILENAME = "screenshot.png"
        private const val CRASH_FILENAME = "crash.log"

        private const val BUFFER_SIZE = 1024 * 1024 * 50

    }

    // the http client
    private val mOkHttpClient = OkHttpClient()

    // the pending bug report call
    private var mBugReportCall: Call? = null


    // boolean to cancel the bug report
    private val mIsCancelled = false

    /**
     * Get current Screenshot
     *
     * @return screenshot or null if not available
     */
    var screenshot: Bitmap? = null
        private set

    private val LOGCAT_CMD_ERROR = arrayOf("logcat", ///< Run 'logcat' command
            "-d", ///< Dump the log rather than continue outputting it
            "-v", // formatting
            "threadtime", // include timestamps
            "AndroidRuntime:E " + ///< Pick all AndroidRuntime errors (such as uncaught exceptions)"communicatorjni:V " + ///< All communicatorjni logging
                    "libcommunicator:V " + ///< All libcommunicator logging
                    "DEBUG:V " + ///< All DEBUG logging - which includes native land crashes (seg faults, etc)
                    "*:S" ///< Everything else silent, so don't pick it..
    )

    private val LOGCAT_CMD_DEBUG = arrayOf("logcat", "-d", "-v", "threadtime", "*:*")

    /**
     * Bug report upload listener
     */
    interface IMXBugReportListener {
        /**
         * The bug report has been cancelled
         */
        fun onUploadCancelled()

        /**
         * The bug report upload failed.
         *
         * @param reason the failure reason
         */
        fun onUploadFailed(reason: String?)

        /**
         * The upload progress (in percent)
         *
         * @param progress the upload progress
         */
        fun onProgress(progress: Int)

        /**
         * The bug report upload succeeded.
         */
        fun onUploadSucceed()
    }

    /**
     * Send a bug report.
     *
     * @param context           the application context
     * @param forSuggestion     true to send a suggestion
     * @param withDevicesLogs   true to include the device log
     * @param withCrashLogs     true to include the crash logs
     * @param withScreenshot    true to include the screenshot
     * @param theBugDescription the bug description
     * @param listener          the listener
     */
    @SuppressLint("StaticFieldLeak")
    fun sendBugReport(context: Context,
                      forSuggestion: Boolean,
                      withDevicesLogs: Boolean,
                      withCrashLogs: Boolean,
                      withScreenshot: Boolean,
                      theBugDescription: String,
                      listener: IMXBugReportListener?) {
        object : AsyncTask<Void, Int, String>() {

            // enumerate files to delete
            val mBugReportFiles: MutableList<File> = ArrayList()

            override fun doInBackground(vararg voids: Void?): String? {
                var bugDescription = theBugDescription
                var serverError: String? = null
                val crashCallStack = getCrashDescription(context)

                if (null != crashCallStack) {
                    bugDescription += "\n\n\n\n--------------------------------- crash call stack ---------------------------------\n"
                    bugDescription += crashCallStack
                }

                val gzippedFiles = ArrayList<File>()

                if (withDevicesLogs) {
                    val files = vectorFileLogger.getLogFiles()

                    for (f in files) {
                        if (!mIsCancelled) {
                            val gzippedFile = compressFile(f)

                            if (null != gzippedFile) {
                                gzippedFiles.add(gzippedFile)
                            }
                        }
                    }
                }

                if (!mIsCancelled && (withCrashLogs || withDevicesLogs)) {
                    val gzippedLogcat = saveLogCat(context, false)

                    if (null != gzippedLogcat) {
                        if (gzippedFiles.size == 0) {
                            gzippedFiles.add(gzippedLogcat)
                        } else {
                            gzippedFiles.add(0, gzippedLogcat)
                        }
                    }

                    val crashDescription = getCrashFile(context)
                    if (crashDescription.exists()) {
                        val compressedCrashDescription = compressFile(crashDescription)

                        if (null != compressedCrashDescription) {
                            if (gzippedFiles.size == 0) {
                                gzippedFiles.add(compressedCrashDescription)
                            } else {
                                gzippedFiles.add(0, compressedCrashDescription)
                            }
                        }
                    }
                }

                var deviceId = "undefined"
                var userId = "undefined"
                var olmVersion = "undefined"

                activeSessionHolder.getSafeActiveSession()?.let { session ->
                    userId = session.myUserId
                    deviceId = session.sessionParams.credentials.deviceId ?: "undefined"
                    olmVersion = session.getCryptoVersion(context, true)
                }

                if (!mIsCancelled) {
                    val text = "[RiotX] " +
                            if (forSuggestion) {
                                "[Suggestion] "
                            } else {
                                ""
                            } +
                            bugDescription

                    // build the multi part request
                    val builder = BugReporterMultipartBody.Builder()
                            .addFormDataPart("text", text)
                            .addFormDataPart("app", "riot-android")
                            .addFormDataPart("user_agent", Matrix.getInstance(context).getUserAgent())
                            .addFormDataPart("user_id", userId)
                            .addFormDataPart("device_id", deviceId)
                            .addFormDataPart("version", versionProvider.getVersion(longFormat = true, useBuildNumber = false))
                            .addFormDataPart("branch_name", context.getString(R.string.git_branch_name))
                            .addFormDataPart("matrix_sdk_version", Matrix.getSdkVersion())
                            .addFormDataPart("olm_version", olmVersion)
                            .addFormDataPart("device", Build.MODEL.trim())
                            .addFormDataPart("lazy_loading", true.toOnOff())
                            .addFormDataPart("multi_window", inMultiWindowMode.toOnOff())
                            .addFormDataPart("os", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") "
                                    + Build.VERSION.INCREMENTAL + "-" + Build.VERSION.CODENAME)
                            .addFormDataPart("locale", Locale.getDefault().toString())
                            .addFormDataPart("app_language", VectorLocale.applicationLocale.toString())
                            .addFormDataPart("default_app_language", getDeviceLocale(context).toString())
                            .addFormDataPart("theme", ThemeUtils.getApplicationTheme(context))

                    val buildNumber = context.getString(R.string.build_number)
                    if (!TextUtils.isEmpty(buildNumber) && buildNumber != "0") {
                        builder.addFormDataPart("build_number", buildNumber)
                    }

                    // add the gzipped files
                    for (file in gzippedFiles) {
                        builder.addFormDataPart("compressed-log", file.name, file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                    }

                    mBugReportFiles.addAll(gzippedFiles)

                    if (withScreenshot) {
                        val bitmap = screenshot

                        if (null != bitmap) {
                            val logCatScreenshotFile = File(context.cacheDir.absolutePath, LOG_CAT_SCREENSHOT_FILENAME)

                            if (logCatScreenshotFile.exists()) {
                                logCatScreenshotFile.delete()
                            }

                            try {
                                val fos = FileOutputStream(logCatScreenshotFile)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                                fos.flush()
                                fos.close()

                                builder.addFormDataPart("file",
                                        logCatScreenshotFile.name, logCatScreenshotFile.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
                            } catch (e: Exception) {
                                Timber.e(e, "## sendBugReport() : fail to write screenshot$e")
                            }

                        }
                    }

                    screenshot = null

                    // add some github labels
                    builder.addFormDataPart("label", BuildConfig.VERSION_NAME)
                    builder.addFormDataPart("label", BuildConfig.FLAVOR_DESCRIPTION)
                    builder.addFormDataPart("label", context.getString(R.string.git_branch_name))

                    // Special for RiotX
                    builder.addFormDataPart("label", "[RiotX]")

                    // Suggestion
                    if (forSuggestion) {
                        builder.addFormDataPart("label", "[Suggestion]")
                    }

                    if (getCrashFile(context).exists()) {
                        builder.addFormDataPart("label", "crash")
                        deleteCrashFile(context)
                    }

                    val requestBody = builder.build()

                    // add a progress listener
                    requestBody.setWriteListener { totalWritten, contentLength ->
                        val percentage: Int

                        if (-1L != contentLength) {
                            if (totalWritten > contentLength) {
                                percentage = 100
                            } else {
                                percentage = (totalWritten * 100 / contentLength).toInt()
                            }
                        } else {
                            percentage = 0
                        }

                        if (mIsCancelled && null != mBugReportCall) {
                            mBugReportCall!!.cancel()
                        }

                        Timber.v("## onWrite() : $percentage%")
                        publishProgress(percentage)
                    }

                    // build the request
                    val request = Request.Builder()
                            .url(context.getString(R.string.bug_report_url))
                            .post(requestBody)
                            .build()

                    var responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR
                    var response: Response? = null
                    var errorMessage: String? = null

                    // trigger the request
                    try {
                        mBugReportCall = mOkHttpClient.newCall(request)
                        response = mBugReportCall!!.execute()
                        responseCode = response.code
                    } catch (e: Exception) {
                        Timber.e(e, "response")
                        errorMessage = e.localizedMessage
                    }

                    // if the upload failed, try to retrieve the reason
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        if (null != errorMessage) {
                            serverError = "Failed with error $errorMessage"
                        } else if (null == response || null == response.body) {
                            serverError = "Failed with error $responseCode"
                        } else {
                            var inputStream: InputStream? = null

                            try {
                                inputStream = response.body!!.byteStream()

                                var ch = inputStream.read()
                                val b = StringBuilder()
                                while (ch != -1) {
                                    b.append(ch.toChar())
                                    ch = inputStream.read()
                                }
                                serverError = b.toString()
                                inputStream.close()

                                // check if the error message
                                try {
                                    val responseJSON = JSONObject(serverError)
                                    serverError = responseJSON.getString("error")
                                } catch (e: JSONException) {
                                    Timber.e(e, "doInBackground ; Json conversion failed")
                                }

                                // should never happen
                                if (null == serverError) {
                                    serverError = "Failed with error $responseCode"
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "## sendBugReport() : failed to parse error")
                            } finally {
                                try {
                                    inputStream?.close()
                                } catch (e: Exception) {
                                    Timber.e(e, "## sendBugReport() : failed to close the error stream")
                                }

                            }
                        }
                    }
                }

                return serverError
            }


            override fun onProgressUpdate(vararg progress: Int?) {
                if (null != listener) {
                    try {
                        listener.onProgress(progress[0] ?: 0)
                    } catch (e: Exception) {
                        Timber.e(e, "## onProgress() : failed")
                    }

                }
            }

            override fun onPostExecute(reason: String?) {
                mBugReportCall = null

                // delete when the bug report has been successfully sent
                for (file in mBugReportFiles) {
                    file.delete()
                }

                if (null != listener) {
                    try {
                        if (mIsCancelled) {
                            listener.onUploadCancelled()
                        } else if (null == reason) {
                            listener.onUploadSucceed()
                        } else {
                            listener.onUploadFailed(reason)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "## onPostExecute() : failed")
                    }

                }
            }
        }.execute()
    }

    /**
     * Send a bug report either with email or with Vector.
     */
    fun openBugReportScreen(activity: Activity, forSuggestion: Boolean = false) {
        screenshot = takeScreenshot(activity)

        val intent = Intent(activity, BugReportActivity::class.java)
        intent.putExtra("FOR_SUGGESTION", forSuggestion)
        activity.startActivity(intent)
    }

    //==============================================================================================================
    // crash report management
    //==============================================================================================================

    /**
     * Provides the crash file
     *
     * @param context the context
     * @return the crash file
     */
    private fun getCrashFile(context: Context): File {
        return File(context.cacheDir.absolutePath, CRASH_FILENAME)
    }

    /**
     * Remove the crash file
     *
     * @param context
     */
    fun deleteCrashFile(context: Context) {
        val crashFile = getCrashFile(context)

        if (crashFile.exists()) {
            crashFile.delete()
        }

        // Also reset the screenshot
        screenshot = null
    }

    /**
     * Save the crash report
     *
     * @param context          the context
     * @param crashDescription teh crash description
     */
    fun saveCrashReport(context: Context, crashDescription: String) {
        val crashFile = getCrashFile(context)

        if (crashFile.exists()) {
            crashFile.delete()
        }

        if (!TextUtils.isEmpty(crashDescription)) {
            try {
                val fos = FileOutputStream(crashFile)
                val osw = OutputStreamWriter(fos)
                osw.write(crashDescription)
                osw.close()

                fos.flush()
                fos.close()
            } catch (e: Exception) {
                Timber.e(e, "## saveCrashReport() : fail to write $e")
            }

        }
    }

    /**
     * Read the crash description file and return its content.
     *
     * @param context teh context
     * @return the crash description
     */
    private fun getCrashDescription(context: Context): String? {
        var crashDescription: String? = null
        val crashFile = getCrashFile(context)

        if (crashFile.exists()) {
            try {
                val fis = FileInputStream(crashFile)
                val isr = InputStreamReader(fis)

                val buffer = CharArray(fis.available())
                val len = isr.read(buffer, 0, fis.available())
                crashDescription = String(buffer, 0, len)
                isr.close()
                fis.close()
            } catch (e: Exception) {
                Timber.e(e, "## getCrashDescription() : fail to read $e")
            }

        }

        return crashDescription
    }

    //==============================================================================================================
    // Screenshot management
    //==============================================================================================================

    /**
     * Take a screenshot of the display.
     *
     * @return the screenshot
     */
    private fun takeScreenshot(activity: Activity): Bitmap? {
        // get content view
        val contentView = activity.findViewById<View>(android.R.id.content)
        if (contentView == null) {
            Timber.e("Cannot find content view on $activity. Cannot take screenshot.")
            return null
        }

        // get the root view to snapshot
        val rootView = contentView.rootView
        if (rootView == null) {
            Timber.e("Cannot find root view on $activity. Cannot take screenshot.")
            return null
        }
        // refresh it
        rootView.isDrawingCacheEnabled = false
        rootView.isDrawingCacheEnabled = true

        try {
            var bitmap = rootView.drawingCache

            // Make a copy, because if Activity is destroyed, the bitmap will be recycled
            bitmap = Bitmap.createBitmap(bitmap)

            return bitmap
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "Cannot get drawing cache for $activity OOM.")
        } catch (e: Exception) {
            Timber.e(e, "Cannot get snapshot of screen: $e")
        }

        return null
    }

    //==============================================================================================================
    // Logcat management
    //==============================================================================================================

    /**
     * Save the logcat
     *
     * @param context       the context
     * @param isErrorLogcat true to save the error logcat
     * @return the file if the operation succeeds
     */
    private fun saveLogCat(context: Context, isErrorLogcat: Boolean): File? {
        val logCatErrFile = File(context.cacheDir.absolutePath, if (isErrorLogcat) LOG_CAT_ERROR_FILENAME else LOG_CAT_FILENAME)

        if (logCatErrFile.exists()) {
            logCatErrFile.delete()
        }

        try {
            val fos = FileOutputStream(logCatErrFile)
            val osw = OutputStreamWriter(fos)
            getLogCatError(osw, isErrorLogcat)
            osw.close()

            fos.flush()
            fos.close()

            return compressFile(logCatErrFile)
        } catch (error: OutOfMemoryError) {
            Timber.e(error, "## saveLogCat() : fail to write logcat$error")
        } catch (e: Exception) {
            Timber.e(e, "## saveLogCat() : fail to write logcat$e")
        }

        return null
    }

    /**
     * Retrieves the logs
     *
     * @param streamWriter  the stream writer
     * @param isErrorLogCat true to save the error logs
     */
    private fun getLogCatError(streamWriter: OutputStreamWriter, isErrorLogCat: Boolean) {
        val logcatProc: Process

        try {
            logcatProc = Runtime.getRuntime().exec(if (isErrorLogCat) LOGCAT_CMD_ERROR else LOGCAT_CMD_DEBUG)
        } catch (e1: IOException) {
            return
        }

        var reader: BufferedReader? = null
        try {
            val separator = System.getProperty("line.separator")
            reader = BufferedReader(InputStreamReader(logcatProc.inputStream), BUFFER_SIZE)
            var line = reader.readLine()
            while (line != null) {
                streamWriter.append(line)
                streamWriter.append(separator)
                line = reader.readLine()
            }
        } catch (e: IOException) {
            Timber.e(e, "getLog fails")
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    Timber.e(e, "getLog fails with")
                }

            }
        }
    }

    //==============================================================================================================
    // File compression management
    //==============================================================================================================

    /**
     * GZip a file
     *
     * @param fin the input file
     * @return the gzipped file
     */
    private fun compressFile(fin: File): File? {
        Timber.v("## compressFile() : compress ${fin.name}")

        val dstFile = File(fin.parent, fin.name + ".gz")

        if (dstFile.exists()) {
            dstFile.delete()
        }

        var fos: FileOutputStream? = null
        var gos: GZIPOutputStream? = null
        var inputStream: InputStream? = null
        try {
            fos = FileOutputStream(dstFile)
            gos = GZIPOutputStream(fos)

            inputStream = FileInputStream(fin)

            val buffer = ByteArray(2048)
            var n = inputStream.read(buffer)
            while (n != -1) {
                gos.write(buffer, 0, n)
                n = inputStream.read(buffer)
            }

            gos.close()
            inputStream.close()

            Timber.v("## compressFile() : ${fin.length()} compressed to ${dstFile.length()} bytes")
            return dstFile
        } catch (e: Exception) {
            Timber.e(e, "## compressFile() failed")
        } catch (oom: OutOfMemoryError) {
            Timber.e(oom, "## compressFile() failed")
        } finally {
            try {
                fos?.close()
                gos?.close()
                inputStream?.close()
            } catch (e: Exception) {
                Timber.e(e, "## compressFile() failed to close inputStream")
            }

        }

        return null
    }
}
