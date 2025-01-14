/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.util.Pair
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.databinding.ViewAttachmentTypeSelectorBinding
import im.vector.app.features.attachments.AttachmentTypeSelectorView.Callback
import im.vector.lib.strings.CommonStrings
import kotlin.math.max

private const val ANIMATION_DURATION = 250

/**
 * This class is the view presenting choices for picking attachments.
 * It will return result through [Callback].
 */

class AttachmentTypeSelectorView(
        context: Context,
        inflater: LayoutInflater,
        var callback: Callback?
) : PopupWindow(context) {

    interface Callback {
        fun onTypeSelected(type: AttachmentType)
    }

    private val views: ViewAttachmentTypeSelectorBinding

    private var anchor: View? = null

    init {
        contentView = inflater.inflate(R.layout.view_attachment_type_selector, null, false)
        views = ViewAttachmentTypeSelectorBinding.bind(contentView)
        views.attachmentGalleryButton.configure(AttachmentType.GALLERY)
        views.attachmentCameraButton.configure(AttachmentType.CAMERA)
        views.attachmentFileButton.configure(AttachmentType.FILE)
        views.attachmentStickersButton.configure(AttachmentType.STICKER)
        views.attachmentContactButton.configure(AttachmentType.CONTACT)
        views.attachmentPollButton.configure(AttachmentType.POLL)
        views.attachmentLocationButton.configure(AttachmentType.LOCATION)
        views.attachmentVoiceBroadcast.configure(AttachmentType.VOICE_BROADCAST)
        width = LinearLayout.LayoutParams.MATCH_PARENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        animationStyle = 0
        @Suppress("DEPRECATION")
        setBackgroundDrawable(BitmapDrawable())
        inputMethodMode = INPUT_METHOD_NOT_NEEDED
        isFocusable = true
        isTouchable = true

        views.attachmentCloseButton.onClick {
            dismiss()
        }
    }

    private fun animateOpen() {
        views.attachmentCloseButton.animate()
                .setDuration(200)
                .rotation(135f)
    }

    private fun animateClose() {
        views.attachmentCloseButton.animate()
                .setDuration(200)
                .rotation(0f)
    }

    fun show(anchor: View) {
        animateOpen()

        this.anchor = anchor
        val anchorCoordinates = IntArray(2)
        anchor.getLocationOnScreen(anchorCoordinates)
        showAtLocation(anchor, Gravity.NO_GRAVITY, 0, anchorCoordinates[1])

        contentView.doOnNextLayout {
            animateWindowInCircular(anchor, contentView)
        }
    }

    override fun dismiss() {
        animateClose()

        val capturedAnchor = anchor
        if (capturedAnchor != null) {
            animateWindowOutCircular(capturedAnchor, contentView)
        } else {
            animateWindowOutTranslate(contentView)
        }
    }

    fun setAttachmentVisibility(type: AttachmentType, isVisible: Boolean) {
        when (type) {
            AttachmentType.CAMERA -> views.attachmentCameraButton
            AttachmentType.GALLERY -> views.attachmentGalleryButton
            AttachmentType.FILE -> views.attachmentFileButton
            AttachmentType.STICKER -> views.attachmentStickersButton
            AttachmentType.CONTACT -> views.attachmentContactButton
            AttachmentType.POLL -> views.attachmentPollButton
            AttachmentType.LOCATION -> views.attachmentLocationButton
            AttachmentType.VOICE_BROADCAST -> views.attachmentVoiceBroadcast
        }.let {
            it.isVisible = isVisible
        }
    }

    private fun animateWindowInCircular(anchor: View, contentView: View) {
        val coordinates = getClickCoordinates(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(
                contentView,
                coordinates.first,
                coordinates.second,
                0f,
                max(contentView.width, contentView.height).toFloat()
        )
        animator.duration = ANIMATION_DURATION.toLong()
        animator.start()
    }

    private fun animateWindowOutCircular(anchor: View, contentView: View) {
        val coordinates = getClickCoordinates(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(
                getContentView(),
                coordinates.first,
                coordinates.second,
                max(getContentView().width, getContentView().height).toFloat(),
                0f
        )

        animator.duration = ANIMATION_DURATION.toLong()
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super@AttachmentTypeSelectorView.dismiss()
            }
        })
        animator.start()
    }

    private fun animateWindowOutTranslate(contentView: View) {
        val animation = TranslateAnimation(0f, 0f, 0f, (contentView.top + contentView.height).toFloat())
        animation.duration = ANIMATION_DURATION.toLong()
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                super@AttachmentTypeSelectorView.dismiss()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        getContentView().startAnimation(animation)
    }

    private fun getClickCoordinates(anchor: View, contentView: View): Pair<Int, Int> {
        val anchorCoordinates = IntArray(2)
        anchor.getLocationOnScreen(anchorCoordinates)
        val contentCoordinates = IntArray(2)
        contentView.getLocationOnScreen(contentCoordinates)
        val x = anchorCoordinates[0] - contentCoordinates[0] + anchor.width / 2
        val y = anchorCoordinates[1] - contentCoordinates[1]
        return Pair(x, y)
    }

    private fun ImageButton.configure(type: AttachmentType): ImageButton {
        this.setOnClickListener(TypeClickListener(type))
        TooltipCompat.setTooltipText(this, context.getString(attachmentTooltipLabels.getValue(type)))
        return this
    }

    private inner class TypeClickListener(private val type: AttachmentType) : View.OnClickListener {

        override fun onClick(v: View) {
            dismiss()
            callback?.onTypeSelected(type)
        }
    }

    /**
     * The all possible types to pick with their required permissions and tooltip resource.
     */
    private companion object {
        private val attachmentTooltipLabels: Map<AttachmentType, Int> = AttachmentType.values().associateWith {
            when (it) {
                AttachmentType.CAMERA -> CommonStrings.tooltip_attachment_photo
                AttachmentType.GALLERY -> CommonStrings.tooltip_attachment_gallery
                AttachmentType.FILE -> CommonStrings.tooltip_attachment_file
                AttachmentType.STICKER -> CommonStrings.tooltip_attachment_sticker
                AttachmentType.CONTACT -> CommonStrings.tooltip_attachment_contact
                AttachmentType.POLL -> CommonStrings.tooltip_attachment_poll
                AttachmentType.LOCATION -> CommonStrings.tooltip_attachment_location
                AttachmentType.VOICE_BROADCAST -> CommonStrings.tooltip_attachment_voice_broadcast
            }
        }
    }
}
