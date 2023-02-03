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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.toSpannable
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.shape.MaterialShapeDrawable
import im.vector.app.R
import im.vector.app.core.extensions.setTextIfDifferent
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ComposerRichTextLayoutBinding
import im.vector.app.databinding.ViewRichTextMenuButtonBinding
import io.element.android.wysiwyg.EditorEditText
import io.element.android.wysiwyg.inputhandlers.models.InlineFormat
import io.element.android.wysiwyg.inputhandlers.models.LinkAction
import io.element.android.wysiwyg.utils.RustErrorCollector
import uniffi.wysiwyg_composer.ActionState
import uniffi.wysiwyg_composer.ComposerAction

internal class RichTextComposerLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), MessageComposerView {

    private val views: ComposerRichTextLayoutBinding

    override var callback: Callback? = null

    // There is no need to persist these values since they're always updated by the parent fragment
    private var isFullScreen = false
    private var hasRelatedMessage = false
    private var composerMode: MessageComposerMode? = null

    var isTextFormattingEnabled = true
        set(value) {
            if (field == value) return
            syncEditTexts()
            field = value
            updateTextFieldBorder(isFullScreen)
            updateEditTextVisibility()
            updateFullScreenButtonVisibility()
            // If formatting is no longer enabled and it's in full screen, minimise the editor
            if (!value && isFullScreen) {
                callback?.onFullScreenModeChanged()
            }
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

    // Border of the EditText
    private val borderShapeDrawable: MaterialShapeDrawable by lazy {
        MaterialShapeDrawable().apply {
            val typedData = TypedValue()
            val lineColor = context.theme.obtainStyledAttributes(typedData.data, intArrayOf(R.attr.vctr_content_quaternary))
                    .getColor(0, 0)
            strokeColor = ColorStateList.valueOf(lineColor)
            strokeWidth = 1 * resources.displayMetrics.scaledDensity
            fillColor = ColorStateList.valueOf(Color.TRANSPARENT)
            val cornerSize = resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_single_line)
            setCornerSize(cornerSize.toFloat())
        }
    }

    private val dimensionConverter = DimensionConverter(resources)

    fun setFullScreen(isFullScreen: Boolean, animated: Boolean) {
        if (!animated && views.composerLayout.layoutParams != null) {
            views.composerLayout.updateLayoutParams<ViewGroup.LayoutParams> {
                height =
                        if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        editText.updateLayoutParams<ViewGroup.LayoutParams> {
            height = if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        }

        updateTextFieldBorder(isFullScreen)
        updateEditTextVisibility()

        updateEditTextFullScreenState(views.richTextComposerEditText, isFullScreen)
        updateEditTextFullScreenState(views.plainTextComposerEditText, isFullScreen)

        views.composerFullScreenButton.setImageResource(
                if (isFullScreen) R.drawable.ic_composer_collapse else R.drawable.ic_composer_full_screen
        )

        views.bottomSheetHandle.isVisible = isFullScreen
        if (isFullScreen) {
            editText.showKeyboard(true)
        }
        this.isFullScreen = isFullScreen
    }

    fun notifyIsBeingDragged(percentage: Float) {
        // Calculate a new shape for the border according to the position in screen
        val isSingleLine = editText.lineCount == 1
        val cornerSize = if (!isSingleLine || hasRelatedMessage) {
            resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_expanded).toFloat()
        } else {
            val multilineCornerSize = resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_expanded)
            val singleLineCornerSize = resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_single_line)
            val diff = singleLineCornerSize - multilineCornerSize
            multilineCornerSize + diff * (1 - percentage)
        }
        if (cornerSize != borderShapeDrawable.bottomLeftCornerResolvedSize) {
            borderShapeDrawable.setCornerSize(cornerSize)
        }

