/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.crypto.verification

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import butterknife.BindView
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.crypto.sas.SasVerificationTxState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.commitTransactionNow
import im.vector.riotx.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.riotx.features.home.AvatarRenderer
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.bottom_sheet_verification.*
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass

class VerificationBottomSheet : VectorBaseBottomSheetDialogFragment() {

    @Parcelize
    data class VerificationArgs(
            val otherUserId: String,
            val verificationId: String? = null,
            val roomId: String? = null
    ) : Parcelable

    @Inject
    lateinit var verificationRequestViewModelFactory: VerificationRequestViewModel.Factory
    @Inject
    lateinit var verificationViewModelFactory: VerificationBottomSheetViewModel.Factory
    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private val viewModel by fragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    @BindView(R.id.verificationRequestName)
    lateinit var otherUserNameText: TextView

    @BindView(R.id.verificationRequestAvatar)
    lateinit var otherUserAvatarImageView: ImageView

    override fun getLayoutResId() = R.layout.bottom_sheet_verification

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.requestLiveData.observe(viewLifecycleOwner, Observer {
            it.peekContent().let { va ->
                when (va) {
                    is Success -> {
                        if (va.invoke() is VerificationAction.GotItConclusion) {
                            dismiss()
                        }
                    }
                }
            }
        })
    }

    override fun invalidate() = withState(viewModel) {
        it.otherUserMxItem?.let { matrixItem ->
            otherUserNameText.text = getString(R.string.verification_request_alert_title, matrixItem.getBestName())
            avatarRenderer.render(matrixItem, otherUserAvatarImageView)
        }

        // Did the request result in a SAS transaction?
        if (it.sasTransactionState != null) {
            when (it.sasTransactionState) {
                SasVerificationTxState.None,
                SasVerificationTxState.SendingStart,
                SasVerificationTxState.Started,
                SasVerificationTxState.OnStarted,
                SasVerificationTxState.SendingAccept,
                SasVerificationTxState.Accepted,
                SasVerificationTxState.OnAccepted,
                SasVerificationTxState.SendingKey,
                SasVerificationTxState.KeySent,
                SasVerificationTxState.OnKeyReceived,
                SasVerificationTxState.ShortCodeReady,
                SasVerificationTxState.ShortCodeAccepted,
                SasVerificationTxState.SendingMac,
                SasVerificationTxState.MacSent,
                SasVerificationTxState.Verifying   -> {
                    showFragment(SASVerificationCodeFragment::class, Bundle().apply {
                        putParcelable(MvRx.KEY_ARG, VerificationArgs(
                                it.otherUserMxItem?.id ?: "",
                                it.pendingRequest?.transactionId))
                    })
                }
                SasVerificationTxState.Verified,
                SasVerificationTxState.Cancelled,
                SasVerificationTxState.OnCancelled -> {
                    showFragment(VerificationConclusionFragment::class, Bundle().apply {
                        putParcelable(MvRx.KEY_ARG, VerificationConclusionFragment.Args(
                                it.sasTransactionState == SasVerificationTxState.Verified,
                                it.cancelCode?.value))
                    })
                }
            }

            return@withState
        }

        // At this point there is no transaction for this request

        // Transaction has not yet started
        if (it.pendingRequest?.cancelConclusion != null) {
            // The request has been declined, we should dismiss
            dismiss()
        }

        // If it's an outgoing
        if (it.pendingRequest == null || !it.pendingRequest.isIncoming) {
            Timber.v("## SAS show bottom sheet for outgoing request")
            if (it.pendingRequest?.isReady == true) {
                Timber.v("## SAS show bottom sheet for outgoing and ready request")
                // Show choose method fragment with waiting
                showFragment(VerificationChooseMethodFragment::class, Bundle().apply {
                    putParcelable(MvRx.KEY_ARG, VerificationArgs(it.otherUserMxItem?.id
                            ?: "", it.pendingRequest.transactionId))
                })
            } else {
                // Stay on the start fragment
                showFragment(VerificationRequestFragment::class, Bundle().apply {
                    putParcelable(MvRx.KEY_ARG, VerificationArgs(
                            it.otherUserMxItem?.id ?: "",
                            it.pendingRequest?.transactionId,
                            it.roomId))
                })
            }
        } else if (it.pendingRequest.isIncoming) {
            Timber.v("## SAS show bottom sheet for Incoming request")
            // For incoming we can switch to choose method because ready is being sent or already sent
            showFragment(VerificationChooseMethodFragment::class, Bundle().apply {
                putParcelable(MvRx.KEY_ARG, VerificationArgs(it.otherUserMxItem?.id
                        ?: "", it.pendingRequest.transactionId))
            })
        }
        super.invalidate()
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, bundle: Bundle) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            // We want to animate the bottomsheet bound changes
            bottomSheetFragmentContainer.getParentCoordinatorLayout()?.let { coordinatorLayout ->
                TransitionManager.beginDelayedTransition(coordinatorLayout, AutoTransition().apply { duration = 150 })
            }
            // Commit now, to ensure changes occurs before next rendering frame (or bottomsheet want animate)
            childFragmentManager.commitTransactionNow {
                replace(R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        bundle,
                        fragmentClass.simpleName
                )
            }
        }
    }

    companion object {
        fun withArgs(roomId: String, otherUserId: String, transactionId: String? = null): VerificationBottomSheet {
            return VerificationBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(MvRx.KEY_ARG, VerificationBottomSheet.VerificationArgs(
                            otherUserId = otherUserId,
                            roomId = roomId,
                            verificationId = transactionId
                    ))
                }
            }
        }
    }
}

fun View.getParentCoordinatorLayout(): CoordinatorLayout? {
    var current = this as? View
    while (current != null) {
        if (current is CoordinatorLayout) return current
        current = current.parent as? View
    }
    return null
}
