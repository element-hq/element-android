/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.user

import android.app.Dialog
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.toSpannable
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.databinding.BottomSheetVerificationBinding
import im.vector.app.features.crypto.verification.VerificationAction
import im.vector.app.features.crypto.verification.VerificationBottomSheetViewEvents
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
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

    init {
        // we manage dismiss/back manually to confirm cancel on verification
        isCancelable = false
    }

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
                            .setTitle(getString(CommonStrings.dialog_title_error))
                            .setMessage(event.errorMessage)
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings.ok, null)
                            .show()
                }
                VerificationBottomSheetViewEvents.ResetAll,
                VerificationBottomSheetViewEvents.DismissAndOpenDeviceSettings -> {
                    // no-op for user verification
                }
                is VerificationBottomSheetViewEvents.RequestNotFound -> {
                    // no-op for user verification
                }
                is VerificationBottomSheetViewEvents.ConfirmCancel -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(CommonStrings.dialog_title_confirmation))
                            .setMessage(
                                    getString(CommonStrings.verify_cancel_other, event.otherUserId, event.deviceId ?: "*")
                                            .toSpannable()
                                            .colorizeMatchingText(
                                                    event.otherUserId,
                                                    ThemeUtils.getColor(requireContext(), im.vector.lib.ui.styles.R.attr.vctr_notice_text_color)
                                            )
                            )
                            .setCancelable(false)
                            .setPositiveButton(CommonStrings._resume, null)
                            .setNegativeButton(CommonStrings.action_cancel) { _, _ ->
                                viewModel.handle(VerificationAction.CancelPendingVerification)
                            }
                            .show()
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

    override fun invalidate() = withState(viewModel) { state ->
        avatarRenderer.render(state.otherUserMxItem, views.otherUserAvatarImageView)
        views.otherUserNameText.text = getString(CommonStrings.verification_verify_user, state.otherUserMxItem.getBestName())
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
