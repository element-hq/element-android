/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import android.text.Editable
import android.widget.EditText
import android.widget.ImageButton

interface MessageComposerView {

    companion object {
        const val MAX_LINES_WHEN_COLLAPSED = 10
    }

    val text: Editable?
    val formattedText: String?
    val editText: EditText
    val emojiButton: ImageButton?
    val sendButton: ImageButton
    val attachmentButton: ImageButton

    var callback: Callback?

    fun setTextIfDifferent(text: CharSequence?): Boolean
    fun renderComposerMode(mode: MessageComposerMode)
}

interface Callback : ComposerEditText.Callback {
    fun onCloseRelatedMessage()
    fun onSendMessage(text: CharSequence)
    fun onAddAttachment()
    fun onExpandOrCompactChange()
    fun onFullScreenModeChanged()
    fun onSetLink(isTextSupported: Boolean, initialLink: String?)
}
