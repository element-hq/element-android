/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import javax.inject.Inject

class HomeDrawerActionsController @Inject constructor() : EpoxyController() {

    interface Listener {
        fun inviteByEmail()
        fun openTermAndConditions()
    }

    var listener: Listener? = null

    init {
        requestModelBuild()
    }

    override fun buildModels() {
        val host = this
        homeDrawerActionItem {
            id("emailInvite")
            titleRes(R.string.tchap_invite_to)
            iconRes(R.drawable.ic_tchap_invite)
            itemClickAction { host.listener?.inviteByEmail() }
        }
        homeDrawerActionItem {
            id("openTAC")
            titleRes(R.string.settings_app_term_conditions)
            iconRes(R.drawable.ic_tchap_term_conditions)
            itemClickAction { host.listener?.openTermAndConditions() }
        }
    }
}
