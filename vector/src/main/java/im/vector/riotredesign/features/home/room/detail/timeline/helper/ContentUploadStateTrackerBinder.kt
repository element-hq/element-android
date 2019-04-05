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
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.content.ContentUploadStateTracker
import im.vector.riotredesign.R

object ContentUploadStateTrackerBinder {

    private val updateListeners = mutableMapOf<String, ContentUploadStateTracker.UpdateListener>()

    fun bind(eventId: String, progressLayout: ViewGroup) {
        Matrix.getInstance().currentSession?.also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            val updateListener = ContentMediaProgressUpdater(progressLayout)
            updateListeners[eventId] = updateListener
            uploadStateTracker.track(eventId, updateListener)
        }
    }

    fun unbind(eventId: String) {
        Matrix.getInstance().currentSession?.also { session ->
            val uploadStateTracker = session.contentUploadProgressTracker()
            updateListeners[eventId]?.also {
                uploadStateTracker.untrack(eventId, it)
            }
        }
    }

}

private class ContentMediaProgressUpdater(private val progressLayout: ViewGroup) : ContentUploadStateTracker.UpdateListener {

    override fun onUpdate(state: ContentUploadStateTracker.State) {
        when (state) {
            is ContentUploadStateTracker.State.Idle,
            is ContentUploadStateTracker.State.Failure,
            is ContentUploadStateTracker.State.Success      -> hideProgress()
            is ContentUploadStateTracker.State.ProgressData -> showProgress(state)
        }
    }

    private fun hideProgress() {
        progressLayout.visibility = View.GONE
    }

    private fun showProgress(state: ContentUploadStateTracker.State.ProgressData) {
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
