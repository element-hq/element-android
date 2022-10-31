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

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.toSpannable
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.animateLayoutChange
import im.vector.app.core.extensions.setTextIfDifferent
import im.vector.app.databinding.ComposerRichTextLayoutBinding
import im.vector.app.databinding.ViewRichTextMenuButtonBinding
import io.element.android.wysiwyg.EditorEditText
import io.element.android.wysiwyg.inputhandlers.models.InlineFormat
import uniffi.wysiwyg_composer.ComposerAction
import uniffi.wysiwyg_composer.MenuState

class RichTextComposerLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), MessageComposerView {

    private val views: ComposerRichTextLayoutBinding

    override var callback: Callback? = null

    private var currentConstraintSetId: Int = -1
    private val animationDuration = 100L
    private val maxEditTextLinesWhenCollapsed = 12

    private val isFullScreen: Boolean get() = currentConstraintSetId == R.layout.composer_rich_text_layout_constraint_set_fullscreen

    var isTextFormattingEnabled = true
        set(value) {
            if (field == value) return
            syncEditTexts()
            field = value
            updateEditTextVisibility()
        }

    override val text: Editable?
        get() = editText.text
    override val formattedText: String?
        get() = (editText as? EditorEditText)?.getHtmlOutput()
    override val editText: EditText
        get() = if (isTextFormattingEnabled) {
            views.richTextComposerEditText
        } else {
            views.plainTextComposerEditText
        }
    override val emojiButton: ImageButton?
        get() = null
    override val sendButton: ImageButton
        get() = views.sendButton
    override val attachmentButton: ImageButton
        get() = views.attachmentButton
    override val fullScreenButton: ImageButton?
        get() = views.composerFullScreenButton
    override val composerRelatedMessageActionIcon: ImageView
        get() = views.composerRelatedMessageActionIcon
    override val composerRelatedMessageAvatar: ImageView
        get() = views.composerRelatedMessageAvatar
    override val composerRelatedMessageContent: TextView
        get() = views.composerRelatedMessageContent
    override val composerRelatedMessageImage: ImageView
        get() = views.composerRelatedMessageImage
    override val composerRelatedMessageTitle: TextView
        get() = views.composerRelatedMessageTitle
    override var isVisible: Boolean
        get() = views.root.isVisible
        set(value) { views.root.isVisible = value }

    init {
        inflate(context, R.layout.composer_rich_text_layout, this)
        views = ComposerRichTextLayoutBinding.bind(this)

        collapse(false)

        views.richTextComposerEditText.addTextChangedListener(
                TextChangeListener({ callback?.onTextChanged(it) }, { updateTextFieldBorder() })
        )
        views.plainTextComposerEditText.addTextChangedListener(
                TextChangeListener({ callback?.onTextChanged(it) }, { updateTextFieldBorder() })
        )

        views.composerRelatedMessageCloseButton.setOnClickListener {
            collapse()
            callback?.onCloseRelatedMessage()
        }

        views.sendButton.setOnClickListener {
            val textMessage = text?.toSpannable() ?: ""
            callback?.onSendMessage(textMessage)
        }

        views.attachmentButton.setOnClickListener {
            callback?.onAddAttachment()
        }

        views.composerFullScreenButton.setOnClickListener {
            callback?.onFullScreenModeChanged()
        }

        setupRichTextMenu()
    }

