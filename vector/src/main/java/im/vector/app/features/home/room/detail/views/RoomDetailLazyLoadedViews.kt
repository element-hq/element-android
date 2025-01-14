/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.views

import android.view.View
import android.view.ViewStub
import im.vector.app.core.ui.views.FailedMessagesWarningView
import im.vector.app.databinding.FragmentTimelineBinding
import im.vector.app.features.invite.VectorInviteView
import kotlin.reflect.KMutableProperty0

/**
 * This is an holder for lazy loading some views of the RoomDetail screen.
 * It's using some ViewStub where it makes sense.
 */
class RoomDetailLazyLoadedViews {

    private var roomDetailBinding: FragmentTimelineBinding? = null

    private var failedMessagesWarningView: FailedMessagesWarningView? = null
    private var inviteView: VectorInviteView? = null

    fun bind(roomDetailBinding: FragmentTimelineBinding) {
        this.roomDetailBinding = roomDetailBinding
    }

    fun unBind() {
        roomDetailBinding = null
        inviteView = null
        failedMessagesWarningView = null
    }

    fun failedMessagesWarningView(inflateIfNeeded: Boolean, callback: FailedMessagesWarningView.Callback? = null): FailedMessagesWarningView? {
        return getOrInflate(inflateIfNeeded, roomDetailBinding?.failedMessagesWarningStub, this::failedMessagesWarningView)?.apply {
            this.callback = callback
        }
    }

    fun inviteView(inflateIfNeeded: Boolean): VectorInviteView? {
        return getOrInflate(inflateIfNeeded, roomDetailBinding?.inviteViewStub, this::inviteView)
    }

    private inline fun <reified T : View> getOrInflate(inflateIfNeeded: Boolean, stub: ViewStub?, reference: KMutableProperty0<T?>): T? {
        if (!inflateIfNeeded || stub == null || stub.parent == null) return reference.get()
        val inflatedView = stub.inflate() as T
        reference.set(inflatedView)
        return inflatedView
    }
}
