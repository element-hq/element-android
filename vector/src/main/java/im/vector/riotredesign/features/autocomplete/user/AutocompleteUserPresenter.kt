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

package im.vector.riotredesign.features.autocomplete.user

import android.content.Context
import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Success
import im.vector.matrix.android.api.session.user.model.User
import im.vector.riotredesign.features.autocomplete.EpoxyViewPresenter

class AutocompleteUserPresenter(context: Context,
                                private val controller: AutocompleteUserController
) : EpoxyViewPresenter<User>(context) {

    var callback: Callback? = null

    override fun providesController(): EpoxyController {
        return controller
    }

    override fun onQuery(query: CharSequence?) {
        callback?.onQueryUsers(query)
    }

    fun render(users: Async<List<User>>) {
        if (users is Success) {
            controller.setData(users())
        }
    }

    interface Callback {
        fun onQueryUsers(query: CharSequence?)
    }

}