/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.paging.PagedListEpoxyController
import im.vector.app.core.utils.createUIHandler
import im.vector.app.features.home.RoomListDisplayMode
import im.vector.app.features.settings.FontScalePreferences
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class RoomSummaryPagedController(
        private val roomSummaryItemFactory: RoomSummaryItemFactory,
        private val displayMode: RoomListDisplayMode,
        fontScalePreferences: FontScalePreferences
) : PagedListEpoxyController<RoomSummary>(
        // Important it must match the PageList builder notify Looper
        modelBuildingHandler = createUIHandler()
), CollapsableControllerExtension {

    var listener: RoomListListener? = null
    private val shouldUseSingleLine: Boolean

    init {
        val fontScale = fontScalePreferences.getResolvedFontScaleValue()
        shouldUseSingleLine = fontScale.scale > FontScalePreferences.SCALE_LARGE
    }

    var roomChangeMembershipStates: Map<String, ChangeMembershipState>? = null
        set(value) {
            field = value
            // ideally we could search for visible models and update only those
            requestForcedModelBuild()
        }

    override var collapsed = false
        set(value) {
            if (field != value) {
                field = value
                requestForcedModelBuild()
            }
        }

    override fun addModels(models: List<EpoxyModel<*>>) {
        if (collapsed) {
            super.addModels(emptyList())
        } else {
            super.addModels(models)
        }
    }

    override fun buildItemModel(currentPosition: Int, item: RoomSummary?): EpoxyModel<*> {
        return if (item == null) {
            val host = this
            RoomSummaryPlaceHolderItem_().apply {
                id(currentPosition)
                useSingleLineForLastEvent(host.shouldUseSingleLine)
            }
        } else {
            roomSummaryItemFactory.create(item, roomChangeMembershipStates.orEmpty(), emptySet(), displayMode, listener, shouldUseSingleLine)
        }
    }
}
