/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher

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
            (data.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let { uriList.add(it) }
        } else if (data.action == Intent.ACTION_SEND_MULTIPLE) {
            val extraUriList: List<Uri>? = data.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            extraUriList?.let { uriList.addAll(it) }
        }

        val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(data, PackageManager.MATCH_DEFAULT_ONLY)
        uriList.forEach {
            for (resolveInfo in resInfoList) {
                val packageName: String = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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

    protected fun getSelectedUriList(data: Intent?): List<Uri> {
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
            data?.extras?.get(Intent.EXTRA_STREAM)?.let {
                (it as? List<*>)?.filterIsInstance<Uri>()?.let { uriList ->
                    selectedUriList.addAll(uriList)
                }
                if (it is Uri) {
                    selectedUriList.add(it)
                }
            }
        }
        return selectedUriList
    }
}
