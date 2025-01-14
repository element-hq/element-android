/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import im.vector.lib.core.utils.compat.getParcelableArrayListExtraCompat
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.core.utils.compat.queryIntentActivitiesCompat
import timber.log.Timber

/**
 * Abstract class to provide all types of Pickers.
 */
abstract class Picker<T> {

    protected var single = false

    /**
     * Call this function from onActivityResult(int, int, Intent).
     * @return selected files or empty list if user did not select any files.
     */
    abstract fun getSelectedFiles(context: Context, data: Intent?): List<T>

    /**
     * Use this function to retrieve files which are shared from another application or internally
     * by using android.intent.action.SEND or android.intent.action.SEND_MULTIPLE actions.
     */
    fun getIncomingFiles(context: Context, data: Intent?): List<T> {
        if (data == null) return emptyList()

        val uriList = mutableListOf<Uri>()
        if (data.action == Intent.ACTION_SEND) {
            data.getParcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)?.let { uriList.add(it) }
        } else if (data.action == Intent.ACTION_SEND_MULTIPLE) {
            val extraUriList: List<Uri>? = data.getParcelableArrayListExtraCompat(Intent.EXTRA_STREAM)
            extraUriList?.let { uriList.addAll(it) }
        }

        val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivitiesCompat(data, PackageManager.MATCH_DEFAULT_ONLY)
        uriList.forEach {
            for (resolveInfo in resInfoList) {
                val packageName: String = resolveInfo.activityInfo.packageName

                // Replace implicit intent by an explicit to fix crash on some devices like Xiaomi.
                // see https://juejin.cn/post/7031736325422186510
                try {
                    context.grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    continue
                }
                data.action = null
                data.component = ComponentName(packageName, resolveInfo.activityInfo.name)
                break
            }
        }
        return getSelectedFiles(context, data)
    }

    /**
     * Call this function to disable multiple selection of files.
     */
    fun single(): Picker<T> {
        single = true
        return this
    }

    abstract fun createIntent(): Intent

    /**
     * Start Storage Access Framework UI by using a ActivityResultLauncher.
     * @param activityResultLauncher to handle the result.
     */
    fun startWith(activityResultLauncher: ActivityResultLauncher<Intent>) {
        activityResultLauncher.launch(createIntent().apply { addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
    }

    protected fun getSelectedUriList(context: Context, data: Intent?): List<Uri> {
        val selectedUriList = mutableListOf<Uri>()
        val dataUri = data?.data
        val clipData = data?.clipData

        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                selectedUriList.add(clipData.getItemAt(i).uri)
            }
        } else if (dataUri != null) {
            selectedUriList.add(dataUri)
        } else {
            @Suppress("DEPRECATION")
            data?.extras?.get(Intent.EXTRA_STREAM)?.let {
                (it as? List<*>)?.filterIsInstance<Uri>()?.let { uriList ->
                    selectedUriList.addAll(uriList)
                }
                if (it is Uri) {
                    selectedUriList.add(it)
                }
            }
        }
        selectedUriList.forEach { uri ->
            try {
                context.grantUriPermission(context.applicationContext.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: SecurityException) {
                // Handle the exception, e.g., log it or notify the user
                Timber.w("Picker", "Failed to grant URI permission for $uri: ${e.message}")
            }
        }
        return selectedUriList
    }
}
