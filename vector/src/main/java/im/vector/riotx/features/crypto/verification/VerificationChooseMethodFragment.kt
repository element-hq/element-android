package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import androidx.transition.AutoTransition
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import butterknife.OnClick
import com.airbnb.mvrx.MvRx
import im.vector.riotx.R
import im.vector.riotx.core.extensions.commitTransaction
import im.vector.riotx.core.platform.VectorBaseFragment
import javax.inject.Inject

class VerificationChooseMethodFragment @Inject constructor() : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_verification_choose_method

//    init {
//        sharedElementEnterTransition = ChangeBounds()
//        sharedElementReturnTransition = ChangeBounds()
//    }

    @OnClick(R.id.verificationByEmojiButton)
    fun test() { //withState(viewModel) { state ->
        getParentCoordinatorLayout()?.let {
            TransitionManager.beginDelayedTransition(it, AutoTransition().apply { duration = 150 })
        }
        parentFragmentManager.commitTransaction {
            //            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            replace(R.id.bottomSheetFragmentContainer,
                    OutgoingVerificationRequestFragment::class.java,
                    Bundle().apply { putString(MvRx.KEY_ARG, "@valere35:matrix.org") },
                    "REQUEST"
            )
        }
    }
}
