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

package im.vector.app.features.autocomplete.member

import android.content.Context
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.autocompleteHeaderItem
import im.vector.app.features.autocomplete.autocompleteMatrixItem
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.toEveryoneInRoomMatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class AutocompleteMemberController @Inject constructor(private val context: Context) :
        TypedEpoxyController<List<AutocompleteMemberItem>>() {

    /* ==========================================================================================
     * Fields
     * ========================================================================================== */

    var listener: AutocompleteClickListener<AutocompleteMemberItem>? = null

    /* ==========================================================================================
     * Dependencies
     * ========================================================================================== */

    @Inject lateinit var avatarRenderer: AvatarRenderer

    /* ==========================================================================================
     * Specialization
     * ========================================================================================== */

    override fun buildModels(data: List<AutocompleteMemberItem>?) {
        if (data.isNullOrEmpty()) {
            return
        }
        data.forEach { item ->
            when (item) {
                is AutocompleteMemberItem.Header     -> buildHeaderItem(item)
                is AutocompleteMemberItem.RoomMember -> buildRoomMemberItem(item)
                is AutocompleteMemberItem.Everyone   -> buildEveryoneItem(item)
            }
        }
    }

    /* ==========================================================================================
     * Helper methods
     * ========================================================================================== */

    private fun buildHeaderItem(header: AutocompleteMemberItem.Header) {
        autocompleteHeaderItem {
            id(header.id)
            title(header.title)
        }
    }

    private fun buildRoomMemberItem(roomMember: AutocompleteMemberItem.RoomMember) {
        val host = this
        autocompleteMatrixItem {
            roomMember.roomMemberSummary.let { user ->
                id(user.userId)
                matrixItem(user.toMatrixItem())
                avatarRenderer(host.avatarRenderer)
                clickListener { host.listener?.onItemClick(roomMember) }
            }
        }
    }

    private fun buildEveryoneItem(everyone: AutocompleteMemberItem.Everyone) {
        val host = this
        autocompleteMatrixItem {
            everyone.roomSummary.let { room ->
                id(room.roomId)
                matrixItem(room.toEveryoneInRoomMatrixItem())
                subName(host.context.getString(R.string.room_message_notify_everyone))
                avatarRenderer(host.avatarRenderer)
                clickListener { host.listener?.onItemClick(everyone) }
            }
        }
    }
}
