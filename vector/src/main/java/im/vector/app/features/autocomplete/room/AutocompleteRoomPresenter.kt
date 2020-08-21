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

class AutocompleteRoomPresenter @Inject constructor(context: Context,
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
        val rooms = session.getRoomSummaries(queryParams)
                .asSequence()
                .sortedBy { it.displayName }
        controller.setData(rooms.toList())
    }

    fun clear() {
        controller.listener = null
    }
}
