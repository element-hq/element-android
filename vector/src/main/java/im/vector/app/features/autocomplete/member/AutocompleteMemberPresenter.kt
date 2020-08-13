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
import androidx.recyclerview.widget.RecyclerView
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.RecyclerViewPresenter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary

class AutocompleteMemberPresenter @AssistedInject constructor(context: Context,
                                                              @Assisted val roomId: String,
                                                              private val session: Session,
                                                              private val controller: AutocompleteMemberController
) : RecyclerViewPresenter<RoomMemberSummary>(context), AutocompleteClickListener<RoomMemberSummary> {

    private val room = session.getRoom(roomId)!!

    init {
        controller.listener = this
    }

    fun clear() {
        controller.listener = null
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): AutocompleteMemberPresenter
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return controller.adapter
    }

    override fun onItemClick(t: RoomMemberSummary) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        val queryParams = roomMemberQueryParams {
            displayName = if (query.isNullOrBlank()) {
                QueryStringValue.IsNotEmpty
            } else {
                QueryStringValue.Contains(query.toString(), QueryStringValue.Case.INSENSITIVE)
            }
            memberships = listOf(Membership.JOIN)
            excludeSelf = true
        }
        val members = room.getRoomMembers(queryParams)
                .asSequence()
                .sortedBy { it.displayName }
        controller.setData(members.toList())
    }
}
