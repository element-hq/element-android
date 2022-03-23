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
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.core.utils.showIdentityServerConsentDialog
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.discovery.change.SetIdentityServerFragment
import im.vector.app.features.navigation.SettingsActivityPayload
import im.vector.app.features.settings.VectorSettingsActivity
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.terms.TermsService
import javax.inject.Inject

class DiscoverySettingsFragment @Inject constructor(
        private val controller: DiscoverySettingsController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        DiscoverySettingsController.Listener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(DiscoverySettingsViewModel::class)
    private val discoveryArgs: SettingsActivityPayload.DiscoverySettings by args()

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
            }
        }

        viewModel.observeViewEvents {
            when (it) {
                is DiscoverySettingsViewEvents.Failure -> {
                    displayErrorDialog(it.throwable)
                }
            }
        }
        if (discoveryArgs.expandIdentityPolicies) {
            viewModel.handle(DiscoverySettingsAction.SetPoliciesExpandState(expanded = true))
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
                    state.identityServer()?.serverUrl?.ensureProtocol() ?: "",
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
                    .setNegativeButton(R.string.action_cancel, null)
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

            val serverUrl = state.identityServer()?.serverUrl.orEmpty()
            val message = if (hasBoundIds) {
                getString(R.string.settings_discovery_disconnect_with_bound_pid, serverUrl, serverUrl)
            } else {
                getString(R.string.disconnect_identity_server_dialog_content, serverUrl)
            }

            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.disconnect_identity_server)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_disconnect) { _, _ -> viewModel.handle(DiscoverySettingsAction.DisconnectIdentityServer) }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
        }
    }

    override fun onTapUpdateUserConsent(newValue: Boolean) {
        if (newValue) {
            withState(viewModel) { state ->
                requireContext().showIdentityServerConsentDialog(
                        state.identityServer.invoke(),
                        consentCallBack = { viewModel.handle(DiscoverySettingsAction.UpdateUserConsent(true)) }
                )
            }
        } else {
            viewModel.handle(DiscoverySettingsAction.UpdateUserConsent(false))
        }
    }

    override fun onTapRetryToRetrieveBindings() {
        viewModel.handle(DiscoverySettingsAction.RetrieveBinding)
    }

    override fun onPolicyUrlsExpandedStateToggled(newExpandedState: Boolean) {
        viewModel.handle(DiscoverySettingsAction.SetPoliciesExpandState(expanded = newExpandedState))
    }

    override fun onPolicyTapped(policy: ServerPolicy) {
        openUrlInChromeCustomTab(requireContext(), null, policy.url)
    }

    private fun navigateToChangeIdentityServerFragment() {
        (vectorBaseActivity as? VectorSettingsActivity)?.navigateTo(SetIdentityServerFragment::class.java)
    }
}
