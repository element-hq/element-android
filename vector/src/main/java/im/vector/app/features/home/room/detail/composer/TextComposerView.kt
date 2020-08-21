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

package im.vector.app.features.home.room.detail.composer

import android.content.Context
import android.net.Uri
import android.text.Editable
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.app.R
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel
import kotlinx.android.synthetic.main.merge_composer_layout.view.*

/**
 * Encapsulate the timeline composer UX.
 *
 */
class TextComposerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Callback : ComposerEditText.Callback {
        fun onCloseRelatedMessage()
        fun onSendMessage(text: CharSequence)
        fun onAddAttachment()
    }

    var callback: Callback? = null

    @BindView(R.id.composer_related_message_sender)
    lateinit var composerRelatedMessageTitle: TextView
    @BindView(R.id.composer_related_message_preview)
    lateinit var composerRelatedMessageContent: TextView
    @BindView(R.id.composer_related_message_avatar_view)
    lateinit var composerRelatedMessageAvatar: ImageView
    @BindView(R.id.composer_related_message_action_image)
    lateinit var composerRelatedMessageActionIcon: ImageView
    @BindView(R.id.composer_related_message_close)
    lateinit var composerRelatedMessageCloseButton: ImageButton
    @BindView(R.id.composerEditText)
    lateinit var composerEditText: ComposerEditText
    @BindView(R.id.composer_avatar_view)
    lateinit var composerAvatarImageView: ImageView
    @BindView(R.id.composer_shield)
    lateinit var composerShieldImageView: ImageView

    private var currentConstraintSetId: Int = -1

    private val animationDuration = 100L

    val text: Editable?
        get() = composerEditText.text

    init {
        inflate(context, R.layout.merge_composer_layout, this)
        ButterKnife.bind(this)
        collapse(false)
        composerEditText.callback = object : ComposerEditText.Callback {
            override fun onRichContentSelected(contentUri: Uri): Boolean {
                return callback?.onRichContentSelected(contentUri) ?: false
            }
        }
        composerRelatedMessageCloseButton.setOnClickListener {
            collapse()
            callback?.onCloseRelatedMessage()
        }

        sendButton.setOnClickListener {
            val textMessage = text?.toSpannable() ?: ""
            callback?.onSendMessage(textMessage)
        }

        attachmentButton.setOnClickListener {
            callback?.onAddAttachment()
        }
    }

    fun collapse(animate: Boolean = true, transitionComplete: (() -> Unit)? = null) {
        if (currentConstraintSetId == R.layout.constraint_set_composer_layout_compact) {
            // ignore we good
            return
        }
        currentConstraintSetId = R.layout.constraint_set_composer_layout_compact
        if (animate) {
            val transition = AutoTransition()
            transition.duration = animationDuration
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    transitionComplete?.invoke()
                }

                override fun onTransitionResume(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionCancel(transition: Transition) {}

                override fun onTransitionStart(transition: Transition) {}
            }
            )
            TransitionManager.beginDelayedTransition((parent as? ViewGroup ?: this), transition)
        }
        ConstraintSet().also {
            it.clone(context, currentConstraintSetId)
            it.applyTo(this)
        }
    }

    fun expand(animate: Boolean = true, transitionComplete: (() -> Unit)? = null) {
        if (currentConstraintSetId == R.layout.constraint_set_composer_layout_expanded) {
            // ignore we good
            return
        }
        currentConstraintSetId = R.layout.constraint_set_composer_layout_expanded
        if (animate) {
            val transition = AutoTransition()
            transition.duration = animationDuration
            transition.addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition) {
                    transitionComplete?.invoke()
                }

                override fun onTransitionResume(transition: Transition) {}

                override fun onTransitionPause(transition: Transition) {}

                override fun onTransitionCancel(transition: Transition) {}

                override fun onTransitionStart(transition: Transition) {}
            }
            )
            TransitionManager.beginDelayedTransition((parent as? ViewGroup ?: this), transition)
        }
        ConstraintSet().also {
            it.clone(context, currentConstraintSetId)
            it.applyTo(this)
        }
    }

    fun setRoomEncrypted(isEncrypted: Boolean, roomEncryptionTrustLevel: RoomEncryptionTrustLevel?) {
        if (isEncrypted) {
            composerEditText.setHint(R.string.room_message_placeholder)
            composerShieldImageView.isVisible = true
            val shieldRes = when (roomEncryptionTrustLevel) {
                RoomEncryptionTrustLevel.Trusted -> R.drawable.ic_shield_trusted
                RoomEncryptionTrustLevel.Warning -> R.drawable.ic_shield_warning
                else                             -> R.drawable.ic_shield_black
            }
            composerShieldImageView.setImageResource(shieldRes)
        } else {
            composerEditText.setHint(R.string.room_message_placeholder)
            composerShieldImageView.isVisible = false
        }
    }
}
