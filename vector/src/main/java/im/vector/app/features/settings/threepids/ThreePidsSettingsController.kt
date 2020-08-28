/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.threepids

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.GenericItem
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

class ThreePidsSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<ThreePidsSettingsViewState>() {

    interface InteractionListener {
        fun addEmail()
        fun addMsisdn()
        fun continueThreePid(threePid: ThreePid)
        fun deleteThreePid(threePid: ThreePid)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: ThreePidsSettingsViewState?) {
        if (data == null) return
        when (data.threePids) {
            is Loading -> {
                loadingItem {
                    id("loading")
                    loadingText(stringProvider.getString(R.string.loading))
                }
            }
            is Fail -> {
                genericFooterItem {
                    id("fail")
                    text(data.threePids.error.localizedMessage)
                }
            }
            is Success -> {
                val dataList = data.threePids.invoke()
                buildThreePids(dataList, data.pendingThreePids)
            }
        }
    }

    private fun buildThreePids(list: List<ThreePid>, pendingThreePids: Async<List<ThreePid>>) {
        val splited = list.groupBy { it is ThreePid.Email }
        val emails = splited[true].orEmpty()
        val msisdn = splited[false].orEmpty()

        settingsSectionTitleItem {
            id("email")
            title(stringProvider.getString(R.string.settings_emails))
        }

        emails.forEach { buildThreePid("email_", it) }

        // Pending threePids
        pendingThreePids.invoke()
                ?.filterIsInstance(ThreePid.Email::class.java)
                ?.forEach { buildPendingThreePid("email_", it) }

        genericButtonItem {
            id("addEmail")
            text(stringProvider.getString(R.string.settings_add_email_address))
            textColor(colorProvider.getColor(R.color.riotx_accent))
            buttonClickAction(View.OnClickListener { interactionListener?.addEmail() })
        }

        settingsSectionTitleItem {
            id("msisdn")
            title(stringProvider.getString(R.string.settings_phone_numbers))
        }

        msisdn.forEach { buildThreePid("msisdn_", it) }

        // Pending threePids
        pendingThreePids.invoke()
                ?.filterIsInstance(ThreePid.Msisdn::class.java)
                ?.forEach { buildPendingThreePid("msisdn_", it) }

        genericButtonItem {
            id("addMsisdn")
            text(stringProvider.getString(R.string.settings_add_phone_number))
            textColor(colorProvider.getColor(R.color.riotx_accent))
            buttonClickAction(View.OnClickListener { interactionListener?.addMsisdn() })
        }
    }

    private fun buildThreePid(idPrefix: String, threePid: ThreePid) {
        genericItem {
            id(idPrefix + threePid.value)
            title(threePid.value)
            destructiveButtonAction(
                    GenericItem.Action(stringProvider.getString(R.string.remove))
                            .apply {
                                perform = Runnable { interactionListener?.deleteThreePid(threePid) }
                            }
            )
        }
    }

    private fun buildPendingThreePid(idPrefix: String, threePid: ThreePid) {
        genericItem {
            id(idPrefix + threePid.value)
            title(threePid.value)
            if (threePid is ThreePid.Email) {
                description(stringProvider.getString(R.string.account_email_validation_message))
            }
            buttonAction(
                    GenericItem.Action(stringProvider.getString(R.string._continue))
                            .apply {
                                perform = Runnable { interactionListener?.continueThreePid(threePid) }
                            }
            )
        }
    }
}
