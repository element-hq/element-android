/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.autocomplete.member

import android.content.Context
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.autocompleteHeaderItem
import im.vector.app.features.autocomplete.autocompleteMatrixItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
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
                is AutocompleteMemberItem.Header -> buildHeaderItem(item)
                is AutocompleteMemberItem.RoomMember -> buildRoomMemberItem(item)
                is AutocompleteMemberItem.Everyone -> buildEveryoneItem(item)
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
                subName(user.userId)
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
                subName(host.context.getString(CommonStrings.room_message_notify_everyone))
                avatarRenderer(host.avatarRenderer)
                clickListener { host.listener?.onItemClick(everyone) }
            }
        }
    }
}
