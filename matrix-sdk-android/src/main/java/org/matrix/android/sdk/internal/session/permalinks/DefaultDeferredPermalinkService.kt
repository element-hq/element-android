/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.permalinks

import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import org.matrix.android.sdk.api.session.permalinks.DeferredPermalinkService
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import javax.inject.Inject

class DefaultDeferredPermalinkService @Inject constructor(
        private val context: Context
) : DeferredPermalinkService {

    override fun getLinkFromClipBoard(): String? {
        val clipboard = context.getSystemService<ClipboardManager>()
        clipboard?.primaryClip?.let { clip ->
            if (clip.itemCount == 0) {
                return null
            }
            for (i in 0 until clip.itemCount) {
                val clipText = clip.getItemAt(i).text.toString()
                val data = PermalinkParser.parse(clipText)
                if (data is PermalinkData.RoomLink) {
                    return clipText
                }
            }
        }
        return null
    }
}
