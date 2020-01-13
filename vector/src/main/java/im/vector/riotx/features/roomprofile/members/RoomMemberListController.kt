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
import im.vector.matrix.android.api.session.room.model.RoomMemberSummary
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.profiles.buildProfileSection
import im.vector.riotx.core.epoxy.profiles.profileMatrixItem
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class RoomMemberListController @Inject constructor(private val avatarRenderer: AvatarRenderer,
                                                   private val stringProvider: StringProvider) : TypedEpoxyController<RoomMemberListViewState>() {

    interface Callback {
        fun onRoomMemberClicked(roomMember: RoomMemberSummary)
    }

    var callback: Callback? = null

    init {
        setData(null)
    }

    override fun buildModels(data: RoomMemberListViewState?) {
        val roomMembersByPowerLevel = data?.roomMemberSummaries?.invoke() ?: return
        for ((powerLevelCategory, roomMemberList) in roomMembersByPowerLevel) {
            if (roomMemberList.isEmpty()) {
                continue
            }
            buildProfileSection(
                    stringProvider.getString(powerLevelCategory.titleRes)
            )
            roomMemberList.forEach { roomMember ->
                profileMatrixItem {
                    id(roomMember.userId)
                    matrixItem(roomMember.toMatrixItem())
                    avatarRenderer(avatarRenderer)
                    clickListener { _ ->
                        callback?.onRoomMemberClicked(roomMember)
                    }
                }

                dividerItem {
                    id("divider_${roomMember.userId}")
                }
            }
        }
    }


}
