/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.helper

import android.graphics.drawable.Drawable
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import dagger.hilt.android.scopes.ActivityScoped
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.home.room.detail.timeline.item.MessageFileItem
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import javax.inject.Inject

@ActivityScoped
class ContentDownloadStateTrackerBinder @Inject constructor(private val activeSessionHolder: ActiveSessionHolder) {

    private val updateListeners = mutableMapOf<String, ContentDownloadUpdater>()

    fun bind(
            mxcUrl: String,
            holder: MessageFileItem.Holder
    ) {
        activeSessionHolder.getSafeActiveSession()?.also { session ->
            val downloadStateTracker = session.contentDownloadProgressTracker()
            val updateListener = ContentDownloadUpdater(holder)
            updateListeners[mxcUrl] = updateListener
            downloadStateTracker.track(mxcUrl, updateListener)
        }
    }

    fun unbind(mxcUrl: String) {
        activeSessionHolder.getSafeActiveSession()?.also { session ->
            val downloadStateTracker = session.contentDownloadProgressTracker()
            updateListeners[mxcUrl]?.also {
                it.stop()
                downloadStateTracker.unTrack(mxcUrl, it)
            }
        }
    }

    fun clear() {
        activeSessionHolder.getSafeActiveSession()?.also {
            it.contentDownloadProgressTracker().clear()
        }
    }
}

private class ContentDownloadUpdater(private val holder: MessageFileItem.Holder) : ContentDownloadStateTracker.UpdateListener {

    override fun onDownloadStateUpdate(state: ContentDownloadStateTracker.State) {
        when (state) {
            ContentDownloadStateTracker.State.Idle -> handleIdle()
            is ContentDownloadStateTracker.State.Downloading -> handleProgress(state)
            ContentDownloadStateTracker.State.Decrypting -> handleDecrypting()
            ContentDownloadStateTracker.State.Success -> handleSuccess()
            is ContentDownloadStateTracker.State.Failure -> handleFailure()
        }
    }

    private var animatedDrawable: AnimatedVectorDrawableCompat? = null
    private var animationLoopCallback = object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            animatedDrawable?.start()
        }
    }

    fun stop() {
        animatedDrawable?.unregisterAnimationCallback(animationLoopCallback)
        animatedDrawable?.stop()
        animatedDrawable = null
    }

    private fun handleIdle() {
        holder.fileDownloadProgress.progress = 0
        holder.fileDownloadProgress.isIndeterminate = false
    }

    private fun handleDecrypting() {
        holder.fileDownloadProgress.isIndeterminate = true
    }

    private fun handleProgress(state: ContentDownloadStateTracker.State.Downloading) {
        doHandleProgress(state.current, state.total)
    }

    private fun doHandleProgress(current: Long, total: Long) {
        val percent = 100L * (current.toFloat() / total.toFloat())
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = percent.toInt()
        if (animatedDrawable == null) {
            animatedDrawable = AnimatedVectorDrawableCompat.create(holder.view.context, R.drawable.ic_download_anim)
            holder.fileImageView.setImageDrawable(animatedDrawable)
            animatedDrawable?.start()
            animatedDrawable?.registerAnimationCallback(animationLoopCallback)
        }
    }

    private fun handleFailure() {
        stop()
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = 0
        holder.fileImageView.setImageResource(R.drawable.ic_close_round)
    }

    private fun handleSuccess() {
        stop()
        holder.fileDownloadProgress.isIndeterminate = false
        holder.fileDownloadProgress.progress = 0
        holder.fileImageView.setImageResource(R.drawable.ic_paperclip)
    }
}
