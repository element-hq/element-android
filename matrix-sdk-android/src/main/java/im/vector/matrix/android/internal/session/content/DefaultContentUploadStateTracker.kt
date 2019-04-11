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

internal class DefaultContentUploadStateTracker : ContentUploadStateTracker {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressByEvent = mutableMapOf<String, ContentUploadStateTracker.State>()
    private val listenersByEvent = mutableMapOf<String, MutableList<ContentUploadStateTracker.UpdateListener>>()

    override fun track(eventId: String, updateListener: ContentUploadStateTracker.UpdateListener) {
        val listeners = listenersByEvent[eventId] ?: ArrayList()
        listeners.add(updateListener)
        listenersByEvent[eventId] = listeners
        val currentState = progressByEvent[eventId] ?: ContentUploadStateTracker.State.Idle
        mainHandler.post { updateListener.onUpdate(currentState) }
    }

    override fun untrack(eventId: String, updateListener: ContentUploadStateTracker.UpdateListener) {
        listenersByEvent[eventId]?.apply {
            remove(updateListener)
        }
    }

    internal fun setFailure(eventId: String) {
        val failure = ContentUploadStateTracker.State.Failure
        updateState(eventId, failure)
    }

    internal fun setSuccess(eventId: String) {
        val success = ContentUploadStateTracker.State.Success
        updateState(eventId, success)
    }

    internal fun setProgress(eventId: String, current: Long, total: Long) {
        val progressData = ContentUploadStateTracker.State.ProgressData(current, total)
        updateState(eventId, progressData)
    }

    private fun updateState(eventId: String, state: ContentUploadStateTracker.State) {
        progressByEvent[eventId] = state
        mainHandler.post {
            listenersByEvent[eventId]?.also { listeners ->
                listeners.forEach { it.onUpdate(state) }
            }
        }
    }

}
