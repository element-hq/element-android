/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.content

import android.os.Handler
import android.os.Looper
import org.matrix.android.sdk.api.session.content.ContentUploadStateTracker
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultContentUploadStateTracker @Inject constructor() : ContentUploadStateTracker {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val states = mutableMapOf<String, ContentUploadStateTracker.State>()
    private val listeners = mutableMapOf<String, MutableList<ContentUploadStateTracker.UpdateListener>>()

    override fun track(key: String, updateListener: ContentUploadStateTracker.UpdateListener) {
        val listeners = listeners.getOrPut(key) { ArrayList() }
        listeners.add(updateListener)
        val currentState = states[key] ?: ContentUploadStateTracker.State.Idle
        mainHandler.post {
            try {
                updateListener.onUpdate(currentState)
            } catch (e: Exception) {
                Timber.e(e, "## ContentUploadStateTracker.onUpdate() failed")
            }
        }
    }

    override fun untrack(key: String, updateListener: ContentUploadStateTracker.UpdateListener) {
        listeners[key]?.apply {
            remove(updateListener)
        }
    }

    override fun clear() {
        listeners.clear()
    }

    internal fun setFailure(key: String, throwable: Throwable) {
        val failure = ContentUploadStateTracker.State.Failure(throwable)
        updateState(key, failure)
    }

    internal fun setSuccess(key: String) {
        val success = ContentUploadStateTracker.State.Success
        updateState(key, success)
    }

    internal fun setEncryptingThumbnail(key: String) {
        val progressData = ContentUploadStateTracker.State.EncryptingThumbnail
        updateState(key, progressData)
    }

    internal fun setProgressThumbnail(key: String, current: Long, total: Long) {
        val progressData = ContentUploadStateTracker.State.UploadingThumbnail(current, total)
        updateState(key, progressData)
    }

    internal fun setEncrypting(key: String, current: Long, total: Long) {
        val progressData = ContentUploadStateTracker.State.Encrypting(current, total)
        updateState(key, progressData)
    }

    internal fun setCompressingImage(key: String) {
        val progressData = ContentUploadStateTracker.State.CompressingImage
        updateState(key, progressData)
    }

    internal fun setCompressingVideo(key: String, percent: Float) {
        val progressData = ContentUploadStateTracker.State.CompressingVideo(percent)
        updateState(key, progressData)
    }

    internal fun setProgress(key: String, current: Long, total: Long) {
        val progressData = ContentUploadStateTracker.State.Uploading(current, total)
        updateState(key, progressData)
    }

    private fun updateState(key: String, state: ContentUploadStateTracker.State) {
        states[key] = state
        mainHandler.post {
            listeners[key]?.forEach {
                try {
                    it.onUpdate(state)
                } catch (e: Exception) {
                    Timber.e(e, "## ContentUploadStateTracker.onUpdate() failed")
                }
            }
        }
    }
}
