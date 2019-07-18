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

package im.vector.riotx.features.ui

import androidx.annotation.ColorInt
import im.vector.matrix.android.api.session.room.send.SendState
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider

@ColorInt
fun ColorProvider.getMessageTextColor(sendState: SendState): Int {
    return when (sendState) {
        // SendStates, in the classical order they will occur
        SendState.UNKNOWN,
        SendState.UNSENT                 -> getColorFromAttribute(R.attr.vctr_sending_message_text_color)
        SendState.ENCRYPTING             -> getColorFromAttribute(R.attr.vctr_encrypting_message_text_color)
        SendState.SENDING                -> getColorFromAttribute(R.attr.vctr_sending_message_text_color)
        SendState.SENT,
        SendState.SYNCED                 -> getColorFromAttribute(R.attr.vctr_message_text_color)
        SendState.UNDELIVERED,
        SendState.FAILED_UNKNOWN_DEVICES -> getColorFromAttribute(R.attr.vctr_unsent_message_text_color)
    }
}
