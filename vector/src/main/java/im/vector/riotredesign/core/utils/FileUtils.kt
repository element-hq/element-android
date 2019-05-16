/*
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

package im.vector.riotredesign.core.utils

import android.content.Context
import android.text.TextUtils
import timber.log.Timber
import java.io.File

// Implementation should return true in case of success
typealias ActionOnFile = (file: File) -> Boolean

/* ==========================================================================================
 * Delete
 * ========================================================================================== */

fun deleteAllFiles(context: Context) {
    Timber.v("Delete cache dir:")
    recursiveActionOnFile(context.cacheDir, ::deleteAction)

    Timber.v("Delete files dir:")
    recursiveActionOnFile(context.filesDir, ::deleteAction)
}

private fun deleteAction(file: File): Boolean {
    if (file.exists()) {
        Timber.v("deleteFile: $file")
        return file.delete()
    }

    return true
}

/* ==========================================================================================
 * Log
 * ========================================================================================== */

fun lsFiles(context: Context) {
    Timber.v("Content of cache dir:")
    recursiveActionOnFile(context.cacheDir, ::logAction)

    Timber.v("Content of files dir:")
    recursiveActionOnFile(context.filesDir, ::logAction)
}

private fun logAction(file: File): Boolean {
    if (file.isDirectory) {
        Timber.d(file.toString())
    } else {
        Timber.d(file.toString() + " " + file.length() + " bytes")
    }
    return true
}

/* ==========================================================================================
 * Private
 * ========================================================================================== */

/**
 * Return true in case of success
 */
private fun recursiveActionOnFile(file: File, action: ActionOnFile): Boolean {
    if (file.isDirectory) {
        file.list().forEach {
            val result = recursiveActionOnFile(File(file, it), action)

            if (!result) {
                // Break the loop
                return false
            }
        }
    }

    return action.invoke(file)
}


/**
 * Get the file extension of a fileUri or a filename
 *
 * @param fileUri the fileUri (can be a simple filename)
 * @return the file extension, in lower case, or null is extension is not available or empty
 */
fun getFileExtension(fileUri: String): String? {
    var reducedStr = fileUri

    if (!TextUtils.isEmpty(reducedStr)) {
        // Remove fragment
        val fragment = fileUri.lastIndexOf('#')
        if (fragment > 0) {
            reducedStr = fileUri.substring(0, fragment)
        }

        // Remove query
        val query = reducedStr.lastIndexOf('?')
        if (query > 0) {
            reducedStr = reducedStr.substring(0, query)
        }

        // Remove path
        val filenamePos = reducedStr.lastIndexOf('/')
        val filename = if (0 <= filenamePos) reducedStr.substring(filenamePos + 1) else reducedStr

        // Contrary to method MimeTypeMap.getFileExtensionFromUrl, we do not check the pattern
        // See https://stackoverflow.com/questions/14320527/android-should-i-use-mimetypemap-getfileextensionfromurl-bugs
        if (!filename.isEmpty()) {
            val dotPos = filename.lastIndexOf('.')
            if (0 <= dotPos) {
                val ext = filename.substring(dotPos + 1)

                if (ext.isNotBlank()) {
                    return ext.toLowerCase()
                }
            }
        }
    }

    return null
}