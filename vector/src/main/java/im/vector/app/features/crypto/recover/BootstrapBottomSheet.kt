/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.crypto.recover

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.commitTransaction
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.toMvRxBundle
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.BottomSheetBootstrapBinding
import im.vector.app.features.auth.ReAuthActivity
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import kotlin.reflect.KClass

@AndroidEntryPoint
class BootstrapBottomSheet : VectorBaseBottomSheetDialogFragment<BottomSheetBootstrapBinding>() {

    @Parcelize
    data class Args(
            val setUpMode: SetupMode = SetupMode.NORMAL
    ) : Parcelable

    override val showExpanded = true

    private val viewModel by fragmentViewModel(BootstrapSharedViewModel::class)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): BottomSheetBootstrapBinding {
        return BottomSheetBootstrapBinding.inflate(inflater, container, false)
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(BootstrapActions.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(BootstrapActions.PasswordAuthDone(password))
                }
                else                    -> {
                    viewModel.handle(BootstrapActions.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(BootstrapActions.ReAuthCancelled)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.observeViewEvents { event ->
            when (event) {
                is BootstrapViewEvents.Dismiss       -> {
                    bottomSheetResult = if (event.success) ResultListener.RESULT_OK else ResultListener.RESULT_CANCEL
                    dismiss()
                }
                is BootstrapViewEvents.ModalError    -> {
                    MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(event.error)
                            .setPositiveButton(R.string.ok, null)
                            .show()
                }
                BootstrapViewEvents.RecoveryKeySaved -> {
                    KeepItSafeDialog().show(requireActivity())
                }
                is BootstrapViewEvents.SkipBootstrap -> {
                    promptSkip()
                }
                is BootstrapViewEvents.RequestReAuth -> {
                    ReAuthActivity.newIntent(requireContext(),
                            event.flowResponse,
                            event.lastErrorCode,
                            getString(R.string.initialize_cross_signing)).let { intent ->
                        reAuthActivityResultLauncher.launch(intent)
                    }
                }
            }
        }
    }

    private fun promptSkip() {
        MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.bootstrap_cancel_text)
                .setPositiveButton(R.string._continue, null)
                .setNegativeButton(R.string.action_skip) { _, _ ->
                    bottomSheetResult = ResultListener.RESULT_CANCEL
                    dismiss()
                }
                .show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialog?.window?.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return rootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnKeyListener { _, keyCode, keyEvent ->
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                    viewModel.handle(BootstrapActions.GoBack)
                    true
                } else {
                    false
                }
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        when (state.step) {
            is BootstrapStep.CheckingMigration           -> {
                views.bootstrapIcon.isVisible = false
                views.bootstrapTitleText.text = getString(R.string.bottom_sheet_setup_secure_backup_title)
                showFragment(BootstrapWaitingFragment::class)
            }
            is BootstrapStep.FirstForm                   -> {
                views.bootstrapIcon.isVisible = false
                views.bootstrapTitleText.text = getString(R.string.bottom_sheet_setup_secure_backup_title)
                showFragment(BootstrapSetupRecoveryKeyFragment::class)
            }
            is BootstrapStep.SetupPassphrase             -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security_phrase_24dp))
                views.bootstrapTitleText.text = getString(R.string.set_a_security_phrase_title)
                showFragment(BootstrapEnterPassphraseFragment::class)
            }
            is BootstrapStep.ConfirmPassphrase           -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security_phrase_24dp))
                views.bootstrapTitleText.text = getString(R.string.set_a_security_phrase_title)
                showFragment(BootstrapConfirmPassphraseFragment::class)
            }
            is BootstrapStep.AccountReAuth               -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user))
                views.bootstrapTitleText.text = getString(R.string.re_authentication_activity_title)
                showFragment(BootstrapReAuthFragment::class)
            }
            is BootstrapStep.Initializing                -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security_key_24dp))
                views.bootstrapTitleText.text = getString(R.string.bootstrap_loading_title)
                showFragment(BootstrapWaitingFragment::class)
            }
            is BootstrapStep.SaveRecoveryKey             -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security_key_24dp))
                views.bootstrapTitleText.text = getString(R.string.bottom_sheet_save_your_recovery_key_title)
                showFragment(BootstrapSaveRecoveryKeyFragment::class)
            }
            is BootstrapStep.DoneSuccess                 -> {
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_security_key_24dp))
                views.bootstrapTitleText.text = getString(R.string.bootstrap_finish_title)
                showFragment(BootstrapConclusionFragment::class)
            }
            is BootstrapStep.GetBackupSecretForMigration -> {
                val isKey = state.step.useKey()
                val drawableRes = if (isKey) R.drawable.ic_security_key_24dp else R.drawable.ic_security_phrase_24dp
                views.bootstrapIcon.isVisible = true
                views.bootstrapIcon.setImageDrawable(ContextCompat.getDrawable(
                        requireContext(),
                        drawableRes)
                )
                views.bootstrapTitleText.text = getString(R.string.upgrade_security)
                showFragment(BootstrapMigrateBackupFragment::class)
            }
        }
        super.invalidate()
    }

    companion object {

        fun show(fragmentManager: FragmentManager, mode: SetupMode): BootstrapBottomSheet {
            return BootstrapBottomSheet().apply {
                isCancelable = false
                setArguments(Args(setUpMode = mode))
            }.also {
                it.show(fragmentManager, "BootstrapBottomSheet")
            }
        }
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
}
