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

package im.vector.riotx.features.settings.devtools

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.core.ui.list.genericItemHeader
import me.gujun.android.span.span
import javax.inject.Inject

class KeyRequestEpoxyController @Inject constructor(
        private val stringProvider: StringProvider
) : TypedEpoxyController<KeyRequestListViewState>() {

    interface InteractionListener {
        //fun didTap(data: UserAccountData)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: KeyRequestListViewState?) {
        data?.outgoingRoomKeyRequest?.let { async ->
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
                                            span("sessionId:") {
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
