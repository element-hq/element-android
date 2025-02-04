/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.autocomplete.room

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.RecyclerViewPresenter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.roomSummaryQueryParams
import javax.inject.Inject

class AutocompleteRoomPresenter @Inject constructor(
        context: Context,
        private val controller: AutocompleteRoomController,
        private val session: Session
) : RecyclerViewPresenter<RoomSummary>(context), AutocompleteClickListener<RoomSummary> {

    init {
        controller.listener = this
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return controller.adapter
    }

    override fun onItemClick(t: RoomSummary) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        val queryParams = roomSummaryQueryParams {
            canonicalAlias = if (query.isNullOrBlank()) {
                QueryStringValue.IsNotNull
            } else {
                QueryStringValue.Contains(query.toString(), QueryStringValue.Case.INSENSITIVE)
            }
        }
        val rooms = session.roomService().getRoomSummaries(queryParams)
                .asSequence()
                .sortedBy { it.displayName }
        controller.setData(rooms.toList())
    }

    fun clear() {
        controller.listener = null
    }
}
