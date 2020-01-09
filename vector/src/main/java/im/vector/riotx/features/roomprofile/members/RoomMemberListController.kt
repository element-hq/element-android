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

package im.vector.riotx.features.roomprofile.members

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.epoxy.profiles.profileItemAction
import im.vector.riotx.core.epoxy.profiles.profileMatrixItem
import im.vector.riotx.features.autocomplete.autocompleteMatrixItem
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class RoomMemberListController @Inject constructor(private val avatarRenderer: AvatarRenderer) : TypedEpoxyController<RoomMemberListViewState>() {

    interface Callback {
        fun onRoomMemberClicked(roomMember: RoomMember)
    }

    var callback: Callback? = null

    override fun buildModels(data: RoomMemberListViewState?) {
        data?.roomMembers?.invoke()?.forEach { roomMember ->
            profileMatrixItem {
                id(roomMember.userId)
                matrixItem(roomMember.toMatrixItem())
                avatarRenderer(avatarRenderer)
                clickListener { _ ->
                    callback?.onRoomMemberClicked(roomMember)
                }
            }
        }
    }

}
