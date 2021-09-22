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

import im.vector.app.core.extensions.inflateIfNeeded
import im.vector.app.databinding.FragmentRoomDetailBinding
import im.vector.app.features.invite.VectorInviteView

/**
 * This is an holder for lazy loading some views of the RoomDetail screen.
 * It's using some ViewStub where it makes sense.
 */
class RoomDetailLazyLoadedViews {

    private var roomDetailBinding: FragmentRoomDetailBinding? = null

    var inviteView: VectorInviteView? = null
        private set
        get() {
            roomDetailBinding?.inviteViewStub?.inflateIfNeeded<VectorInviteView> {
                inviteView = it
            }
            return field
        }

    fun bind(roomDetailBinding: FragmentRoomDetailBinding) {
        this.roomDetailBinding = roomDetailBinding
    }

    fun unBind() {
        roomDetailBinding = null
        inviteView = null
    }
}
