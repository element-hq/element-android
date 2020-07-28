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
package im.vector.riotx.features.home.room.detail.timeline.action

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Success
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.bottomsheet.BottomSheetQuickReactionsItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetMessagePreviewItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetQuickReactionsItem
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetSendStateItem
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.item.E2EDecoration
import im.vector.riotx.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.riotx.features.home.room.detail.timeline.tools.linkify
import javax.inject.Inject

/**
 * Epoxy controller for message action list
 */
class MessageActionsEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val fontProvider: EmojiCompatFontProvider
) : TypedEpoxyController<MessageActionState>() {

    var listener: MessageActionsEpoxyControllerListener? = null

    override fun buildModels(state: MessageActionState) {
        // Message preview
        bottomSheetMessagePreviewItem {
            id("preview")
            avatarRenderer(avatarRenderer)
            matrixItem(state.informationData.matrixItem)
            movementMethod(createLinkMovementMethod(listener))
            userClicked { listener?.didSelectMenuAction(EventSharedAction.OpenUserProfile(state.informationData.senderId)) }
            body(state.messageBody.linkify(listener))
            time(state.time())
        }

        // Send state
        if (state.informationData.sendState.isSending()) {
            bottomSheetSendStateItem {
                id("send_state")
                showProgress(true)
                text(stringProvider.getString(R.string.event_status_sending_message))
            }
        } else if (state.informationData.sendState.hasFailed()) {
            bottomSheetSendStateItem {
                id("send_state")
                showProgress(false)
                text(stringProvider.getString(R.string.unable_to_send_message))
                drawableStart(R.drawable.ic_warning_badge)
            }
        }

        when (state.informationData.e2eDecoration) {
            E2EDecoration.WARN_IN_CLEAR        -> {
                bottomSheetSendStateItem {
                    id("e2e_clear")
                    showProgress(false)
                    text(stringProvider.getString(R.string.unencrypted))
                    drawableStart(R.drawable.ic_shield_warning_small)
                }
            }
            E2EDecoration.WARN_SENT_BY_UNVERIFIED,
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                bottomSheetSendStateItem {
                    id("e2e_unverified")
                    showProgress(false)
                    text(stringProvider.getString(R.string.encrypted_unverified))
                    drawableStart(R.drawable.ic_shield_warning_small)
                }
            }
            else                               -> {
                // nothing
            }
        }

        // Quick reactions
        if (state.canReact() && state.quickStates is Success) {
            // Separator
            dividerItem {
                id("reaction_separator")
            }

            bottomSheetQuickReactionsItem {
                id("quick_reaction")
                fontProvider(fontProvider)
                texts(state.quickStates()?.map { it.reaction }.orEmpty())
                selecteds(state.quickStates.invoke().map { it.isSelected })
                listener(object : BottomSheetQuickReactionsItem.Listener {
                    override fun didSelect(emoji: String, selected: Boolean) {
                        listener?.didSelectMenuAction(EventSharedAction.QuickReact(state.eventId, emoji, selected))
                    }
                })
            }
        }

        // Separator
        dividerItem {
            id("actions_separator")
        }

        // Action
        state.actions.forEachIndexed { index, action ->
            if (action is EventSharedAction.Separator) {
                dividerItem {
                    id("separator_$index")
                }
            } else {
                bottomSheetActionItem {
                    id("action_$index")
                    iconRes(action.iconResId)
                    textRes(action.titleRes)
                    showExpand(action is EventSharedAction.ReportContent)
                    expanded(state.expendedReportContentMenu)
                    listener(View.OnClickListener { listener?.didSelectMenuAction(action) })
                    destructive(action.destructive)
                }

                if (action is EventSharedAction.ReportContent && state.expendedReportContentMenu) {
                    // Special case for report content menu: add the submenu
                    listOf(
                            EventSharedAction.ReportContentSpam(action.eventId, action.senderId),
                            EventSharedAction.ReportContentInappropriate(action.eventId, action.senderId),
                            EventSharedAction.ReportContentCustom(action.eventId, action.senderId)
                    ).forEachIndexed { indexReport, actionReport ->
                        bottomSheetActionItem {
                            id("actionReport_$indexReport")
                            subMenuItem(true)
                            iconRes(actionReport.iconResId)
                            textRes(actionReport.titleRes)
                            listener(View.OnClickListener { listener?.didSelectMenuAction(actionReport) })
                        }
                    }
                }
            }
        }
    }

    interface MessageActionsEpoxyControllerListener : TimelineEventController.UrlClickCallback {
        fun didSelectMenuAction(eventAction: EventSharedAction)
    }
}
