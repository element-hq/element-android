/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.create

import android.text.InputType
import com.airbnb.epoxy.TypedEpoxyController
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericPillItem
import im.vector.app.features.form.formEditTextItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class SpaceAdd3pidEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<CreateSpaceState>() {

    var listener: Listener? = null

    override fun buildModels(data: CreateSpaceState?) {
        val host = this
        data ?: return
        genericFooterItem {
            id("info_help_header")
            style(ItemStyle.TITLE)
            text(host.stringProvider.getString(CommonStrings.create_spaces_invite_public_header).toEpoxyCharSequence())
            textColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        }
        genericFooterItem {
            id("info_help_desc")
            text(host.stringProvider.getString(CommonStrings.create_spaces_invite_public_header_desc, data.name ?: "").toEpoxyCharSequence())
            textColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary))
        }

        if (data.canInviteByMail) {
            buildEmailFields(data, host)
        } else {
            genericPillItem {
                id("no_IDS")
                imageRes(im.vector.app.R.drawable.ic_baseline_perm_contact_calendar_24)
                text(host.stringProvider.getString(CommonStrings.create_space_identity_server_info_none).toEpoxyCharSequence())
            }
            genericButtonItem {
                id("Discover_Settings")
                text(host.stringProvider.getString(CommonStrings.open_discovery_settings))
                textColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                buttonClickAction {
                    host.listener?.onNoIdentityServer()
                }
            }
        }
    }

    private fun buildEmailFields(data: CreateSpaceState, host: SpaceAdd3pidEpoxyController) {
        for (index in 0..2) {
            val mail = data.default3pidInvite?.get(index)
            formEditTextItem {
                id("3pid$index")
                enabled(true)
                value(mail)
                hint(host.stringProvider.getString(CommonStrings.medium_email))
                inputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                endIconMode(TextInputLayout.END_ICON_CLEAR_TEXT)
                errorMessage(
                        if (data.emailValidationResult?.get(index) == false) {
                            host.stringProvider.getString(CommonStrings.does_not_look_like_valid_email)
                        } else null
                )
                onTextChange { text ->
                    host.listener?.on3pidChange(index, text)
                }
            }
        }
    }

    interface Listener {
        fun on3pidChange(index: Int, newName: String)
        fun onNoIdentityServer()
    }
}
