/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline

import androidx.annotation.ColorInt
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.getColorFromUserId
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.session.room.send.SendState
import javax.inject.Inject

class MessageColorProvider @Inject constructor(
        private val colorProvider: ColorProvider,
        private val vectorPreferences: VectorPreferences) {

    @ColorInt
    fun getMemberNameTextColor(userId: String): Int {
        return colorProvider.getColor(getColorFromUserId(userId))
    }

    @ColorInt
    fun getMessageTextColor(sendState: SendState): Int {
        return if (vectorPreferences.developerMode()) {
            when (sendState) {
                // SendStates, in the classical order they will occur
                SendState.UNKNOWN,
                SendState.UNSENT                 -> colorProvider.getColorFromAttribute(R.attr.vctr_sending_message_text_color)
                SendState.ENCRYPTING             -> colorProvider.getColorFromAttribute(R.attr.vctr_encrypting_message_text_color)
                SendState.SENDING                -> colorProvider.getColorFromAttribute(R.attr.vctr_sending_message_text_color)
                SendState.SENT,
                SendState.SYNCED                 -> colorProvider.getColorFromAttribute(R.attr.vctr_message_text_color)
                SendState.UNDELIVERED,
                SendState.FAILED_UNKNOWN_DEVICES -> colorProvider.getColorFromAttribute(R.attr.vctr_unsent_message_text_color)
            }
        } else {
            // When not in developer mode, we do not use special color for the encrypting state
            when (sendState) {
                SendState.UNKNOWN,
                SendState.UNSENT,
                SendState.ENCRYPTING,
                SendState.SENDING                -> colorProvider.getColorFromAttribute(R.attr.vctr_sending_message_text_color)
                SendState.SENT,
                SendState.SYNCED                 -> colorProvider.getColorFromAttribute(R.attr.vctr_message_text_color)
                SendState.UNDELIVERED,
                SendState.FAILED_UNKNOWN_DEVICES -> colorProvider.getColorFromAttribute(R.attr.vctr_unsent_message_text_color)
            }
        }
    }
}
