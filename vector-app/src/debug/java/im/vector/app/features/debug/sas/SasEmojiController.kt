/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
