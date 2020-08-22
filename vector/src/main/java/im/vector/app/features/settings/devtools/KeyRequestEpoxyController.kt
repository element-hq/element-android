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
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericItemHeader
import me.gujun.android.span.span
import javax.inject.Inject

class KeyRequestEpoxyController @Inject constructor(
        private val stringProvider: StringProvider
) : TypedEpoxyController<KeyRequestListViewState>() {

    interface InteractionListener {
        // fun didTap(data: UserAccountData)
    }

    var outgoing = true

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: KeyRequestListViewState?) {
        if (outgoing) {
            buildOutgoing(data)
        } else {
            buildIncoming(data)
        }
    }

    private fun buildIncoming(data: KeyRequestListViewState?) {
        data?.incomingRequests?.let { async ->
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
                    val requestList = async.invoke().groupBy { it.userId }

                    requestList.forEach {
                        genericItemHeader {
                            id(it.key)
                            text("From user: ${it.key}")
                        }
                        it.value.forEach { roomKeyRequest ->
                            genericItem {
                                id(roomKeyRequest.requestId)
                                title(roomKeyRequest.requestId)
                                description(
                                        span {
                                            span("sessionId:") {
                                                textStyle = "bold"
                                            }
                                            span("\nFrom device:") {
                                                textStyle = "bold"
                                            }
                                            +"${roomKeyRequest.deviceId}"
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
