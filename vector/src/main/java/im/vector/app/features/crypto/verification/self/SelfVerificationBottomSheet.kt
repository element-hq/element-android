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

package im.vector.app.features.crypto.verification.self

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetVerificationBinding
import im.vector.app.features.crypto.quads.SharedSecureStorageActivity
import im.vector.app.features.crypto.quads.SharedSecureStorageViewState
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewEvents
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.crosssigning.KEYBACKUP_SECRET_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.MASTER_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.SELF_SIGNING_KEY_SSSS_NAME
import org.matrix.android.sdk.api.session.crypto.crosssigning.USER_SIGNING_KEY_SSSS_NAME
import kotlin.reflect.KClass

class SelfVerificationBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetVerificationBinding>() {

    override val showExpanded = true

    @Parcelize
    data class Args(
            // when verifying a new session from an existing safe one
            val targetDevice: String? = null,
            // when we started from an incoming request
            val transactionId: String? = null,
    ) : Parcelable

    private val viewModel by fragmentViewModel(SelfVerificationViewModel::class)

    override fun getBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
    ) = BottomSheetVerificationBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFragment(SelfVerificationFragment::class)
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents { event ->
            when (event) {
                VerificationBottomSheetViewEvents.AccessSecretStore -> {
                    secretStartForActivityResult.launch(
                            SharedSecureStorageActivity.newReadIntent(
                                    requireContext(),
                                    null, // use default key
                                    listOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME),
                                    SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS,
                            )
                    )
                }
                VerificationBottomSheetViewEvents.ResetAll -> {
                    secretStartForActivityResult.launch(
                            SharedSecureStorageActivity.newReadIntent(
                                    requireContext(),
                                    null, // use default key
                                    listOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME),
                                    SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS,
                                    SharedSecureStorageViewState.Step.ResetAll
                            )
                    )
                }
                VerificationBottomSheetViewEvents.Dismiss -> {
                    dismiss()
                }
                VerificationBottomSheetViewEvents.GoToSettings -> {
                    // nop for user verificaiton
                }
                is VerificationBottomSheetViewEvents.ModalError -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.dialog_title_error))
                            .setMessage(event.errorMessage)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
            }
        }
    }

    //        viewModel.observeViewEvents {
//            when (it) {
//                is VerificationBottomSheetViewEvents.Dismiss -> dismiss()
//                is VerificationBottomSheetViewEvents.AccessSecretStore -> {
//                    secretStartForActivityResult.launch(
//                            SharedSecureStorageActivity.newReadIntent(
//                                    requireContext(),
//                                    null, // use default key
//                                    listOf(MASTER_KEY_SSSS_NAME, USER_SIGNING_KEY_SSSS_NAME, SELF_SIGNING_KEY_SSSS_NAME, KEYBACKUP_SECRET_SSSS_NAME),
//                                    SharedSecureStorageActivity.DEFAULT_RESULT_KEYSTORE_ALIAS
//                            )
//                    )
//                }
//                is VerificationBottomSheetViewEvents.ModalError -> {
//                    MaterialAlertDialogBuilder(requireContext())
//                            .setTitle(getString(R.string.dialog_title_error))
//                            .setMessage(it.errorMessage)
//                            .setCancelable(false)
//                            .setPositiveButton(R.string.ok, null)
//                            .show()
//                    Unit
//                }
//                VerificationBottomSheetViewEvents.GoToSettings -> {
//                    dismiss()
//                    (activity as? VectorBaseActivity<*>)?.let { activity ->
//                        activity.navigator.openSettings(activity, VectorSettingsActivity.EXTRA_DIRECT_ACCESS_SECURITY_PRIVACY)
//                    }
//                }
//            }
//        }
//    }

    override fun invalidate() = withState(viewModel) { state ->
//        avatarRenderer.render(state.otherUserMxItem, views.otherUserAvatarImageView)
        views.otherUserShield.isVisible = false
        if (state.isThisSessionVerified) {
            views.otherUserAvatarImageView.setImageResource(
                    R.drawable.ic_shield_trusted
            )
            views.otherUserNameText.text = getString(R.string.verification_profile_verify)
        } else {
            views.otherUserAvatarImageView.setImageResource(
                    R.drawable.ic_shield_black
            )
            views.otherUserNameText.text = getString(R.string.crosssigning_verify_this_session)
        }

        super.invalidate()
    }

    private fun showFragment(fragmentClass: KClass<out Fragment>, argsParcelable: Parcelable? = null) {
        if (childFragmentManager.findFragmentByTag(fragmentClass.simpleName) == null) {
            childFragmentManager.commitTransaction {
                replace(
                        R.id.bottomSheetFragmentContainer,
                        fragmentClass.java,
                        argsParcelable?.toMvRxBundle(),
                        fragmentClass.simpleName
                )
            }
        }
    }

    companion object {
        fun verifyOwnUntrustedDevice(): SelfVerificationBottomSheet {
            return SelfVerificationBottomSheet().apply {
                setArguments(
                        Args()
                )
            }
        }

        fun forTransaction(transactionId: String): SelfVerificationBottomSheet {
            return SelfVerificationBottomSheet().apply {
                setArguments(
                        Args(transactionId = transactionId)
                )
            }
        }
    }
}