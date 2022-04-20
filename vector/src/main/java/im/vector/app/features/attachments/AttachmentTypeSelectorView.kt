/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.utils.PERMISSIONS_EMPTY
import im.vector.app.core.utils.PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING
import im.vector.app.core.utils.PERMISSIONS_FOR_PICKING_CONTACT
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.databinding.ViewAttachmentTypeSelectorBinding
import im.vector.app.features.attachments.AttachmentTypeSelectorView.Callback
import kotlin.math.max

private const val ANIMATION_DURATION = 250

/**
 * This class is the view presenting choices for picking attachments.
 * It will return result through [Callback].
 */

class AttachmentTypeSelectorView(context: Context,
                                 inflater: LayoutInflater,
                                 var callback: Callback?
) : PopupWindow(context) {

    interface Callback {
        fun onTypeSelected(type: Type)
    }

    private val views: ViewAttachmentTypeSelectorBinding

    private var anchor: View? = null

    init {
        contentView = inflater.inflate(R.layout.view_attachment_type_selector, null, false)
        views = ViewAttachmentTypeSelectorBinding.bind(contentView)
        views.attachmentGalleryButton.configure(Type.GALLERY)
        views.attachmentCameraButton.configure(Type.CAMERA)
        views.attachmentFileButton.configure(Type.FILE)
        views.attachmentStickersButton.configure(Type.STICKER)
        views.attachmentContactButton.configure(Type.CONTACT)
        views.attachmentPollButton.configure(Type.POLL)
        views.attachmentLocationButton.configure(Type.LOCATION)
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

    fun setAttachmentVisibility(type: Type, isVisible: Boolean) {
        when (type) {
            Type.CAMERA   -> views.attachmentCameraButton
            Type.GALLERY  -> views.attachmentGalleryButton
            Type.FILE     -> views.attachmentFileButton
            Type.STICKER  -> views.attachmentStickersButton
            Type.CONTACT  -> views.attachmentContactButton
            Type.POLL     -> views.attachmentPollButton
            Type.LOCATION -> views.attachmentLocationButton
        }.let {
            it.isVisible = isVisible
        }
    }

    private fun animateWindowInCircular(anchor: View, contentView: View) {
        val coordinates = getClickCoordinates(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(contentView,
                coordinates.first,
                coordinates.second,
                0f,
                max(contentView.width, contentView.height).toFloat())
        animator.duration = ANIMATION_DURATION.toLong()
        animator.start()
    }

    private fun animateWindowOutCircular(anchor: View, contentView: View) {
        val coordinates = getClickCoordinates(anchor, contentView)
        val animator = ViewAnimationUtils.createCircularReveal(getContentView(),
                coordinates.first,
                coordinates.second,
                max(getContentView().width, getContentView().height).toFloat(),
                0f)

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

    private fun ImageButton.configure(type: Type): ImageButton {
        this.setOnClickListener(TypeClickListener(type))
        TooltipCompat.setTooltipText(this, context.getString(type.tooltipRes))
        return this
    }

    private inner class TypeClickListener(private val type: Type) : View.OnClickListener {

        override fun onClick(v: View) {
            dismiss()
            callback?.onTypeSelected(type)
        }
    }

    /**
     * The all possible types to pick with their required permissions and tooltip resource
     */
    enum class Type(val permissions: List<String>, @StringRes val tooltipRes: Int) {
        CAMERA(PERMISSIONS_FOR_TAKING_PHOTO, R.string.tooltip_attachment_photo),
        GALLERY(PERMISSIONS_EMPTY, R.string.tooltip_attachment_gallery),
        FILE(PERMISSIONS_EMPTY, R.string.tooltip_attachment_file),
        STICKER(PERMISSIONS_EMPTY, R.string.tooltip_attachment_sticker),
        CONTACT(PERMISSIONS_FOR_PICKING_CONTACT, R.string.tooltip_attachment_contact),
        POLL(PERMISSIONS_EMPTY, R.string.tooltip_attachment_poll),
        LOCATION(PERMISSIONS_FOR_FOREGROUND_LOCATION_SHARING, R.string.tooltip_attachment_location)
    }
}
