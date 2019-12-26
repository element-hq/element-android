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

package im.vector.riotx.features.autocomplete.member

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import com.otaliastudios.autocomplete.RecyclerViewPresenter
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.riotx.features.autocomplete.AutocompleteClickListener
import javax.inject.Inject

class AutocompleteMemberPresenter @Inject constructor(context: Context,
                                                      private val controller: AutocompleteMemberController
) : RecyclerViewPresenter<RoomMember>(context), AutocompleteClickListener<RoomMember> {

    var callback: Callback? = null

    init {
        controller.listener = this
    }

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        // Also remove animation
        recyclerView?.itemAnimator = null
        return controller.adapter
    }

    override fun onItemClick(t: RoomMember) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        callback?.onQueryMembers(query)
    }

    fun render(members: Async<List<RoomMember>>) {
        if (members is Success) {
            controller.setData(members())
        }
    }

    interface Callback {
        fun onQueryMembers(query: CharSequence?)
    }
}
