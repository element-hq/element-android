/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.Editable
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import im.vector.app.core.extensions.ooi
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.features.home.room.detail.composer.images.UriContentListener
import im.vector.app.features.html.PillImageSpan
import timber.log.Timber

class ComposerEditText @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    interface Callback {
        fun onRichContentSelected(contentUri: Uri): Boolean
        fun onTextChanged(text: CharSequence)
    }

    var callback: Callback? = null

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
        var ic = super.onCreateInputConnection(editorInfo) ?: return null
        val mimeTypes = ViewCompat.getOnReceiveContentMimeTypes(this) ?: arrayOf("image/*")

        EditorInfoCompat.setContentMimeTypes(editorInfo, mimeTypes)
        ic = InputConnectionCompat.createWrapper(this, ic, editorInfo)

        ViewCompat.setOnReceiveContentListener(
                this,
                mimeTypes,
                UriContentListener { callback?.onRichContentSelected(it) }
        )

        return ic
    }

    /** Set whether the keyboard should disable personalized learning. */
    @RequiresApi(Build.VERSION_CODES.O)
    fun setUseIncognitoKeyboard(useIncognitoKeyboard: Boolean) {
        imeOptions = if (useIncognitoKeyboard) {
            imeOptions or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        } else {
            imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
        }
    }

    /** Set whether enter should send the message or add a new line. */
    fun setSendMessageWithEnter(sendMessageWithEnter: Boolean) {
        if (sendMessageWithEnter) {
            inputType = inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE.inv()
            imeOptions = imeOptions or EditorInfo.IME_ACTION_SEND
        } else {
            inputType = inputType or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
            imeOptions = imeOptions and EditorInfo.IME_ACTION_SEND.inv()
        }
    }

    init {
        addTextChangedListener(
                object : SimpleTextWatcher() {
                    var spanToRemove: PillImageSpan? = null

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                        Timber.v("Pills: beforeTextChanged: start:$start count:$count after:$after")

                        if (count > after) {
                            // A char has been deleted
                            val deleteCharPosition = start + count
                            Timber.v("Pills: beforeTextChanged: deleted char at $deleteCharPosition")

                            // Get the first span at this position
                            spanToRemove = editableText.getSpans(deleteCharPosition, deleteCharPosition, PillImageSpan::class.java)
                                    .ooi { Timber.v("Pills: beforeTextChanged: found ${it.size} span(s)") }
                                    .firstOrNull()
                        }
                    }

                    override fun afterTextChanged(s: Editable) {
                        if (spanToRemove != null) {
                            val start = editableText.getSpanStart(spanToRemove)
                            val end = editableText.getSpanEnd(spanToRemove)
                            Timber.v("Pills: afterTextChanged Removing the span start:$start end:$end")
                            // Must be done before text replacement
                            editableText.removeSpan(spanToRemove)
                            if (start != -1 && end != -1) {
                                editableText.replace(start, end, "")
                            }
                            spanToRemove = null
                        }
                        callback?.onTextChanged(s.toString())
                    }
                }
        )
    }
}
