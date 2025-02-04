/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.threepids

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.getFormattedValue
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.isMsisdn
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.auth.ReAuthActivity
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import org.matrix.android.sdk.api.extensions.isEmail
import org.matrix.android.sdk.api.session.identity.ThreePid
import javax.inject.Inject

@AndroidEntryPoint
class ThreePidsSettingsFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        OnBackPressed,
        ThreePidsSettingsController.InteractionListener {

    @Inject lateinit var epoxyController: ThreePidsSettingsController

    private val viewModel: ThreePidsSettingsViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        views.genericRecyclerView.configureWith(epoxyController)
        epoxyController.interactionListener = this

        viewModel.observeViewEvents {
            when (it) {
                is ThreePidsSettingsViewEvents.Failure -> displayErrorDialog(it.throwable)
                is ThreePidsSettingsViewEvents.RequestReAuth -> askAuthentication(it)
            }
        }
    }

    private fun askAuthentication(event: ThreePidsSettingsViewEvents.RequestReAuth) {
        ReAuthActivity.newIntent(
                requireContext(),
                event.registrationFlowResponse,
                event.lastErrorCode,
                getString(CommonStrings.settings_add_email_address)
        ).let { intent ->
            reAuthActivityResultLauncher.launch(intent)
        }
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(ThreePidsSettingsAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(ThreePidsSettingsAction.PasswordAuthDone(password))
                }
                else -> {
                    viewModel.handle(ThreePidsSettingsAction.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(ThreePidsSettingsAction.ReAuthCancelled)
        }
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        epoxyController.interactionListener = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.settings_emails_and_phone_numbers_title)
    }

    override fun invalidate() = withState(viewModel) { state ->
        if (state.isLoading) {
            showLoadingDialog()
        } else {
            dismissLoadingDialog()
        }
        epoxyController.setData(state)
    }

    override fun addEmail() {
        viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingEmail(null)))
    }

    override fun doAddEmail(email: String) {
        // Sanity
        val safeEmail = email.trim().replace(" ", "")
        viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingEmail(null)))

        // Check that email is valid
        if (!safeEmail.isEmail()) {
            viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingEmail(getString(CommonStrings.auth_invalid_email))))
            return
        }

        viewModel.handle(ThreePidsSettingsAction.AddThreePid(ThreePid.Email(safeEmail)))
    }

    override fun addMsisdn() {
        viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingPhoneNumber(null)))
    }

    override fun doAddMsisdn(msisdn: String) {
        // Sanity
        val safeMsisdn = msisdn.trim().replace(" ", "")

        viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingPhoneNumber(null)))

        // Check that phone number is valid
        if (!msisdn.startsWith("+")) {
            viewModel.handle(
                    ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingPhoneNumber(
                            getString(CommonStrings.login_msisdn_error_not_international))
                    )
            )
            return
        }

        if (!msisdn.isMsisdn()) {
            viewModel.handle(
                    ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.AddingPhoneNumber(getString(CommonStrings.login_msisdn_error_other)))
            )
            return
        }

        viewModel.handle(ThreePidsSettingsAction.AddThreePid(ThreePid.Msisdn(safeMsisdn)))
    }

    override fun submitCode(threePid: ThreePid.Msisdn, code: String) {
        viewModel.handle(ThreePidsSettingsAction.SubmitCode(threePid, code))
        // Hide the keyboard
        view?.hideKeyboard()
    }

    override fun cancelAdding() {
        viewModel.handle(ThreePidsSettingsAction.ChangeUiState(ThreePidsSettingsUiState.Idle))
        // Hide the keyboard
        view?.hideKeyboard()
    }

    override fun continueThreePid(threePid: ThreePid) {
        viewModel.handle(ThreePidsSettingsAction.ContinueThreePid(threePid))
    }

    override fun cancelThreePid(threePid: ThreePid) {
        viewModel.handle(ThreePidsSettingsAction.CancelThreePid(threePid))
    }

    override fun deleteThreePid(threePid: ThreePid) {
        MaterialAlertDialogBuilder(requireActivity(), im.vector.lib.ui.styles.R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setMessage(getString(CommonStrings.settings_remove_three_pid_confirmation_content, threePid.getFormattedValue()))
                .setPositiveButton(CommonStrings.action_remove) { _, _ ->
                    viewModel.handle(ThreePidsSettingsAction.DeleteThreePid(threePid))
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return withState(viewModel) {
            if (it.uiState is ThreePidsSettingsUiState.Idle) {
                false
            } else {
                cancelAdding()
                true
            }
        }
    }
}
