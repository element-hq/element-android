/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.espresso.tools

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
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
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
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
    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "$screenshotName.jpeg")
    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, screenshotLocation)
    val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        contentResolver.openOutputStream(uri)?.let { saveScreenshotToStream(bitmap, it) }
        contentResolver.update(uri, contentValues, null, null)
    }
}

@Suppress("DEPRECATION")
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
    val file = File(directory, "$screenshotName.jpeg")
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
