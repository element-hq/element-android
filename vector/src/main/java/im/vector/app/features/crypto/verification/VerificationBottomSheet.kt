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
package im.vector.app.features.crypto.verification

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetVerificationBinding
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import im.vector.app.features.crypto.verification.cancel.VerificationCancelFragment
import im.vector.app.features.crypto.verification.cancel.VerificationNotMeFragment
import im.vector.app.features.crypto.verification.choose.VerificationChooseMethodFragment
import im.vector.app.features.crypto.verification.conclusion.VerificationConclusionFragment
import im.vector.app.features.crypto.verification.emoji.VerificationEmojiCodeFragment
import im.vector.app.features.crypto.verification.qrconfirmation.VerificationQRWaitingFragment
import im.vector.app.features.crypto.verification.qrconfirmation.VerificationQrScannedByOtherFragment
import im.vector.app.features.crypto.verification.request.VerificationRequestFragment
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.settings.VectorSettingsActivity
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import org.matrix.android.sdk.api.session.crypto.verification.CancelCode
import org.matrix.android.sdk.api.session.crypto.verification.VerificationTxState
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class VerificationBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetVerificationBinding>() {

    @Parcelize
    data class VerificationArgs(
            val otherUserId: String,
            val verificationId: String? = null,
            val verificationLocalId: String? = null,
            val roomId: String? = null,
            // Special mode where UX should show loading wheel until other session sends a request/tx
            val selfVerificationMode: Boolean = false
    ) : Parcelable

    override val showExpanded = true

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private val viewModel by fragmentViewModel(VerificationBottomSheetViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetVerificationBinding {
        return BottomSheetVerificationBinding.inflate(inflater, container, false)
    }

    init {
        isCancelable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.observeViewEvents {
            when (it) {
                is VerificationBottomSheetViewEvents.Dismiss           -> dismiss()
                is VerificationBottomSheetViewEvents.AccessSecretStore -> {
                    secretStartForActivityResult.launch(SharedSecureStorageActivity.newIntent(
                            requireContext(),
                            null, // use default key
                            listOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME),
                            SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS
                    ))
                }
                is VerificationBottomSheetViewEvents.ModalError        -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_title_error))
                            .setMessage(it.errorMessage)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    Unit
                }
                VerificationBottomSheetViewEvents.GoToSettings         -> {
                    dismiss()
                    (activity as? VectorBaseActivity<*>)?.let { activity ->
                        activity.navigator.openSettings(activity, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY)
                    }
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { _, keyCode, keyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                    viewModel.queryCancel()
                    true
                } else {
                    false
                }
            }
        }
    }

    private val secretStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = activityResult.data?.getStringExtra(SharedSecureStorageActivity.EXTRA_DATA_RESULT)
            val reset = activityResult.data?.getBooleanExtra(SharedSecureStorageActivity.EXTRA_DATA_RESET, false) ?: false
            if (result != null) {
                viewModel.handle(VerificationAction.GotResultFromSsss(result, SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS))
            } else if (reset) {
                // all have been reset, so we are verified?
                viewModel.handle(VerificationAction.SecuredStorageHasBeenReset)
            }
        } else {
            viewModel.handle(VerificationAction.CancelledFromSsss)
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        state.otherUserMxItem?.let { matrixItem ->
            if (state.isMe) {
                avatarRenderer.render(matrixItem, views.otherUserAvatarImageView)
                if (state.sasTransactionState == VerificationTxState.Verified ||
                        state.qrTransactionState == VerificationTxState.Verified ||
                        state.verifiedFromPrivateKeys) {
                    views.otherUserShield.render(RoomEncryptionTrustLevel.Trusted)
                } else {
                    views.otherUserShield.render(RoomEncryptionTrustLevel.Warning)
                }
                views.otherUserNameText.text = getString(
                        if (state.selfVerificationMode) R.string.crosssigning_verify_this_session else R.string.crosssigning_verify_session
                )
            } else {
                avatarRenderer.render(matrixItem, views.otherUserAvatarImageView)

                if (state.sasTransactionState == VerificationTxState.Verified || state.qrTransactionState == VerificationTxState.Verified) {
                    views.otherUserNameText.text = getString(R.string.verification_verified_user, matrixItem.getBestName())
                    views.otherUserShield.render(RoomEncryptionTrustLevel.Trusted)
                } else {
                    views.otherUserNameText.text = getString(R.string.verification_verify_user, matrixItem.getBestName())
                    views.otherUserShield.render(null)
                }
            }
        }

        if (state.quadSHasBeenReset) {
            showFragment(
                    VerificationConclusionFragment::class,
                    VerificationConclusionFragment.Args(
                            isSuccessFull = true,
                            isMe = true,
                            cancelReason = null
                    ))
            return@withState
        }

        if (state.userThinkItsNotHim) {
            views.otherUserNameText.text = getString(R.string.dialog_title_warning)
            showFragment(VerificationNotMeFragment::class)
            return@withState
        }

        if (state.userWantsToCancel) {
            views.otherUserNameText.text = getString(R.string.are_you_sure)
            showFragment(VerificationCancelFragment::class)
            return@withState
        }

        if (state.selfVerificationMode && state.verifyingFrom4S) {
            showFragment(QuadSLoadingFragment::class)
            return@withState
        }
        if (state.selfVerificationMode && state.verifiedFromPrivateKeys) {
            showFragment(
                    VerificationConclusionFragment::class,
                    VerificationConclusionFragment.Args(true, null, state.isMe)
            )
            return@withState
        }

        // Did the request result in a SAS transaction?
        if (state.sasTransactionState != null) {
            when (state.sasTransactionState) {
                is VerificationTxState.None,
                is VerificationTxState.SendingStart,
                is VerificationTxState.Started,
                is VerificationTxState.OnStarted,
                is VerificationTxState.SendingAccept,
                is VerificationTxState.Accepted,
                is VerificationTxState.OnAccepted,
                is VerificationTxState.SendingKey,
                is VerificationTxState.KeySent,
                is VerificationTxState.OnKeyReceived,
                is VerificationTxState.ShortCodeReady,
                is VerificationTxState.ShortCodeAccepted,
                is VerificationTxState.SendingMac,
                is VerificationTxState.MacSent,
                is VerificationTxState.Verifying -> {
                    showFragment(
                            VerificationEmojiCodeFragment::class,
                            VerificationArgs(
                                    state.otherUserMxItem?.id ?: "",
                                    // If it was outgoing it.transaction id would be null, but the pending request
                                    // would be updated (from localId to txId)
                                    state.pendingRequest.invoke()?.transactionId ?: state.transactionId
                            )
                    )
                }
                is VerificationTxState.Verified  -> {
                    showFragment(
                            VerificationConclusionFragment::class,
                            VerificationConclusionFragment.Args(true, null, state.isMe)
                    )
                }
                is VerificationTxState.Cancelled -> {
                    showFragment(
                            VerificationConclusionFragment::class,
                            VerificationConclusionFragment.Args(false, state.sasTransactionState.cancelCode.value, state.isMe)
                    )
                }
                else                             -> Unit
            }

            return@withState
        }

        when (state.qrTransactionState) {
            is VerificationTxState.QrScannedByOther               -> {
                showFragment(VerificationQrScannedByOtherFragment::class)
                return@withState
            }
            is VerificationTxState.Started,
            is VerificationTxState.WaitingOtherReciprocateConfirm -> {
                showFragment(
                        VerificationQRWaitingFragment::class,
                        VerificationQRWaitingFragment.Args(
                                isMe = state.isMe,
                                otherUserName = state.otherUserMxItem?.getBestName() ?: ""
                        )
                )
                return@withState
            }
            is VerificationTxState.Verified                       -> {
                showFragment(
                        VerificationConclusionFragment::class,
                        VerificationConclusionFragment.Args(true, null, state.isMe)
                )
                return@withState
            }
            is VerificationTxState.Cancelled                      -> {
                showFragment(
                        VerificationConclusionFragment::class,
                        VerificationConclusionFragment.Args(false, state.qrTransactionState.cancelCode.value, state.isMe)
                )
                return@withState
            }
            else                                                  -> Unit
        }

        // At this point there is no SAS transaction for this request

        // Transaction has not yet started
        if (state.pendingRequest.invoke()?.cancelConclusion != null) {
            // The request has been declined, we should dismiss
            views.otherUserNameText.text = getString(R.string.verification_cancelled)
            showFragment(
                    VerificationConclusionFragment::class,
                    VerificationConclusionFragment.Args(
                            isSuccessFull = false,
                            cancelReason = state.pendingRequest.invoke()?.cancelConclusion?.value ?: CancelCode.User.value,
                            isMe = state.isMe
                    )
            )
            return@withState
        }

        // If it's an outgoing
        if (state.pendingRequest.invoke() == null || state.pendingRequest.invoke()?.isIncoming == false || state.selfVerificationMode) {
            Timber.v("## SAS show bottom sheet for outgoing request")
            if (state.pendingRequest.invoke()?.isReady == true) {
                Timber.v("## SAS show bottom sheet for outgoing and ready request")
                // Show choose method fragment with waiting
                showFragment(
                        VerificationChooseMethodFragment::class,
                        VerificationArgs(
                                otherUserId = state.otherUserMxItem?.id ?: "",
                                verificationId = state.pendingRequest.invoke()?.transactionId
                        )
                )
            } else {
                // Stay on the start fragment
                showFragment(
                        VerificationRequestFragment::class,
                        VerificationArgs(
                                otherUserId = state.otherUserMxItem?.id ?: "",
                                verificationId = state.pendingRequest.invoke()?.transactionId,
                                verificationLocalId = state.roomId
                        )
                )
            }
        } else if (state.pendingRequest.invoke()?.isIncoming == true) {
            Timber.v("## SAS show bottom sheet for Incoming request")
            // For incoming we can switch to choose method because ready is being sent or already sent
            showFragment(
                    VerificationChooseMethodFragment::class,
                    VerificationArgs(
                            otherUserId = state.otherUserMxItem?.id ?: "",
                            verificationId = state.pendingRequest.invoke()?.transactionId
                    )
            )
        }
        super.invalidate()
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, argsParcelable: Parcelable? = null) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        argsParcelable?.toMvRxBundle(),
                        fragmentClass.simpleName
                )
            }
        }
    }

    companion object {
        fun withArgs(roomId: String?, otherUserId: String, transactionId: String? = null): VerificationBottomSheet {
            return VerificationBottomSheet().apply {
                setArguments(VerificationArgs(
                        otherUserId = otherUserId,
                        roomId = roomId,
                        verificationId = transactionId,
                        selfVerificationMode = false
                ))
            }
        }

        fun forSelfVerification(session: Session): VerificationBottomSheet {
            return VerificationBottomSheet().apply {
                setArguments(VerificationArgs(
                        otherUserId = session.myUserId,
                        selfVerificationMode = true
                ))
            }
        }

        fun forSelfVerification(session: Session, outgoingRequest: String): VerificationBottomSheet {
            return VerificationBottomSheet().apply {
                setArguments(VerificationArgs(
                        otherUserId = session.myUserId,
                        selfVerificationMode = true,
                        verificationId = outgoingRequest
                ))
            }
        }

        const val WAITING_SELF_VERIF_TAG: String = "WAITING_SELF_VERIF_TAG"
    }
}

// fun View.getParentCoordinatorLayout(): CoordinatorLayout? {
//    var current = this as? View
//    while (current != null) {
//        if (current is CoordinatorLayout) return current
//        current = current.parent as? View
//    }
//    return null
// }
