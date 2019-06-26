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
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.home.room.detail.timeline.helper.senderAvatar
import im.vector.riotredesign.features.home.room.detail.timeline.helper.senderName
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.home.room.detail.timeline.item.NoticeItem_
import javax.inject.Inject

// This class handles timeline event who haven't been successfully decrypted
class EncryptedItemFactory @Inject constructor(private val stringProvider: StringProvider,
                                               private val avatarRenderer: AvatarRenderer) {

    fun create(timelineEvent: TimelineEvent): VectorEpoxyModel<*>? {
        return when {
            EventType.ENCRYPTED == timelineEvent.root.getClearType() -> {
                val cryptoError = timelineEvent.root.mCryptoError
                val errorDescription =
                        if (cryptoError?.code == MXCryptoError.UNKNOWN_INBOUND_SESSION_ID_ERROR_CODE) {
                            stringProvider.getString(R.string.notice_crypto_error_unkwown_inbound_session_id)
                        } else {
                            cryptoError?.message
                        }

                val message = stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, errorDescription)
                val spannableStr = SpannableString(message)
                spannableStr.setSpan(StyleSpan(Typeface.ITALIC), 0, message.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                // TODO This is not correct format for error, change it
                val informationData = MessageInformationData(
                        eventId = timelineEvent.root.eventId ?: "?",
                        senderId = timelineEvent.root.sender ?: "",
                        sendState = timelineEvent.sendState,
                        avatarUrl = timelineEvent.senderAvatar(),
                        memberName = timelineEvent.senderName(),
                        showInformation = false
                )
                return NoticeItem_()
                        .avatarRenderer(avatarRenderer)
                        .noticeText(spannableStr)
                        .informationData(informationData)
            }
            else                                                     -> null
        }
    }
}