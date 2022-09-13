/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.home.invites

import com.airbnb.epoxy.EpoxyController
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

class InvitesCounterController @Inject constructor(
        val stringProvider: StringProvider
) : EpoxyController() {

    private var count = 0
    var clickListener: (() -> Unit)? = null

    override fun buildModels() {
        val host = this
        if (count != 0) {
            inviteCounterItem {
                id("invites_counter")
                invitesCount(host.count)
                listener { host.clickListener?.invoke() }
            }
        }
    }

    fun submitData(count: Int?) {
        this.count = count ?: 0
        requestModelBuild()
    }
}