    private fun setupRichTextMenu() {
        addRichTextMenuItem(R.drawable.ic_composer_bold, R.string.rich_text_editor_format_bold, ComposerAction.Bold) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Bold)
        }
        addRichTextMenuItem(R.drawable.ic_composer_italic, R.string.rich_text_editor_format_italic, ComposerAction.Italic) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Italic)
        }
        addRichTextMenuItem(R.drawable.ic_composer_underlined, R.string.rich_text_editor_format_underline, ComposerAction.Underline) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Underline)
        }
        addRichTextMenuItem(R.drawable.ic_composer_strikethrough, R.string.rich_text_editor_format_strikethrough, ComposerAction.StrikeThrough) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.StrikeThrough)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        views.richTextComposerEditText.menuStateChangedListener = EditorEditText.OnMenuStateChangedListener { state ->
            if (state is MenuState.Update) {
                updateMenuStateFor(ComposerAction.Bold, state)
                updateMenuStateFor(ComposerAction.Italic, state)
                updateMenuStateFor(ComposerAction.Underline, state)
                updateMenuStateFor(ComposerAction.StrikeThrough, state)
            }
        }

        updateEditTextVisibility()
    }

    private fun updateEditTextVisibility() {
        views.richTextComposerEditText.isVisible = isTextFormattingEnabled
        views.richTextMenu.isVisible = isTextFormattingEnabled
        views.plainTextComposerEditText.isVisible = !isTextFormattingEnabled
    }

    /**
     * Updates the non-active input with the contents of the active input.
     */
    private fun syncEditTexts() =
        if (isTextFormattingEnabled) {
            views.plainTextComposerEditText.setText(views.richTextComposerEditText.getPlainText())
        } else {
            views.richTextComposerEditText.setText(views.plainTextComposerEditText.text.toString())
        }

    private fun addRichTextMenuItem(@DrawableRes iconId: Int, @StringRes description: Int, action: ComposerAction, onClick: () -> Unit) {
        val inflater = LayoutInflater.from(context)
        val button = ViewRichTextMenuButtonBinding.inflate(inflater, views.richTextMenu, true)
        button.root.tag = action
        with(button.root) {
            contentDescription = resources.getString(description)
            setImageResource(iconId)
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun updateMenuStateFor(action: ComposerAction, menuState: MenuState.Update) {
        val button = findViewWithTag<ImageButton>(action) ?: return
        button.isEnabled = !menuState.disabledActions.contains(action)
        button.isSelected = menuState.reversedActions.contains(action)
    }

    private fun updateTextFieldBorder() {
        val isExpanded = editText.editableText.lines().count() > 1
        val borderResource = if (isExpanded || isFullScreen) {
            R.drawable.bg_composer_rich_edit_text_expanded
        } else {
            R.drawable.bg_composer_rich_edit_text_single_line
        }
        views.composerEditTextOuterBorder.setBackgroundResource(borderResource)
    }

    override fun replaceFormattedContent(text: CharSequence) {
        views.richTextComposerEditText.setHtml(text.toString())
    }

    override fun collapse(animate: Boolean, transitionComplete: (() -> Unit)?) {
        if (currentConstraintSetId == R.layout.composer_rich_text_layout_constraint_set_compact) {
            // ignore we good
            return
        }
        currentConstraintSetId = R.layout.composer_rich_text_layout_constraint_set_compact
        applyNewConstraintSet(animate, transitionComplete)
        updateEditTextVisibility()
    }

    override fun expand(animate: Boolean, transitionComplete: (() -> Unit)?) {
        if (currentConstraintSetId == R.layout.composer_rich_text_layout_constraint_set_expanded) {
            // ignore we good
            return
        }
        currentConstraintSetId = R.layout.composer_rich_text_layout_constraint_set_expanded
        applyNewConstraintSet(animate, transitionComplete)
        updateEditTextVisibility()
    }

    override fun setTextIfDifferent(text: CharSequence?): Boolean {
        return editText.setTextIfDifferent(text)
    }

    override fun toggleFullScreen(newValue: Boolean) {
        val constraintSetId = if (newValue) R.layout.composer_rich_text_layout_constraint_set_fullscreen else currentConstraintSetId
        ConstraintSet().also {
            it.clone(context, constraintSetId)
            it.applyTo(this)
        }

        updateTextFieldBorder()
        updateEditTextVisibility()

        updateEditTextFullScreenState(views.richTextComposerEditText, newValue)
        updateEditTextFullScreenState(views.plainTextComposerEditText, newValue)
    }

    private fun updateEditTextFullScreenState(editText: EditText, isFullScreen: Boolean) {
        if (isFullScreen) {
            editText.maxLines = Int.MAX_VALUE
            // This is a workaround to fix incorrect scroll position when maximised
            post { editText.requestLayout() }
        } else {
            editText.maxLines = maxEditTextLinesWhenCollapsed
        }
    }

    private fun applyNewConstraintSet(animate: Boolean, transitionComplete: (() -> Unit)?) {
        // val wasSendButtonInvisible = views.sendButton.isInvisible
        if (animate) {
            animateLayoutChange(animationDuration, transitionComplete)
        }
        ConstraintSet().also {
            it.clone(context, currentConstraintSetId)
            it.applyTo(this)
        }

        // Might be updated by view state just after, but avoid blinks
        // views.sendButton.isInvisible = wasSendButtonInvisible
    }

    override fun setInvisible(isInvisible: Boolean) {
        this.isInvisible = isInvisible
    }

    private class TextChangeListener(
            private val onTextChanged: (s: Editable) -> Unit,
            private val onExpandedChanged: (isExpanded: Boolean) -> Unit,
    ) : TextWatcher {
        private var previousTextWasExpanded = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable) {
            onTextChanged.invoke(s)

            val isExpanded = s.lines().count() > 1
            if (previousTextWasExpanded != isExpanded) {
                onExpandedChanged(isExpanded)
            }
            previousTextWasExpanded = isExpanded
        }
    }
}
