/*
 * Copyright (c) 2021 New Vector Ltd
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