        // Change maxLines while dragging, this should improve the smoothness of animations
        val maxLines = if (percentage > 0.25f) {
            Int.MAX_VALUE
        } else {
            MessageComposerView.MAX_LINES_WHEN_COLLAPSED
        }
        views.richTextComposerEditText.maxLines = maxLines
        views.plainTextComposerEditText.maxLines = maxLines

        views.bottomSheetHandle.isVisible = true
    }

    init {
        inflate(context, R.layout.composer_rich_text_layout, this)
        views = ComposerRichTextLayoutBinding.bind(this)

        // Workaround to avoid cut-off text caused by padding in scrolled TextView (there is no clipToPadding).
        // In TextView, clipTop = padding, but also clipTop -= shadowRadius. So if we set the shadowRadius to padding, they cancel each other
        views.richTextComposerEditText.setShadowLayer(views.richTextComposerEditText.paddingBottom.toFloat(), 0f, 0f, 0)
        views.plainTextComposerEditText.setShadowLayer(views.richTextComposerEditText.paddingBottom.toFloat(), 0f, 0f, 0)

        renderComposerMode(MessageComposerMode.Normal(null))

        views.richTextComposerEditText.addTextChangedListener(
                TextChangeListener({ callback?.onTextChanged(it) }, { updateTextFieldBorder(isFullScreen) })
        )
        views.plainTextComposerEditText.addTextChangedListener(
                TextChangeListener({ callback?.onTextChanged(it) }, { updateTextFieldBorder(isFullScreen) })
        )

        disallowParentInterceptTouchEvent(views.richTextComposerEditText)
        disallowParentInterceptTouchEvent(views.plainTextComposerEditText)

        views.composerModeCloseView.setOnClickListener {
            callback?.onCloseRelatedMessage()
        }

        views.sendButton.setOnClickListener {
            val textMessage = text?.toSpannable() ?: ""
            callback?.onSendMessage(textMessage)
        }

        views.attachmentButton.setOnClickListener {
            callback?.onAddAttachment()
        }

        views.composerFullScreenButton.apply {
            updateFullScreenButtonVisibility()
            setOnClickListener {
                callback?.onFullScreenModeChanged()
            }
        }

        views.composerEditTextOuterBorder.background = borderShapeDrawable

        setupRichTextMenu()

        updateTextFieldBorder(isFullScreen)
    }

    private fun setupRichTextMenu() {
        addRichTextMenuItem(R.drawable.ic_composer_bold, R.string.rich_text_editor_format_bold, ComposerAction.BOLD) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Bold)
        }
        addRichTextMenuItem(R.drawable.ic_composer_italic, R.string.rich_text_editor_format_italic, ComposerAction.ITALIC) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Italic)
        }
        addRichTextMenuItem(R.drawable.ic_composer_underlined, R.string.rich_text_editor_format_underline, ComposerAction.UNDERLINE) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.Underline)
        }
        addRichTextMenuItem(R.drawable.ic_composer_strikethrough, R.string.rich_text_editor_format_strikethrough, ComposerAction.STRIKE_THROUGH) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.StrikeThrough)
        }
        addRichTextMenuItem(R.drawable.ic_composer_link, R.string.rich_text_editor_link, ComposerAction.LINK) {
            views.richTextComposerEditText.getLinkAction()?.let {
                when (it) {
                    LinkAction.InsertLink -> callback?.onSetLink(isTextSupported = true, initialLink = null)
                    is LinkAction.SetLink -> callback?.onSetLink(isTextSupported = false, initialLink = it.currentLink)
                }
            }
        }
        addRichTextMenuItem(R.drawable.ic_composer_bullet_list, R.string.rich_text_editor_bullet_list, ComposerAction.UNORDERED_LIST) {
            views.richTextComposerEditText.toggleList(ordered = false)
        }
        addRichTextMenuItem(R.drawable.ic_composer_numbered_list, R.string.rich_text_editor_numbered_list, ComposerAction.ORDERED_LIST) {
            views.richTextComposerEditText.toggleList(ordered = true)
        }
        addRichTextMenuItem(R.drawable.ic_composer_inline_code, R.string.rich_text_editor_inline_code, ComposerAction.INLINE_CODE) {
            views.richTextComposerEditText.toggleInlineFormat(InlineFormat.InlineCode)
        }
    }

    fun setLink(link: String?) =
            views.richTextComposerEditText.setLink(link)

    fun insertLink(link: String, text: String) =
            views.richTextComposerEditText.insertLink(link, text)

    fun removeLink() =
            views.richTextComposerEditText.removeLink()

    @SuppressLint("ClickableViewAccessibility")
    private fun disallowParentInterceptTouchEvent(view: View) {
        view.setOnTouchListener { v, event ->
            if (v.hasFocus()) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
                val action = event.actionMasked
                if (action == MotionEvent.ACTION_SCROLL) {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        views.richTextComposerEditText.actionStatesChangedListener = EditorEditText.OnActionStatesChangedListener { state ->
            for (action in state.keys) {
                updateMenuStateFor(action, state)
            }
        }
        updateEditTextVisibility()
    }

    fun setOnErrorListener(onError: (e: RichTextEditorException) -> Unit) {
        views.richTextComposerEditText.rustErrorCollector = RustErrorCollector {
            onError(RichTextEditorException(it))
        }
    }

    private fun updateEditTextVisibility() {
        views.richTextComposerEditText.isVisible = isTextFormattingEnabled
        views.richTextMenu.isVisible = isTextFormattingEnabled
        views.plainTextComposerEditText.isVisible = !isTextFormattingEnabled

        // The layouts for formatted text mode and plain text mode are different, so we need to update the constraints
        val dpToPx = { dp: Int -> dimensionConverter.dpToPx(dp) }
        ConstraintSet().apply {
            clone(views.composerLayoutContent)
            clear(R.id.composerEditTextOuterBorder, ConstraintSet.TOP)
            clear(R.id.composerEditTextOuterBorder, ConstraintSet.BOTTOM)
            clear(R.id.composerEditTextOuterBorder, ConstraintSet.START)
            clear(R.id.composerEditTextOuterBorder, ConstraintSet.END)
            if (isTextFormattingEnabled) {
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.TOP, R.id.composerLayoutContent, ConstraintSet.TOP, dpToPx(8))
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.BOTTOM, R.id.sendButton, ConstraintSet.TOP, 0)
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.START, R.id.composerLayoutContent, ConstraintSet.START, dpToPx(12))
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.END, R.id.composerLayoutContent, ConstraintSet.END, dpToPx(12))
            } else {
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.TOP, R.id.composerLayoutContent, ConstraintSet.TOP, dpToPx(8))
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.BOTTOM, R.id.composerLayoutContent, ConstraintSet.BOTTOM, dpToPx(8))
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.START, R.id.attachmentButton, ConstraintSet.END, 0)
                connect(R.id.composerEditTextOuterBorder, ConstraintSet.END, R.id.sendButton, ConstraintSet.START, 0)
            }
            applyTo(views.composerLayoutContent)
        }
    }

    private fun updateFullScreenButtonVisibility() {
        val isLargeScreenDevice = resources.configuration.isLayoutSizeAtLeast(Configuration.SCREENLAYOUT_SIZE_LARGE)
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        // There's no point in having full screen in landscape since there's almost no vertical space
        views.composerFullScreenButton.isInvisible = !isTextFormattingEnabled || (isLandscape && !isLargeScreenDevice)
    }

    /**
     * Updates the non-active input with the contents of the active input.
     */
    private fun syncEditTexts() =
        if (isTextFormattingEnabled) {
            views.plainTextComposerEditText.setText(views.richTextComposerEditText.getMarkdown())
        } else {
            views.richTextComposerEditText.setMarkdown(views.plainTextComposerEditText.text.toString())
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

    private fun updateMenuStateFor(action: ComposerAction, menuState: Map<ComposerAction, ActionState>) {
        val button = findViewWithTag<ImageButton>(action) ?: return
        val stateForAction = menuState[action]
        button.isEnabled = stateForAction != ActionState.DISABLED
        button.isSelected = stateForAction == ActionState.REVERSED
    }

    fun estimateCollapsedHeight(): Int {
        val editText = this.editText
        val originalLines = editText.maxLines
        val originalParamsHeight = editText.layoutParams.height
        editText.maxLines = MessageComposerView.MAX_LINES_WHEN_COLLAPSED
        editText.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.UNSPECIFIED,
        )
        val result = measuredHeight
        editText.layoutParams.height = originalParamsHeight
        editText.maxLines = originalLines
        return result
    }

    private fun updateTextFieldBorder(isFullScreen: Boolean) {
        val isMultiline = editText.editableText.lines().count() > 1 || isFullScreen || hasRelatedMessage
        val cornerSize = if (isMultiline) {
            resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_expanded)
        } else {
            resources.getDimensionPixelSize(R.dimen.rich_text_composer_corner_radius_single_line)
        }.toFloat()
        borderShapeDrawable.setCornerSize(cornerSize)
    }

    private fun replaceFormattedContent(text: CharSequence) {
        views.richTextComposerEditText.setHtml(text.toString())
        updateTextFieldBorder(isFullScreen)
    }

    override fun setTextIfDifferent(text: CharSequence?): Boolean {
        val result = editText.setTextIfDifferent(text)
        updateTextFieldBorder(isFullScreen)
        return result
    }

    private fun updateEditTextFullScreenState(editText: EditText, isFullScreen: Boolean) {
        if (isFullScreen) {
            editText.maxLines = Int.MAX_VALUE
        } else {
            editText.maxLines = MessageComposerView.MAX_LINES_WHEN_COLLAPSED
        }
    }

    override fun renderComposerMode(mode: MessageComposerMode) {
        if (mode is MessageComposerMode.Special) {
            views.composerModeGroup.isVisible = true
            if (isTextFormattingEnabled) {
                replaceFormattedContent(mode.defaultContent)
            } else {
                views.plainTextComposerEditText.setText(mode.defaultContent)
            }
            hasRelatedMessage = true
            editText.showKeyboard(andRequestFocus = true)
        } else {
            views.composerModeGroup.isGone = true
            (mode as? MessageComposerMode.Normal)?.content?.let { text ->
                if (isTextFormattingEnabled) {
                    replaceFormattedContent(text)
                } else {
                    views.plainTextComposerEditText.setText(text)
                }
            }
            hasRelatedMessage = false
        }

        updateTextFieldBorder(isFullScreen)

        if (this.composerMode == mode) return
        this.composerMode = mode

        views.sendButton.apply {
            if (mode is MessageComposerMode.Edit) {
                contentDescription = resources.getString(R.string.action_save)
                setImageResource(R.drawable.ic_composer_rich_text_save)
            } else {
                contentDescription = resources.getString(R.string.action_send)
                setImageResource(R.drawable.ic_rich_composer_send)
            }
        }

        when (mode) {
            is MessageComposerMode.Edit -> {
                views.composerModeTitleView.setText(R.string.editing)
                views.composerModeIconView.setImageResource(R.drawable.ic_composer_rich_text_editor_edit)
            }
            is MessageComposerMode.Quote -> {
                views.composerModeTitleView.setText(R.string.quoting)
                views.composerModeIconView.setImageResource(R.drawable.ic_quote)
            }
            is MessageComposerMode.Reply -> {
                val senderInfo = mode.event.senderInfo
                val userName = senderInfo.displayName ?: senderInfo.disambiguatedDisplayName
                views.composerModeTitleView.text = resources.getString(R.string.replying_to, userName)
                views.composerModeIconView.setImageResource(R.drawable.ic_reply)
            }
            else -> Unit
        }
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
