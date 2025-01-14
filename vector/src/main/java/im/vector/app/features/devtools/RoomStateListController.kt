/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.devtools

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.json.JSONObject
import javax.inject.Inject

class RoomStateListController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<RoomDevToolViewState>() {

    var interactionListener: DevToolsInteractionListener? = null

    override fun buildModels(data: RoomDevToolViewState?) {
        val host = this
        when (data?.displayMode) {
            RoomDevToolViewState.Mode.StateEventList -> {
                val stateEventsGroups = data.stateEvents.invoke().orEmpty().groupBy { it.getClearType() }

                if (stateEventsGroups.isEmpty()) {
                    noResultItem {
                        id("no state events")
                        text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
                    }
                } else {
                    stateEventsGroups.forEach { entry ->
                        genericItem {
                            id(entry.key)
                            title(entry.key.toEpoxyCharSequence())
                            description(host.stringProvider.getQuantityString(CommonPlurals.entries, entry.value.size, entry.value.size).toEpoxyCharSequence())
                            itemClickAction {
                                host.interactionListener?.processAction(RoomDevToolAction.ShowStateEventType(entry.key))
                            }
                        }
                    }
                }
            }
            RoomDevToolViewState.Mode.StateEventListByType -> {
                val stateEvents = data.stateEvents.invoke().orEmpty().filter { it.type == data.currentStateType }
                if (stateEvents.isEmpty()) {
                    noResultItem {
                        id("no state events")
                        text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
                    }
                } else {
                    stateEvents.forEach { stateEvent ->
                        val contentJson = JSONObject(stateEvent.content.orEmpty()).toString().let {
                            if (it.length > 140) {
                                it.take(140) + Typography.ellipsis
                            } else {
                                it.take(140)
                            }
                        }
                        genericItem {
                            id(stateEvent.eventId)
                            title(span {
                                +"Type: "
                                span {
                                    textColor = host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                                    text = "\"${stateEvent.type}\""
                                    textStyle = "normal"
                                }
                                +"\nState Key: "
                                span {
                                    textColor = host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                                    text = stateEvent.stateKey.let { "\"$it\"" }
                                    textStyle = "normal"
                                }
                            }.toEpoxyCharSequence())
                            description(contentJson.toEpoxyCharSequence())
                            itemClickAction {
                                host.interactionListener?.processAction(RoomDevToolAction.ShowStateEvent(stateEvent))
                            }
                        }
                    }
                }
            }
            else -> {
                // nop
            }
        }
    }
}
