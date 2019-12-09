/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.content

import android.os.Handler
import android.os.Looper
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.matrix.android.internal.session.SessionScope
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
        mainHandler.post { updateListener.onUpdate(currentState) }
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

    internal fun setEncrypting(key: String) {
        val progressData = ContentUploadStateTracker.State.Encrypting
        updateState(key, progressData)
    }

    internal fun setProgress(key: String, current: Long, total: Long) {
        val progressData = ContentUploadStateTracker.State.Uploading(current, total)
        updateState(key, progressData)
    }

    private fun updateState(key: String, state: ContentUploadStateTracker.State) {
        states[key] = state
        mainHandler.post {
            listeners[key]?.forEach { it.onUpdate(state) }
        }
    }
}
