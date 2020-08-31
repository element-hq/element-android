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

import android.text.InputType
import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import im.vector.app.R
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.getFormattedValue
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.discovery.settingsContinueCancelItem
import im.vector.app.features.discovery.settingsInformationItem
import im.vector.app.features.discovery.settingsSectionTitleItem
import im.vector.app.features.form.formEditTextItem
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

class ThreePidsSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<ThreePidsSettingsViewState>() {

    interface InteractionListener {
        fun addEmail()
        fun addMsisdn()
        fun cancelAdding()
        fun doAddEmail(email: String)
        fun doAddMsisdn(msisdn: String)
        fun continueThreePid(threePid: ThreePid)
        fun cancelThreePid(threePid: ThreePid)
        fun deleteThreePid(threePid: ThreePid)
    }

    var interactionListener: InteractionListener? = null

    private var currentInputValue = ""

    override fun buildModels(data: ThreePidsSettingsViewState?) {
        if (data == null) return

        if (data.state is ThreePidsSettingsState.Idle) {
            currentInputValue = ""
        }

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
                buildThreePids(dataList, data)
            }
        }
    }

    private fun buildThreePids(list: List<ThreePid>, data: ThreePidsSettingsViewState) {
        val splited = list.groupBy { it is ThreePid.Email }
        val emails = splited[true].orEmpty()
        val msisdn = splited[false].orEmpty()

        settingsSectionTitleItem {
            id("email")
            title(stringProvider.getString(R.string.settings_emails))
        }

        emails.forEach { buildThreePid("email ", it) }

        // Pending threePids
        data.pendingThreePids.invoke()
                ?.filterIsInstance(ThreePid.Email::class.java)
                ?.forEach { buildPendingThreePid("p_email ", it) }

        when (data.state) {
            ThreePidsSettingsState.Idle ->
                genericButtonItem {
                    id("addEmail")
                    text(stringProvider.getString(R.string.settings_add_email_address))
                    textColor(colorProvider.getColor(R.color.riotx_accent))
                    buttonClickAction(View.OnClickListener { interactionListener?.addEmail() })
                }
            is ThreePidsSettingsState.AddingEmail -> {
                formEditTextItem {
                    id("addingEmail")
                    inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    hint(stringProvider.getString(R.string.medium_email))
                    errorMessage(data.state.error)
                    onTextChange { currentInputValue = it }
                    showBottomSeparator(false)
                }
                settingsContinueCancelItem {
                    id("contAddingEmail")
                    continueOnClick { interactionListener?.doAddEmail(currentInputValue) }
                    cancelOnClick { interactionListener?.cancelAdding() }
                }
            }
            is ThreePidsSettingsState.AddingPhoneNumber -> Unit
        }.exhaustive

        settingsSectionTitleItem {
            id("msisdn")
            title(stringProvider.getString(R.string.settings_phone_numbers))
        }

        msisdn.forEach { buildThreePid("msisdn ", it) }

        // Pending threePids
        data.pendingThreePids.invoke()
                ?.filterIsInstance(ThreePid.Msisdn::class.java)
                ?.forEach { buildPendingThreePid("p_msisdn ", it) }

        when (data.state) {
            ThreePidsSettingsState.Idle ->
                genericButtonItem {
                    id("addMsisdn")
                    text(stringProvider.getString(R.string.settings_add_phone_number))
                    textColor(colorProvider.getColor(R.color.riotx_accent))
                    buttonClickAction(View.OnClickListener { interactionListener?.addMsisdn() })
                }
            is ThreePidsSettingsState.AddingEmail -> Unit
            is ThreePidsSettingsState.AddingPhoneNumber -> {
                formEditTextItem {
                    id("addingMsisdn")
                    inputType(InputType.TYPE_CLASS_PHONE)
                    hint(stringProvider.getString(R.string.medium_phone_number))
                    errorMessage(data.state.error)
                    onTextChange { currentInputValue = it }
                    showBottomSeparator(false)
                }
                settingsContinueCancelItem {
                    id("contAddingMsisdn")
                    continueOnClick { interactionListener?.doAddMsisdn(currentInputValue) }
                    cancelOnClick { interactionListener?.cancelAdding() }
                }
            }
        }.exhaustive
    }

    private fun buildThreePid(idPrefix: String, threePid: ThreePid) {
        threePidItem {
            id(idPrefix + threePid.value)
            // TODO Add an icon for emails
            // iconResId(if (threePid is ThreePid.Msisdn) R.drawable.ic_phone else null)
            title(threePid.getFormattedValue())
            deleteClickListener { interactionListener?.deleteThreePid(threePid) }
        }
    }

    private fun buildPendingThreePid(idPrefix: String, threePid: ThreePid) {
        threePidItem {
            id(idPrefix + threePid.value)
            // TODO Add an icon for emails
            // iconResId(if (threePid is ThreePid.Msisdn) R.drawable.ic_phone else null)
            title(threePid.getFormattedValue())
        }

        if (threePid is ThreePid.Email) {
            settingsInformationItem {
                id("info" + idPrefix + threePid.value)
                message(stringProvider.getString(R.string.account_email_validation_message))
                colorProvider(colorProvider)
            }
        }

        settingsContinueCancelItem {
            id("cont" + idPrefix + threePid.value)
            continueOnClick { interactionListener?.continueThreePid(threePid) }
            cancelOnClick { interactionListener?.cancelThreePid(threePid) }
        }
    }
}
