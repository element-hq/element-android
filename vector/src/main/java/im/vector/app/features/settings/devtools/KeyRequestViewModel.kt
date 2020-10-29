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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.MvRxViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.core.platform.VectorViewModelAction
import im.vector.app.core.resources.DateProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.model.event.OlmEventContent
import org.matrix.android.sdk.internal.crypto.model.event.SecretSendEventContent
import org.matrix.android.sdk.internal.crypto.model.rest.ForwardedRoomKeyContent
import org.matrix.android.sdk.internal.crypto.model.rest.GossipingToDeviceObject
import org.matrix.android.sdk.internal.crypto.model.rest.RoomKeyShareRequest
import org.matrix.android.sdk.internal.crypto.model.rest.SecretShareRequest
import org.threeten.bp.format.DateTimeFormatter

sealed class KeyRequestAction : VectorViewModelAction {
    data class ExportAudit(val uri: Uri) : KeyRequestAction()
}

sealed class KeyRequestEvents : VectorViewEvents {
    data class SaveAudit(val uri: Uri, val raw: String) : KeyRequestEvents()
}

data class KeyRequestViewState(
        val exporting: Async<Unit> = Uninitialized
) : MvRxState

class KeyRequestViewModel @AssistedInject constructor(
        @Assisted initialState: KeyRequestViewState,
        private val session: Session)
    : VectorViewModel<KeyRequestViewState, KeyRequestAction, KeyRequestEvents>(initialState) {

    @AssistedInject.Factory
    interface Factory {
        fun create(initialState: KeyRequestViewState): KeyRequestViewModel
    }

    private val full24DateFormatter by lazy {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }

    companion object : MvRxViewModelFactory<KeyRequestViewModel, KeyRequestViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: KeyRequestViewState): KeyRequestViewModel? {
            val fragment: KeyRequestsFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.viewModelFactory.create(state)
        }
    }

    override fun handle(action: KeyRequestAction) {
        when (action) {
            is KeyRequestAction.ExportAudit -> exportAudit(action)
        }.exhaustive
    }

    private fun exportAudit(action: KeyRequestAction.ExportAudit) {
        setState {
            copy(exporting = Loading())
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // this can take long
                val eventList = session.cryptoService().getGossipingEvents()
                // clean it a bit to
                val raw = buildString {
                    eventList.forEach {
                        val clearType = it.getClearType()
                        append("[${getFormattedDate(it.ageLocalTs)}] $clearType from:${it.senderId} - ")
                        when (clearType) {
                            EventType.ROOM_KEY_REQUEST   -> {
                                val content = it.getClearContent().toModel<RoomKeyShareRequest>()
                                append("reqId:${content?.requestId} action:${content?.action} ")
                                if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                                    append("sessionId: ${content.body?.sessionId} ")
                                }
                                append("requestedBy: ${content?.requestingDeviceId}")
                            }
                            EventType.FORWARDED_ROOM_KEY -> {
                                val encryptedContent = it.content.toModel<OlmEventContent>()
                                val content = it.getClearContent().toModel<ForwardedRoomKeyContent>()

                                append("sessionId:${content?.sessionId} From Device (sender key):${encryptedContent?.senderKey}")
                                span("\nFrom Device (sender key):") {
                                    textStyle = "bold"
                                }
                            }
                            EventType.ROOM_KEY           -> {
                                val content = it.getClearContent()
                                append("sessionId:${content?.get("session_id")} roomId:${content?.get("room_id")} dest:${content?.get("_dest") ?: "me"}")
                            }
                            EventType.SEND_SECRET        -> {
                                val content = it.getClearContent().toModel<SecretSendEventContent>()
                                append("requestId:${content?.requestId} From Device:${it.mxDecryptionResult?.payload?.get("sender_device")}")
                            }
                            EventType.REQUEST_SECRET     -> {
                                val content = it.getClearContent().toModel<SecretShareRequest>()
                                append("reqId:${content?.requestId} action:${content?.action} ")
                                if (content?.action == GossipingToDeviceObject.ACTION_SHARE_REQUEST) {
                                    append("secretName:${content.secretName} ")
                                }
                                append("requestedBy:${content?.requestingDeviceId}")
                            }
                            EventType.ENCRYPTED          -> {
                                append("Failed to Decrypt")
                            }
                            else                         -> {
                                append("??")
                            }
                        }
                        append("\n")
                    }
                }
                setState {
                    copy(exporting = Success(Unit))
                }
                _viewEvents.post(KeyRequestEvents.SaveAudit(action.uri, raw))
            } catch (error: Throwable) {
                setState {
                    copy(exporting = Fail(error))
                }
            }
        }
    }

    private fun getFormattedDate(ageLocalTs: Long?): String {
        return ageLocalTs
                ?.let { DateProvider.toLocalDateTime(it) }
                ?.let { full24DateFormatter.format(it) }
                ?: "?"
    }
}
