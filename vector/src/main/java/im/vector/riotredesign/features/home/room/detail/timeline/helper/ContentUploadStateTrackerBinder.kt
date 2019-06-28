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

package im.vector.riotredesign.features.home.room.detail.timeline.helper

import android.content.Context
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.riotredesign.R
import im.vector.riotredesign.core.di.ActiveSessionHolder
import im.vector.riotredesign.features.media.ImageContentRenderer
import java.io.File
import javax.inject.Inject

class ContentUploadStateTrackerBinder @Inject constructor(private val activeSessionHolder: ActiveSessionHolder) {

    private val updateListeners = mutableMapOf<String, ContentUploadStateTracker.UpdateListener>()

    fun bind(eventId: String,
             mediaData: ImageContentRenderer.Data,
             progressLayout: ViewGroup) {

        activeSessionHolder.getActiveSession().also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            val updateListener = ContentMediaProgressUpdater(progressLayout, mediaData)
            updateListeners[eventId] = updateListener
            uploadStateTracker.track(eventId, updateListener)
        }
    }

    fun unbind(eventId: String) {
        activeSessionHolder.getActiveSession().also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            updateListeners[eventId]?.also {
                uploadStateTracker.untrack(eventId, it)
            }
        }
    }

}

private class ContentMediaProgressUpdater(private val progressLayout: ViewGroup,
                                          private val mediaData: ImageContentRenderer.Data) : ContentUploadStateTracker.UpdateListener {

    override fun onUpdate(state: ContentUploadStateTracker.State) {
        when (state) {
            is ContentUploadStateTracker.State.Idle         -> handleIdle(state)
            is ContentUploadStateTracker.State.Failure      -> handleFailure(state)
            is ContentUploadStateTracker.State.Success      -> handleSuccess(state)
            is ContentUploadStateTracker.State.ProgressData -> handleProgress(state)
        }
    }

    private fun handleIdle(state: ContentUploadStateTracker.State.Idle) {
        if (mediaData.isLocalFile()) {
            val file = File(mediaData.url)
            progressLayout.visibility = View.VISIBLE
            val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
            val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
            progressBar?.progress = 0
            progressTextView?.text = formatStats(progressLayout.context, 0L, file.length())
        } else {
            progressLayout.visibility = View.GONE
        }
    }

    private fun handleFailure(state: ContentUploadStateTracker.State.Failure) {

    }

    private fun handleSuccess(state: ContentUploadStateTracker.State.Success) {

    }

    private fun handleProgress(state: ContentUploadStateTracker.State.ProgressData) {
        progressLayout.visibility = View.VISIBLE
        val percent = 100L * (state.current.toFloat() / state.total.toFloat())
        val progressBar = progressLayout.findViewById<ProgressBar>(R.id.mediaProgressBar)
        val progressTextView = progressLayout.findViewById<TextView>(R.id.mediaProgressTextView)
        progressBar?.progress = percent.toInt()
        progressTextView?.text = formatStats(progressLayout.context, state.current, state.total)
    }

    private fun formatStats(context: Context, current: Long, total: Long): String {
        return "${Formatter.formatShortFileSize(context, current)} / ${Formatter.formatShortFileSize(context, total)}"
    }

}
