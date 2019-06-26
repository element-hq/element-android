package im.vector.riotredesign.features.home.room.detail.composer

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.riotredesign.R


/**
 * Encapsulate the timeline composer UX.
 *
 */
class TextComposerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                                                 defStyleAttr: Int = 0) : ConstraintLayout(context, attrs, defStyleAttr) {

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
    lateinit var composerEditText: EditText
    @BindView(R.id.composer_avatar_view)
    lateinit var composerAvatarImageView: ImageView

    var currentConstraintSetId: Int = -1


    init {
        inflate(context, R.layout.merge_composer_layout, this)
        ButterKnife.bind(this)
        collapse(false)
    }


    fun collapse(animate: Boolean = true, transitionComplete: (() -> Unit)? = null) {
        if (currentConstraintSetId == R.layout.constraint_set_composer_layout_compact) {
            //ignore we good
            return
        }
        currentConstraintSetId = R.layout.constraint_set_composer_layout_compact
        if (animate) {
            val transition = AutoTransition()
//            transition.duration = 5000
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
            //ignore we good
            return
        }
        currentConstraintSetId = R.layout.constraint_set_composer_layout_expanded
        if (animate) {
            val transition = AutoTransition()
//            transition.duration = 5000
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

    fun setRoomEncrypted(isEncrypted: Boolean) {
        composerEditText.setHint(
                if (isEncrypted) {
                    R.string.room_message_placeholder_encrypted
                } else {
                    R.string.room_message_placeholder_not_encrypted
                })
    }
}