/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.download

import android.os.Handler
import android.os.Looper
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultContentDownloadStateTracker @Inject constructor() : ProgressListener, ContentDownloadStateTracker {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val states = mutableMapOf<String, ContentDownloadStateTracker.State>()
    private val listeners = mutableMapOf<String, MutableList<ContentDownloadStateTracker.UpdateListener>>()

    override fun track(key: String, updateListener: ContentDownloadStateTracker.UpdateListener) {
        val listeners = listeners.getOrPut(key) { ArrayList() }
        if (!listeners.contains(updateListener)) {
            listeners.add(updateListener)
        }
        val currentState = states[key] ?: ContentDownloadStateTracker.State.Idle
        mainHandler.post {
            try {
                updateListener.onDownloadStateUpdate(currentState)
            } catch (e: Exception) {
                Timber.e(e, "## ContentUploadStateTracker.onUpdate() failed")
            }
        }
    }

    override fun unTrack(key: String, updateListener: ContentDownloadStateTracker.UpdateListener) {
        listeners[key]?.apply {
            remove(updateListener)
        }
    }

    override fun clear() {
        states.clear()
        listeners.clear()
    }

//    private fun URL.toKey() = toString()

    override fun update(url: String, bytesRead: Long, contentLength: Long, done: Boolean) {
        mainHandler.post {
            Timber.v("## DL Progress url:$url read:$bytesRead total:$contentLength done:$done")
            if (done) {
                updateState(url, ContentDownloadStateTracker.State.Success)
            } else {
                updateState(url, ContentDownloadStateTracker.State.Downloading(bytesRead, contentLength, contentLength == -1L))
            }
        }
    }

    override fun error(url: String, errorCode: Int) {
        mainHandler.post {
            Timber.v("## DL Progress Error code:$errorCode")
            updateState(url, ContentDownloadStateTracker.State.Failure(errorCode))
            listeners[url]?.forEach {
                tryOrNull { it.onDownloadStateUpdate(ContentDownloadStateTracker.State.Failure(errorCode)) }
            }
        }
    }

    private fun updateState(url: String, state: ContentDownloadStateTracker.State) {
        states[url] = state
        listeners[url]?.forEach {
            tryOrNull { it.onDownloadStateUpdate(state) }
        }
    }
}
