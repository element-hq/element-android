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
package im.vector.riotx.features.discovery

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.google.i18n.phonenumbers.PhoneNumberUtil
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.identity.SharedState
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import timber.log.Timber
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

class DiscoverySettingsController @Inject constructor(
        private val colorProvider: ColorProvider,
        private val stringProvider: StringProvider,
        private val errorFormatter: ErrorFormatter
) : TypedEpoxyController<DiscoverySettingsState>() {

    var listener: Listener? = null

    override fun buildModels(data: DiscoverySettingsState) {
        when (data.identityServer) {
            is Loading -> {
                loadingItem {
                    id("identityServerLoading")
                }
            }
            is Fail    -> {
                settingsInfoItem {
                    id("identityServerError")
                    helperText(data.identityServer.error.message)
                }
            }
            is Success -> {
                buildIdentityServerSection(data)

                val hasIdentityServer = data.identityServer().isNullOrBlank().not()

                if (hasIdentityServer) {
                    buildMailSection(data)
                    buildPhoneNumberSection(data)
                }
            }
        }
    }

    private fun buildPhoneNumberSection(data: DiscoverySettingsState) {
        settingsSectionTitle {
            id("msisdn")
            titleResId(R.string.settings_discovery_msisdn_title)
        }

        when (data.phoneNumbersList) {
            is Incomplete -> {
                loadingItem {
                    id("msisdnLoading")
                }
            }
            is Fail       -> {
                settingsInfoItem {
                    id("msisdnListError")
                    helperText(data.phoneNumbersList.error.message)
                }
            }
            is Success    -> {
                val phones = data.phoneNumbersList.invoke()
                if (phones.isEmpty()) {
                    settingsInfoItem {
                        id("no_msisdn")
                        helperText(stringProvider.getString(R.string.settings_discovery_no_msisdn))
                    }
                } else {
                    phones.forEach { piState ->
                        val phoneNumber = try {
                            PhoneNumberUtil.getInstance().parse("+${piState.threePid.value}", null)
                        } catch (t: Throwable) {
                            Timber.e(t, "Unable to parse the phone number")
                            null
                        }
                                ?.let {
                                    PhoneNumberUtil.getInstance().format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                                }

                        settingsTextButtonItem {
                            id(piState.threePid.value)
                            title(phoneNumber)
                            colorProvider(colorProvider)
                            stringProvider(stringProvider)
                            when (piState.isShared) {
                                is Loading -> {
                                    buttonIndeterminate(true)
                                }
                                is Fail    -> {
                                    buttonType(SettingsTextButtonItem.ButtonType.NORMAL)
                                    buttonStyle(SettingsTextButtonItem.ButtonStyle.DESTRUCTIVE)
                                    buttonTitle(stringProvider.getString(R.string.global_retry))
                                    infoMessage(piState.isShared.error.message)
                                    buttonClickListener { listener?.onTapRetryToRetrieveBindings() }
                                }
                                is Success -> when (piState.isShared()) {
                                    SharedState.SHARED,
                                    SharedState.NOT_SHARED          -> {
                                        checked(piState.isShared() == SharedState.SHARED)
                                        buttonType(SettingsTextButtonItem.ButtonType.SWITCH)
                                        switchChangeListener { _, checked ->
                                            if (checked) {
                                                listener?.onTapShare(piState.threePid)
                                            } else {
                                                listener?.onTapRevoke(piState.threePid)
                                            }
                                        }
                                    }
                                    SharedState.BINDING_IN_PROGRESS -> {
                                        buttonType(SettingsTextButtonItem.ButtonType.NORMAL)
                                        buttonTitle(null)
                                    }
                                }
                            }
                        }
                        when (piState.isShared()) {
                            SharedState.BINDING_IN_PROGRESS -> {
                                val errorText = if (piState.finalRequest is Fail) {
                                    val error = piState.finalRequest.error
                                    // Deal with error 500
                                    //Ref: https://github.com/matrix-org/sydent/issues/292
                                    if (error is Failure.ServerError
                                            && error.httpCode == HttpsURLConnection.HTTP_INTERNAL_ERROR /* 500 */) {
                                        stringProvider.getString(R.string.settings_text_message_sent_wrong_code)
                                    } else {
                                        errorFormatter.toHumanReadable(error)
                                    }
                                } else {
                                    null
                                }
                                settingsItemEditText {
                                    id("tverif" + piState.threePid.value)
                                    descriptionText(stringProvider.getString(R.string.settings_text_message_sent, phoneNumber))
                                    errorText(errorText)
                                    inProgress(piState.finalRequest is Loading)
                                    interactionListener(object : SettingsItemEditText.Listener {
                                        override fun onValidate(code: String) {
                                            if (piState.threePid is ThreePid.Msisdn) {
                                                listener?.sendMsisdnVerificationCode(piState.threePid, code)
                                            }
                                        }

                                        override fun onCancel() {
                                            listener?.cancelBinding(piState.threePid)
                                        }
                                    })
                                }
                            }
                            else                            -> Unit
                        }.exhaustive
                    }
                }
            }
        }
    }

    private fun buildMailSection(data: DiscoverySettingsState) {
        settingsSectionTitle {
            id("emails")
            titleResId(R.string.settings_discovery_emails_title)
        }
        when (data.emailList) {
            is Incomplete -> {
                loadingItem {
                    id("mailLoading")
                }
            }
            is Fail       -> {
                settingsInfoItem {
                    id("mailListError")
                    helperText(data.emailList.error.message)
                }
            }
            is Success    -> {
                val emails = data.emailList.invoke()
                if (emails.isEmpty()) {
                    settingsInfoItem {
                        id("no_emails")
                        helperText(stringProvider.getString(R.string.settings_discovery_no_mails))
                    }
                } else {
                    emails.forEach { piState ->
                        settingsTextButtonItem {
                            id(piState.threePid.value)
                            title(piState.threePid.value)
                            colorProvider(colorProvider)
                            stringProvider(stringProvider)
                            when (piState.isShared) {
                                is Loading -> {
                                    buttonIndeterminate(true)
                                    showBottomButtons(false)
                                }
                                is Fail    -> {
                                    buttonType(SettingsTextButtonItem.ButtonType.NORMAL)
                                    buttonStyle(SettingsTextButtonItem.ButtonStyle.DESTRUCTIVE)
                                    buttonTitle(stringProvider.getString(R.string.global_retry))
                                    infoMessage(piState.isShared.error.message)
                                    buttonClickListener { listener?.onTapRetryToRetrieveBindings() }
                                    showBottomButtons(false)
                                }
                                is Success -> when (piState.isShared()) {
                                    SharedState.SHARED,
                                    SharedState.NOT_SHARED          -> {
                                        checked(piState.isShared() == SharedState.SHARED)
                                        buttonType(SettingsTextButtonItem.ButtonType.SWITCH)
                                        switchChangeListener { _, checked ->
                                            if (checked) {
                                                listener?.onTapShare(piState.threePid)
                                            } else {
                                                listener?.onTapRevoke(piState.threePid)
                                            }
                                        }
                                    }
                                    SharedState.BINDING_IN_PROGRESS -> {
                                        buttonType(SettingsTextButtonItem.ButtonType.NORMAL)
                                        buttonTitle(null)
                                        showBottomButtons(true)
                                        when (piState.finalRequest) {
                                            is Uninitialized -> {
                                                infoMessage(stringProvider.getString(R.string.settings_discovery_confirm_mail, piState.threePid.value))
                                                infoMessageTintColorId(R.color.vector_info_color)
                                                showBottomLoading(false)
                                            }
                                            is Loading       -> {
                                                infoMessage(stringProvider.getString(R.string.settings_discovery_confirm_mail, piState.threePid.value))
                                                infoMessageTintColorId(R.color.vector_info_color)
                                                showBottomLoading(true)
                                            }
                                            is Fail          -> {
                                                infoMessage(stringProvider.getString(R.string.settings_discovery_confirm_mail_not_clicked, piState.threePid.value))
                                                infoMessageTintColorId(R.color.riotx_destructive_accent)
                                                showBottomLoading(false)
                                            }
                                            is Success       -> Unit /* Cannot happen */
                                        }
                                        cancelClickListener { listener?.cancelBinding(piState.threePid) }
                                        continueClickListener {
                                            if (piState.threePid is ThreePid.Email) {
                                                listener?.checkEmailVerification(piState.threePid)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildIdentityServerSection(data: DiscoverySettingsState) {
        val identityServer = data.identityServer() ?: stringProvider.getString(R.string.none)

        settingsSectionTitle {
            id("idsTitle")
            titleResId(R.string.identity_server)
        }

        settingsItem {
            id("idServer")
            description(identityServer)
        }

        settingsInfoItem {
            id("idServerFooter")
            if (data.termsNotSigned) {
                helperText(stringProvider.getString(R.string.settings_agree_to_terms, identityServer))
                showCompoundDrawable(true)
                itemClickListener(View.OnClickListener { listener?.onSelectIdentityServer() })
            } else {
                showCompoundDrawable(false)
                if (data.identityServer() != null) {
                    helperText(stringProvider.getString(R.string.settings_discovery_identity_server_info, identityServer))
                } else {
                    helperTextResId(R.string.settings_discovery_identity_server_info_none)
                }
            }
        }

        settingsButtonItem {
            id("change")
            colorProvider(colorProvider)
            if (data.identityServer() != null) {
                buttonTitleId(R.string.change_identity_server)
            } else {
                buttonTitleId(R.string.add_identity_server)
            }
            buttonStyle(SettingsTextButtonItem.ButtonStyle.POSITIVE)
            buttonClickListener(View.OnClickListener {
                listener?.onTapChangeIdentityServer()
            })
        }

        if (data.identityServer() != null) {
            settingsInfoItem {
                id("removeInfo")
                helperTextResId(R.string.settings_discovery_disconnect_identity_server_info)
            }
            settingsButtonItem {
                id("remove")
                colorProvider(colorProvider)
                buttonTitleId(R.string.disconnect_identity_server)
                buttonStyle(SettingsTextButtonItem.ButtonStyle.DESTRUCTIVE)
                buttonClickListener(View.OnClickListener {
                    listener?.onTapDisconnectIdentityServer()
                })
            }
        }
    }

    interface Listener {
        fun onSelectIdentityServer()
        fun onTapRevoke(threePid: ThreePid)
        fun onTapShare(threePid: ThreePid)
        fun checkEmailVerification(threePid: ThreePid.Email)
        fun cancelBinding(threePid: ThreePid)
        fun sendMsisdnVerificationCode(threePid: ThreePid.Msisdn, code: String)
        fun onTapChangeIdentityServer()
        fun onTapDisconnectIdentityServer()
        fun onTapRetryToRetrieveBindings()
    }
}

