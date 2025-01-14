/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.joinrule

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericButtonItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.settings.joinrule.advanced.RoomJoinRuleChooseRestrictedState
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import timber.log.Timber
import javax.inject.Inject

class RoomJoinRuleAdvancedController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val avatarRenderer: AvatarRenderer
) : TypedEpoxyController<RoomJoinRuleChooseRestrictedState>() {

    interface InteractionListener {
        fun didSelectRule(rules: RoomJoinRules)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(state: RoomJoinRuleChooseRestrictedState?) {
        state ?: return
        val choices = state.choices ?: return

        val host = this

        genericFooterItem {
            id("header")
            text(host.stringProvider.getString(CommonStrings.room_settings_room_access_title).toEpoxyCharSequence())
            centered(false)
            style(ItemStyle.TITLE)
            textColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
        }

        genericFooterItem {
            id("desc")
            text(host.stringProvider.getString(CommonStrings.decide_who_can_find_and_join).toEpoxyCharSequence())
            centered(false)
        }

        // invite only
        RoomJoinRuleRadioAction(
                roomJoinRule = RoomJoinRules.INVITE,
                description = stringProvider.getString(CommonStrings.room_settings_room_access_private_description),
                title = stringProvider.getString(CommonStrings.room_settings_room_access_private_invite_only_title),
                isSelected = state.currentRoomJoinRules == RoomJoinRules.INVITE
        ).toRadioBottomSheetItem().let {
            it.listener {
                interactionListener?.didSelectRule(RoomJoinRules.INVITE)
//                listener?.didSelectAction(action)
            }
            add(it)
        }

        if (choices.firstOrNull { it.rule == RoomJoinRules.RESTRICTED } != null) {
            val restrictedRule = choices.first { it.rule == RoomJoinRules.RESTRICTED }
            Timber.w("##@@ ${state.updatedAllowList}")
            spaceJoinRuleItem {
                id("restricted")
                avatarRenderer(host.avatarRenderer)
                needUpgrade(restrictedRule.needUpgrade)
                selected(state.currentRoomJoinRules == RoomJoinRules.RESTRICTED)
                restrictedList(state.updatedAllowList)
                listener { host.interactionListener?.didSelectRule(RoomJoinRules.RESTRICTED) }
            }
        }

        // Public
        RoomJoinRuleRadioAction(
                roomJoinRule = RoomJoinRules.PUBLIC,
                description = stringProvider.getString(CommonStrings.room_settings_room_access_public_description),
                title = stringProvider.getString(CommonStrings.room_settings_room_access_public_title),
                isSelected = state.currentRoomJoinRules == RoomJoinRules.PUBLIC
        ).toRadioBottomSheetItem().let {
            it.listener {
                interactionListener?.didSelectRule(RoomJoinRules.PUBLIC)
            }
            add(it)
        }

        genericButtonItem {
            id("save")
            text("")
        }
    }
}
