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
package im.vector.app.features.discovery

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.discovery.change.SetIdentityServerFragment
import im.vector.app.features.settings.VectorSettingsActivity

import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.terms.TermsService
import javax.inject.Inject

class DiscoverySettingsFragment @Inject constructor(
        private val controller: DiscoverySettingsController,
        val viewModelFactory: DiscoverySettingsViewModel.Factory
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        DiscoverySettingsController.Listener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(DiscoverySettingsViewModel::class)

    lateinit var sharedViewModel: DiscoverySharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(DiscoverySharedViewModel::class.java)

        controller.listener = this
        views.genericRecyclerView.configureWith(controller)

        sharedViewModel.navigateEvent.observeEvent(this) {
            when (it) {
                is DiscoverySharedViewModelAction.ChangeIdentityServer ->
                    viewModel.handle(DiscoverySettingsAction.ChangeIdentityServer(it.newUrl))
            }.exhaustive
        }

        viewModel.observeViewEvents {
            when (it) {
                is DiscoverySettingsViewEvents.Failure -> {
                    displayErrorDialog(it.throwable)
                }
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings_discovery_category)

        // If some 3pids are pending, we can try to check if they have been verified here
        viewModel.handle(DiscoverySettingsAction.Refresh)
    }

    private val termsActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            viewModel.handle(DiscoverySettingsAction.RetrieveBinding)
        } else {
            // add some error?
        }
    }

    override fun openIdentityServerTerms() = withState(viewModel) { state ->
        if (state.termsNotSigned) {
            navigator.openTerms(
                    requireContext(),
                    termsActivityResultLauncher,
                    TermsService.ServiceType.IdentityService,
                    state.identityServer()?.ensureProtocol() ?: "",
                    null)
        }
    }

    override fun onTapRevoke(threePid: ThreePid) {
        viewModel.handle(DiscoverySettingsAction.RevokeThreePid(threePid))
    }

    override fun onTapShare(threePid: ThreePid) {
        viewModel.handle(DiscoverySettingsAction.ShareThreePid(threePid))
    }

    override fun checkEmailVerification(threePid: ThreePid.Email) {
        viewModel.handle(DiscoverySettingsAction.FinalizeBind3pid(threePid))
    }

    override fun sendMsisdnVerificationCode(threePid: ThreePid.Msisdn, code: String) {
        viewModel.handle(DiscoverySettingsAction.SubmitMsisdnToken(threePid, code))
    }

    override fun cancelBinding(threePid: ThreePid) {
        viewModel.handle(DiscoverySettingsAction.CancelBinding(threePid))
    }

    override fun onTapChangeIdentityServer() = withState(viewModel) { state ->
        // we should prompt if there are bound items with current is
        val pidList = state.emailList().orEmpty() + state.phoneNumbersList().orEmpty()
        val hasBoundIds = pidList.any { it.isShared() == SharedState.SHARED }

        if (hasBoundIds) {
            // we should prompt
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.change_identity_server)
                    .setMessage(getString(R.string.settings_discovery_disconnect_with_bound_pid, state.identityServer(), state.identityServer()))
                    .setPositiveButton(R.string._continue) { _, _ -> navigateToChangeIdentityServerFragment() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            Unit
        } else {
            navigateToChangeIdentityServerFragment()
        }
    }

    override fun onTapDisconnectIdentityServer() {
        // we should prompt if there are bound items with current is
        withState(viewModel) { state ->
            val pidList = state.emailList().orEmpty() + state.phoneNumbersList().orEmpty()
            val hasBoundIds = pidList.any { it.isShared() == SharedState.SHARED }

            val message = if (hasBoundIds) {
                getString(R.string.settings_discovery_disconnect_with_bound_pid, state.identityServer(), state.identityServer())
            } else {
                getString(R.string.disconnect_identity_server_dialog_content, state.identityServer())
            }

            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.disconnect_identity_server)
                    .setMessage(message)
                    .setPositiveButton(R.string.disconnect) { _, _ -> viewModel.handle(DiscoverySettingsAction.DisconnectIdentityServer) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    override fun onTapUpdateUserConsent(newValue: Boolean) {
        if (newValue) {
            withState(viewModel) { state ->
                MaterialAlertDialogBuilder(requireActivity())
                        .setTitle(R.string.identity_server_consent_dialog_title)
                        .setMessage(getString(R.string.identity_server_consent_dialog_content, state.identityServer.invoke()))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.handle(DiscoverySettingsAction.UpdateUserConsent(true))
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
            }
        } else {
            viewModel.handle(DiscoverySettingsAction.UpdateUserConsent(false))
        }
    }

    override fun onTapRetryToRetrieveBindings() {
        viewModel.handle(DiscoverySettingsAction.RetrieveBinding)
    }

    private fun navigateToChangeIdentityServerFragment() {
        (vectorBaseActivity as? VectorSettingsActivity)?.navigateTo(SetIdentityServerFragment::class.java)
    }
}
