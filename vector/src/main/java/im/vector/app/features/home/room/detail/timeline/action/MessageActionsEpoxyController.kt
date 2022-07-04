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
package im.vector.app.features.home.room.detail.timeline.action

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Success
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.bottomsheet.BottomSheetQuickReactionsItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetActionItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetMessagePreviewItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetQuickReactionsItem
import im.vector.app.core.epoxy.bottomsheet.bottomSheetSendStateItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.format.EventDetailsFormatter
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.home.room.detail.timeline.image.buildImageContentRendererData
import im.vector.app.features.home.room.detail.timeline.item.E2EDecoration
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.app.features.home.room.detail.timeline.tools.linkify
import im.vector.app.features.html.SpanUtils
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.toLocationData
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.events.model.isLocationMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageLocationContent
import org.matrix.android.sdk.api.session.room.send.SendState
import javax.inject.Inject

/**
 * Epoxy controller for message action list.
 */
class MessageActionsEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val avatarRenderer: AvatarRenderer,
        private val fontProvider: EmojiCompatFontProvider,
        private val imageContentRenderer: ImageContentRenderer,
        private val dimensionConverter: DimensionConverter,
        private val errorFormatter: ErrorFormatter,
        private val spanUtils: SpanUtils,
        private val eventDetailsFormatter: EventDetailsFormatter,
        private val vectorPreferences: VectorPreferences,
        private val dateFormatter: VectorDateFormatter,
        private val urlMapProvider: UrlMapProvider,
        private val locationPinProvider: LocationPinProvider
) : TypedEpoxyController<MessageActionState>() {

    var listener: MessageActionsEpoxyControllerListener? = null

    override fun buildModels(state: MessageActionState) {
        val host = this
        // Message preview
        val date = state.timelineEvent()?.root?.originServerTs
        val formattedDate = dateFormatter.format(date, DateFormatKind.MESSAGE_DETAIL)
        val body = state.messageBody.linkify(host.listener)
        val bindingOptions = spanUtils.getBindingOptions(body)
        val locationUiData = buildLocationUiData(state)

        bottomSheetMessagePreviewItem {
            id("preview")
            avatarRenderer(host.avatarRenderer)
            matrixItem(state.informationData.matrixItem)
            movementMethod(createLinkMovementMethod(host.listener))
            imageContentRenderer(host.imageContentRenderer)
            data(state.timelineEvent()?.buildImageContentRendererData(host.dimensionConverter.dpToPx(66)))
            userClicked { host.listener?.didSelectMenuAction(EventSharedAction.OpenUserProfile(state.informationData.senderId)) }
            bindingOptions(bindingOptions)
            body(body.toEpoxyCharSequence())
            bodyDetails(host.eventDetailsFormatter.format(state.timelineEvent()?.root)?.toEpoxyCharSequence())
            time(formattedDate)
            locationUiData(locationUiData)
        }

        // Send state
        val sendState = state.sendState()
        if (sendState?.hasFailed().orFalse()) {
            // Get more details about the error
            val errorMessage = state.timelineEvent()?.root?.sendStateError()
                    ?.let { errorFormatter.toHumanReadable(Failure.ServerError(it, 0)) }
                    ?: stringProvider.getString(R.string.unable_to_send_message)
            bottomSheetSendStateItem {
                id("send_state")
                showProgress(false)
                text(errorMessage)
                drawableStart(R.drawable.ic_warning_badge)
            }
        } else if (sendState?.isSending().orFalse()) {
            bottomSheetSendStateItem {
                id("send_state")
                showProgress(true)
                text(host.stringProvider.getString(R.string.event_status_sending_message))
            }
        } else if (sendState == SendState.SENT) {
            bottomSheetSendStateItem {
                id("send_state")
                showProgress(false)
                drawableStart(R.drawable.ic_message_sent)
                text(host.stringProvider.getString(R.string.event_status_sent_message))
            }
        }

        when (state.informationData.e2eDecoration) {
            E2EDecoration.WARN_IN_CLEAR -> {
                bottomSheetSendStateItem {
                    id("e2e_clear")
                    showProgress(false)
                    text(host.stringProvider.getString(R.string.unencrypted))
                    drawableStart(R.drawable.ic_shield_warning_small)
                }
            }
            E2EDecoration.WARN_SENT_BY_UNVERIFIED,
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                bottomSheetSendStateItem {
                    id("e2e_unverified")
                    showProgress(false)
                    text(host.stringProvider.getString(R.string.encrypted_unverified))
                    drawableStart(R.drawable.ic_shield_warning_small)
                }
            }
            else -> {
                // nothing
            }
        }

        // Quick reactions
        if (state.canReact() && state.quickStates is Success) {
            // Separator
            bottomSheetDividerItem {
                id("reaction_separator")
            }

            bottomSheetQuickReactionsItem {
                id("quick_reaction")
                fontProvider(host.fontProvider)
                texts(state.quickStates()?.map { it.reaction }.orEmpty())
                selecteds(state.quickStates.invoke().map { it.isSelected })
                listener(object : BottomSheetQuickReactionsItem.Listener {
                    override fun didSelect(emoji: String, selected: Boolean) {
                        host.listener?.didSelectMenuAction(EventSharedAction.QuickReact(state.eventId, emoji, selected))
                    }
                })
            }
        }

        if (state.actions.isNotEmpty()) {
            // Separator
            bottomSheetDividerItem {
                id("actions_separator")
            }
        }

        // Action
        state.actions.forEachIndexed { index, action ->
            if (action is EventSharedAction.Separator) {
                bottomSheetDividerItem {
                    id("separator_$index")
                }
            } else {
                val showBetaLabel = action.shouldShowBetaLabel()

                bottomSheetActionItem {
                    id("action_$index")
                    iconRes(action.iconResId)
                    textRes(action.titleRes)
                    showExpand(action is EventSharedAction.ReportContent)
                    expanded(state.expendedReportContentMenu)
                    listener { host.listener?.didSelectMenuAction(action) }
                    destructive(action.destructive)
                    showBetaLabel(showBetaLabel)
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
                            listener { host.listener?.didSelectMenuAction(actionReport) }
                        }
                    }
                }
            }
        }
    }

    private fun buildLocationUiData(state: MessageActionState): LocationUiData? {
        if (state.timelineEvent()?.root?.isLocationMessage() != true) return null

        val locationContent = state.timelineEvent()?.root?.getClearContent().toModel<MessageLocationContent>(catchError = true)
                ?: return null
        val locationUrl = locationContent.toLocationData()
                ?.let { urlMapProvider.buildStaticMapUrl(it, INITIAL_MAP_ZOOM_IN_TIMELINE, 1200, 800) }
                ?: return null
        val locationOwnerId = if (locationContent.isSelfLocation()) state.informationData.matrixItem.id else null

        return LocationUiData(
                locationUrl = locationUrl,
                locationOwnerId = locationOwnerId,
                locationPinProvider = locationPinProvider,
        )
    }

    private fun EventSharedAction.shouldShowBetaLabel(): Boolean =
            this is EventSharedAction.ReplyInThread && !vectorPreferences.areThreadMessagesEnabled()

    interface MessageActionsEpoxyControllerListener : TimelineEventController.UrlClickCallback {
        fun didSelectMenuAction(eventAction: EventSharedAction)
    }
}
