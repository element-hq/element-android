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

package im.vector.riotredesign.features.home.room.detail.timeline.factory

import android.view.View
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.core.utils.DebouncedClickListener
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageTextItem_
import im.vector.riotredesign.features.home.room.detail.timeline.util.MessageInformationDataFactory
import me.gujun.android.span.span

// This class handles timeline events who haven't been successfully decrypted
class EncryptedItemFactory(private val messageInformationDataFactory: MessageInformationDataFactory,
                           private val colorProvider: ColorProvider,
                           private val stringProvider: StringProvider) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {
        event.root.eventId ?: return null

        return when {
            EventType.ENCRYPTED == event.root.getClearType() -> {
                val cryptoError = event.root.mCryptoError
                val errorDescription =
                        if (cryptoError?.code == MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE) {
                            stringProvider.getString(R.string.notice_crypto_error_unkwown_inbound_session_id)
                        } else {
                            cryptoError?.message
                        }

                val message = stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, errorDescription)
                val spannableStr = span(message) {
                    textStyle = "italic"
                    textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                }

                // TODO This is not correct format for error, change it
                val informationData = messageInformationDataFactory.create(event, nextEvent)

                return MessageTextItem_()
                        .message(spannableStr)
                        .informationData(informationData)
                        .highlighted(highlight)
                        .avatarCallback(callback)
                        .urlClickCallback(callback)
                        .cellClickListener(
                                DebouncedClickListener(View.OnClickListener { view ->
                                    callback?.onEncryptedMessageClicked(informationData, view)
                                }))
                        .longClickListener { view ->
                            return@longClickListener callback?.onEventLongClicked(informationData, null, view)
                                    ?: false
                        }
            }
            else                                             -> null
        }
    }
}
