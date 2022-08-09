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

package im.vector.app.features.debug.sas

import com.airbnb.epoxy.TypedEpoxyController
import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation

data class SasState(
        val emojiList: List<EmojiRepresentation>
)

class SasEmojiController : TypedEpoxyController<SasState>() {

    override fun buildModels(data: SasState?) {
        if (data == null) return

        data.emojiList.forEachIndexed { idx, emojiRepresentation ->
            sasEmojiItem {
                id(idx)
                index(idx)
                emojiRepresentation(emojiRepresentation)
            }
        }
    }
}
