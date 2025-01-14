/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.espresso.tools

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import im.vector.lib.core.utils.timer.DefaultClock
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SCREENSHOT_FOLDER_LOCATION = "${Environment.DIRECTORY_PICTURES}/failure_screenshots"
private val deviceLanguage = Locale.getDefault().language

class ScreenshotFailureRule : TestWatcher() {
    override fun failed(e: Throwable?, description: Description) {
        val screenShotName = "$deviceLanguage-${description.methodName}-${SimpleDateFormat("EEE-MMMM-dd-HHmmss").format(Date())}"
        val bitmap = getInstrumentation().uiAutomation.takeScreenshot()
        storeFailureScreenshot(bitmap, screenShotName)
    }
}

/**
 * Stores screenshots in sdcard/Pictures/failure_screenshots
 */
private fun storeFailureScreenshot(bitmap: Bitmap, screenshotName: String) {
    val contentResolver = getInstrumentation().targetContext.applicationContext.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_TAKEN, DefaultClock().epochMillis())
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        useMediaStoreScreenshotStorage(
                contentValues,
                contentResolver,
                screenshotName,
                SCREENSHOT_FOLDER_LOCATION,
                bitmap
        )
    } else {
        usePublicExternalScreenshotStorage(
                contentValues,
                contentResolver,
                screenshotName,
                SCREENSHOT_FOLDER_LOCATION,
                bitmap
        )
    }
}

private fun useMediaStoreScreenshotStorage(
        contentValues: ContentValues,
        contentResolver: ContentResolver,
        screenshotName: String,
        screenshotLocation: String,
        bitmap: Bitmap
) {
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "${screenshotName}.jpeg")
    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, screenshotLocation)
    val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        contentResolver.openOutputStream(uri)?.let { saveScreenshotToStream(bitmap, it) }
        contentResolver.update(uri, contentValues, null, null)
    }
}

private fun usePublicExternalScreenshotStorage(
        contentValues: ContentValues,
        contentResolver: ContentResolver,
        screenshotName: String,
        screenshotLocation: String,
        bitmap: Bitmap
) {
    val directory = File(Environment.getExternalStoragePublicDirectory(screenshotLocation).toString())
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "${screenshotName}.jpeg")
    saveScreenshotToStream(bitmap, FileOutputStream(file))
    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
}

private fun saveScreenshotToStream(bitmap: Bitmap, outputStream: OutputStream) {
    outputStream.use {
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, it)
        } catch (e: IOException) {
            Timber.e("Screenshot was not stored at this time")
        }
    }
}
