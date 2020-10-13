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

package im.vector.app.features.settings.devtools

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.GenericItem
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericItemHeader
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.model.event.OlmEventContent
import org.matrix.android.sdk.internal.crypto.model.event.SecretSendEventContent
import org.matrix.android.sdk.internal.crypto.model.rest.ForwardedRoomKeyContent
import org.matrix.android.sdk.internal.crypto.model.rest.GossipingToDeviceObject
import org.matrix.android.sdk.internal.crypto.model.rest.RoomKeyShareRequest
import org.matrix.android.sdk.internal.crypto.model.rest.SecretShareRequest
import javax.inject.Inject

class GossipingEventsEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val vectorDateFormatter: VectorDateFormatter,
        private val colorProvider: ColorProvider
) : TypedEpoxyController<GossipingEventsPaperTrailState>() {

    interface InteractionListener {
        fun didTap(event: Event)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: GossipingEventsPaperTrailState?) {
        when (val async = data?.events) {
            is Uninitialized,
            is Loading -> {
                loadingItem {
                    id("loadingOutgoing")
                    loadingText(stringProvider.getString(R.string.loading))
                }
            }
            is Fail    -> {
                genericItem {
                    id("failOutgoing")
                    title(async.error.localizedMessage)
                }
            }
            is Success -> {
                val eventList = async.invoke()
                if (eventList.isEmpty()) {
                    genericFooterItem {
                        id("empty")
                        text(stringProvider.getString(R.string.no_result_placeholder))
                    }
                    return
                }

                eventList.forEachIndexed { _, event ->
                    genericItem {
                        id(event.hashCode())
                        itemClickAction(GenericItem.Action("view").apply { perform = Runnable { interactionListener?.didTap(event) } })
                        title(
                                if (event.isEncrypted()) {
                                    "${event.getClearType()} [encrypted]"
                                } else {
                                    event.type
                                }
                        )
                        description(
                                span {
                                    +vectorDateFormatter.format(event.ageLocalTs, DateFormatKind.DEFAULT_DATE_AND_TIME)
                                    span("\nfrom: ") {
                                        textStyle = "bold"
                                    }
                                    +"${event.senderId}"
                                    apply {
                                        if (event.getClearType() == EventType.ROOM_KEY_REQUEST) {
                                            val content = event.getClearContent().toModel<RoomKeyShareRequest>()
                                            span("\nreqId:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.requestId}"
                                            span("\naction:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.action}"
                                            if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                                                span("\nsessionId:") {
                                                    textStyle = "bold"
                                                }
                                                +" ${content.body?.sessionId}"
                                            }
                                            span("\nrequestedBy: ") {
                                                textStyle = "bold"
                                            }
                                            +"${content?.requestingDeviceId}"
                                        } else if (event.getClearType() == EventType.FORWARDED_ROOM_KEY) {
                                            val encryptedContent = event.content.toModel<OlmEventContent>()
                                            val content = event.getClearContent().toModel<ForwardedRoomKeyContent>()
                                            if (event.mxDecryptionResult == null) {
                                                span("**Failed to Decrypt** ${event.mCryptoError}") {
                                                    textColor = colorProvider.getColor(R.color.vector_error_color)
                                                }
                                            }
                                            span("\nsessionId:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.sessionId}"
                                            span("\nFrom Device (sender key):") {
                                                textStyle = "bold"
                                            }
                                            +" ${encryptedContent?.senderKey}"
                                        } else if (event.getClearType() == EventType.SEND_SECRET) {
                                            val content = event.getClearContent().toModel<SecretSendEventContent>()

                                            span("\nrequestId:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.requestId}"
                                            span("\nFrom Device:") {
                                                textStyle = "bold"
                                            }
                                            +" ${event.mxDecryptionResult?.payload?.get("sender_device")}"
                                        } else if (event.getClearType() == EventType.REQUEST_SECRET) {
                                            val content = event.getClearContent().toModel<SecretShareRequest>()
                                            span("\nreqId:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.requestId}"
                                            span("\naction:") {
                                                textStyle = "bold"
                                            }
                                            +" ${content?.action}"
                                            if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                                                span("\nsecretName:") {
                                                    textStyle = "bold"
                                                }
                                                +" ${content.secretName}"
                                            }
                                            span("\nrequestedBy: ") {
                                                textStyle = "bold"
                                            }
                                            +"${content?.requestingDeviceId}"
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    private fun buildOutgoing(data: KeyRequestListViewState?) {
        data?.outgoingRoomKeyRequests?.let { async ->
            when (async) {
                is Uninitialized,
                is Loading -> {
                    loadingItem {
                        id("loadingOutgoing")
                        loadingText(stringProvider.getString(R.string.loading))
                    }
                }
                is Fail    -> {
                    genericItem {
                        id("failOutgoing")
                        title(async.error.localizedMessage)
                    }
                }
                is Success -> {
                    if (async.invoke().isEmpty()) {
                        genericFooterItem {
                            id("empty")
                            text(stringProvider.getString(R.string.no_result_placeholder))
                        }
                        return
                    }

                    val requestList = async.invoke().groupBy { it.roomId }

                    requestList.forEach {
                        genericItemHeader {
                            id(it.key)
                            text("roomId: ${it.key}")
                        }
                        it.value.forEach { roomKeyRequest ->
                            genericItem {
                                id(roomKeyRequest.requestId)
                                title(roomKeyRequest.requestId)
                                description(
                                        span {
                                            span("sessionId:\n") {
                                                textStyle = "bold"
                                            }
                                            +"${roomKeyRequest.sessionId}"
                                            span("\nstate:") {
                                                textStyle = "bold"
                                            }
                                            +"\n${roomKeyRequest.state.name}"
                                        }
                                )
                            }
                        }
                    }
                }
            }.exhaustive
        }
    }
}
