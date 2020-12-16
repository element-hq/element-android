/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.lib.attachmentviewer

import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import im.vector.lib.attachmentviewer.databinding.ItemVideoAttachmentBinding
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

// TODO, it would be probably better to use a unique media player
// for better customization and control
// But for now VideoView is enough, it released player when detached, we use a timer to update progress
class VideoViewHolder constructor(itemView: View) :
        BaseViewHolder(itemView) {

    private var isSelected = false
    private var mVideoPath: String? = null
    private var progressDisposable: Disposable? = null
    private var progress: Int = 0
    private var wasPaused = false

    var eventListener: WeakReference<AttachmentEventListener>? = null

    val views = ItemVideoAttachmentBinding.bind(itemView)

    internal val target = DefaultVideoLoaderTarget(this, views.videoThumbnailImage)

    override fun onRecycled() {
        super.onRecycled()
        progressDisposable?.dispose()
        progressDisposable = null
        mVideoPath = null
    }

    fun videoReady(file: File) {
        mVideoPath = file.path
        if (isSelected) {
            startPlaying()
        }
    }

    fun videoReady(path: String) {
        mVideoPath = path
        if (isSelected) {
            startPlaying()
        }
    }

    fun videoFileLoadError() {
    }

    override fun entersBackground() {
        if (views.videoView.isPlaying) {
            progress = views.videoView.currentPosition
            progressDisposable?.dispose()
            progressDisposable = null
            views.videoView.stopPlayback()
            views.videoView.pause()
        }
    }

    override fun entersForeground() {
        onSelected(isSelected)
    }

    override fun onSelected(selected: Boolean) {
        if (!selected) {
            if (views.videoView.isPlaying) {
                progress = views.videoView.currentPosition
                views.videoView.stopPlayback()
            } else {
                progress = 0
            }
            progressDisposable?.dispose()
            progressDisposable = null
        } else {
            if (mVideoPath != null) {
                startPlaying()
            }
        }
        isSelected = true
    }

    private fun startPlaying() {
        views.videoThumbnailImage.isVisible = false
        views.videoLoaderProgress.isVisible = false
        views.videoView.isVisible = true

        views.videoView.setOnPreparedListener {
            progressDisposable?.dispose()
            progressDisposable = Observable.interval(100, TimeUnit.MILLISECONDS)
                    .timeInterval()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        val duration = views.videoView.duration
                        val progress = views.videoView.currentPosition
                        val isPlaying = views.videoView.isPlaying
//                        Log.v("FOO", "isPlaying $isPlaying $progress/$duration")
                        eventListener?.get()?.onEvent(AttachmentEvents.VideoEvent(isPlaying, progress, duration))
                    }
        }
        try {
            views.videoView.setVideoPath(mVideoPath)
        } catch (failure: Throwable) {
            // Couldn't open
            Log.v(VideoViewHolder::class.java.name, "Failed to start video")
        }

        if (!wasPaused) {
            views.videoView.start()
            if (progress > 0) {
                views.videoView.seekTo(progress)
            }
        }
    }

    override fun handleCommand(commands: AttachmentCommands) {
        if (!isSelected) return
        when (commands) {
            AttachmentCommands.StartVideo -> {
                wasPaused = false
                views.videoView.start()
            }
            AttachmentCommands.PauseVideo -> {
                wasPaused = true
                views.videoView.pause()
            }
            is AttachmentCommands.SeekTo  -> {
                val duration = views.videoView.duration
                if (duration > 0) {
                    val seekDuration = duration * (commands.percentProgress / 100f)
                    views.videoView.seekTo(seekDuration.toInt())
                }
            }
        }
    }

    override fun bind(attachmentInfo: AttachmentInfo) {
        super.bind(attachmentInfo)
        progress = 0
        wasPaused = false
    }
}
