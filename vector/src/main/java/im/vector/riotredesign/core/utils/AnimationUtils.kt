package im.vector.riotredesign.core.utils

import android.view.animation.OvershootInterpolator
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.ChangeBounds
import androidx.transition.Transition
import androidx.transition.TransitionManager


inline fun ConstraintLayout.updateConstraintSet(@LayoutRes layoutId: Int, rootLayoutForAnimation: ConstraintLayout? = null, noinline onAnimationEnd: (() -> Unit)? = null) {
    if (rootLayoutForAnimation != null) {
        val transition = ChangeBounds()
        transition.interpolator = OvershootInterpolator()
        transition.addListener(object : Transition.TransitionListener {
            override fun onTransitionResume(transition: Transition) {
            }

            override fun onTransitionPause(transition: Transition) {
            }

            override fun onTransitionCancel(transition: Transition) {
            }

            override fun onTransitionStart(transition: Transition) {
            }

            override fun onTransitionEnd(transition: Transition) {
                onAnimationEnd?.invoke()
            }
        })
        TransitionManager.beginDelayedTransition(rootLayoutForAnimation, transition)
    }
    ConstraintSet().also {
        it.clone(this@updateConstraintSet.context, layoutId)
        it.applyTo(this@updateConstraintSet)
    }
}