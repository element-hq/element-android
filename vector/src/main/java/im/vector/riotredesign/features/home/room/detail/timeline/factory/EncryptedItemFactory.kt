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

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.MXDecryptionException
import im.vector.matrix.android.internal.crypto.MXEventDecryptionResult
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem_

class EncryptedItemFactory(
        private val session: Session,
        private val stringProvider: StringProvider,
        private val messageItemFactory: MessageItemFactory) {

    fun create(timelineEvent: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {

        return when {
            EventType.ENCRYPTED == timelineEvent.root.getClearType() -> {
                val decrypted: MXEventDecryptionResult?
                try {
                    decrypted = session.decryptEvent(timelineEvent.root, "TODO")
                } catch (e: MXDecryptionException) {
                    val errorDescription =
                            if (e.cryptoError?.code == MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE) {
                                stringProvider.getString(R.string.notice_crypto_error_unkwown_inbound_session_id)
                            } else {
                                e.localizedMessage
                            }

                    val message = stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, errorDescription)

                    val spannableStr = SpannableString(message)
                    spannableStr.setSpan(StyleSpan(Typeface.ITALIC), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    // TODO This is not correct format for error, change it
                    return NoticeItem_()
                            .noticeText(spannableStr)
                            .avatarUrl(timelineEvent.senderAvatar)
                            .memberName(timelineEvent.senderName)
                }

                if (decrypted == null) {
                    return null
                }
                if (decrypted.clearEvent == null) {
                    return null
                }
                val adapter = MoshiProvider.providesMoshi().adapter(Event::class.java)
                val clearEvent = adapter.fromJsonValue(decrypted.clearEvent) ?: return null
                val decryptedTimelineEvent = timelineEvent.copy(root = clearEvent)

                // Success
                return messageItemFactory.create(decryptedTimelineEvent, nextEvent, callback)
            }
            else                                           -> null
        }
    }
}