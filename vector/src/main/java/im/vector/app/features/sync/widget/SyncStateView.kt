/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.sync.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.isAirplaneModeOn
import im.vector.app.databinding.ViewSyncStateBinding
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.api.session.sync.SyncState

class SyncStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
        LinearLayout(context, attrs, defStyle) {

    private val views: ViewSyncStateBinding

    init {
        inflate(context, R.layout.view_sync_state, this)
        views = ViewSyncStateBinding.bind(this)
        orientation = VERTICAL
    }

    @SuppressLint("SetTextI18n")
    fun render(
            newState: SyncState?,
            incrementalSyncRequestState: SyncRequestState.IncrementalSyncRequestState?,
            pushCounter: Int,
            showDebugInfo: Boolean
    ) {
        views.syncStateDebugInfo.isVisible = showDebugInfo
        if (showDebugInfo) {
            views.syncStateDebugInfoText.text =
                    "Sync thread : ${newState.toHumanReadable()}\nSync request: ${incrementalSyncRequestState.toHumanReadable()}"
            views.syncStateDebugInfoPushCounter.text =
                    "Push: $pushCounter"
        }
        views.syncStateProgressBar.isVisible = newState is SyncState.Running && newState.afterPause

        if (newState == SyncState.NoNetwork) {
            val isAirplaneModeOn = context.isAirplaneModeOn()
            views.syncStateNoNetwork.isVisible = isAirplaneModeOn.not()
            views.syncStateNoNetworkAirplane.isVisible = isAirplaneModeOn
        } else {
            views.syncStateNoNetwork.isVisible = false
            views.syncStateNoNetworkAirplane.isVisible = false
        }
    }

    private fun SyncState?.toHumanReadable(): String {
        return when (this) {
            null -> "Unknown"
            SyncState.Idle -> "Idle"
            SyncState.InvalidToken -> "InvalidToken"
            SyncState.Killed -> "Killed"
            SyncState.Killing -> "Killing"
            SyncState.NoNetwork -> "NoNetwork"
            SyncState.Paused -> "Paused"
            is SyncState.Running -> "$this"
        }
    }

    private fun SyncRequestState.IncrementalSyncRequestState?.toHumanReadable(): String {
        return when (this) {
            null -> "Unknown"
            SyncRequestState.IncrementalSyncIdle -> "Idle"
            is SyncRequestState.IncrementalSyncParsing -> "Parsing ${this.rooms} room(s) ${this.toDevice} toDevice(s)"
            SyncRequestState.IncrementalSyncError -> "Error"
            SyncRequestState.IncrementalSyncDone -> "Done"
            else -> "?"
        }
    }
}
