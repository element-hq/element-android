/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer.images

import android.content.ClipData
import android.net.Uri
import android.view.View
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener

class UriContentListener(
        private val onContent: (uri: Uri) -> Unit
) : OnReceiveContentListener {
    override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
        val split = payload.partition { item -> item.uri != null }
        val uriContent = split.first
        val remaining = split.second

        if (uriContent != null) {
            val clip: ClipData = uriContent.clip
            for (i in 0 until clip.itemCount) {
                val uri = clip.getItemAt(i).uri
                // ... app-specific logic to handle the URI ...
                onContent(uri)
            }
        }
        // Return anything that we didn't handle ourselves. This preserves the default platform
        // behavior for text and anything else for which we are not implementing custom handling.
        return remaining
    }
}
