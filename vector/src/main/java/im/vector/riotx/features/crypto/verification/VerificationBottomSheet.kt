package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.commitTransaction
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.core.utils.colorizeMatchingText
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.themes.ThemeUtils
import javax.inject.Inject

class VerificationBottomSheet : VectorBaseBottomSheetDialogFragment() {

    @Inject lateinit var outgoingVerificationRequestViewModelFactory: OutgoingVerificationRequestViewModel.Factory
    @Inject lateinit var verificationViewModelFactory: VerificationBottomSheetViewModel.Factory
    @Inject lateinit var avatarRenderer: AvatarRenderer


    private val viewModel by fragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    @BindView(R.id.verificationRequestName)
    lateinit var otherUserNameText: TextView

    @BindView(R.id.verificationRequestAvatar)
    lateinit var otherUserAvatarImageView: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_verification, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun invalidate() = withState(viewModel) {
        when (it.verificationRequestEvent) {
            is Uninitialized -> {
                if (childFragmentManager.findFragmentByTag("REQUEST") == null) {
                    //Verification not yet started, put outgoing verification
                    childFragmentManager.commitTransaction {
                        setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        replace(R.id.bottomSheetFragmentContainer,
                                OutgoingVerificationRequestFragment::class.java,
                                Bundle().apply { putString(MvRx.KEY_ARG, it.userId) },
                                "REQUEST"
                        )
                    }
                }
            }
        }

        it.otherUserId?.let { matrixItem ->
            val displayName = matrixItem.displayName ?: ""
            otherUserNameText.text = getString(R.string.verification_request_alert_title, displayName)
                    .toSpannable()
                    .colorizeMatchingText(displayName, ThemeUtils.getColor(requireContext(), R.attr.vctr_notice_text_color))

            avatarRenderer.render(matrixItem, otherUserAvatarImageView)
        }

        super.invalidate()
    }
}


fun Fragment.getParentCoordinatorLayout(): CoordinatorLayout? {
    var current = view?.parent as? View
    while (current != null) {
        if (current is CoordinatorLayout) return current
        current = current.parent as? View
    }
    return null
}
