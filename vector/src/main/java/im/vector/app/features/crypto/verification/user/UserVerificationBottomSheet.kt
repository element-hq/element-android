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

package im.vector.app.features.crypto.verification.user

import android.os.Bundle
import android.os.Parcelable
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
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetVerificationBinding
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewEvents
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Specific to other users verification (not self verification).
 */
@AndroidEntryPoint
class UserVerificationBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetVerificationBinding>() {
    @Parcelize
    data class Args(
            val otherUserId: String,
            val verificationId: String? = null,
            // user verifications happen in DMs
            val roomId: String? = null,
    ) : Parcelable

    override val showExpanded = true

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private val viewModel by fragmentViewModel(UserVerificationViewModel::class)

    override fun getBinding(
            inflater: LayoutInflater,
            container: ViewGroup?
    ) = BottomSheetVerificationBinding.inflate(inflater, container, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFragment(UserVerificationFragment::class)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents { event ->
            when (event) {
                VerificationBottomSheetViewEvents.AccessSecretStore -> {
                    // nop for user verification?
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
                VerificationBottomSheetViewEvents.ResetAll -> {
                    // no-op for user verification
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        avatarRenderer.render(state.otherUserMxItem, views.otherUserAvatarImageView)
        views.otherUserNameText.text = getString(R.string.verification_verify_user, state.otherUserMxItem.getBestName())
        views.otherUserShield.render(
                if (state.otherUserIsTrusted) RoomEncryptionTrustLevel.Trusted
                else RoomEncryptionTrustLevel.Default
        )
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
        fun verifyUser(otherUserId: String, transactionId: String? = null): UserVerificationBottomSheet {
            return UserVerificationBottomSheet().apply {
                setArguments(
                        Args(
                                otherUserId = otherUserId,
                                verificationId = transactionId
                        )
                )
            }
        }
    }
}
