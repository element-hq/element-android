/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer

import android.text.Editable
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

interface MessageComposerView {

    val text: Editable?
    val formattedText: String?
    val editText: EditText
    val emojiButton: ImageButton?
    val sendButton: ImageButton
    val attachmentButton: ImageButton
    val composerRelatedMessageTitle: TextView
    val composerRelatedMessageContent: TextView
    val composerRelatedMessageImage: ImageView
    val composerRelatedMessageActionIcon: ImageView
    val composerRelatedMessageAvatar: ImageView

    var callback: PlainTextComposerLayout.Callback?

    var isVisible: Boolean

    fun collapse(animate: Boolean = true, transitionComplete: (() -> Unit)? = null)
    fun expand(animate: Boolean = true, transitionComplete: (() -> Unit)? = null)
    fun setTextIfDifferent(text: CharSequence?): Boolean
    fun replaceFormattedContent(text: CharSequence)

    fun setInvisible(isInvisible: Boolean)
}
