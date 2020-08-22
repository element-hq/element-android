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
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.observeEvent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.features.discovery.change.SetIdentityServerFragment
import im.vector.app.features.terms.ReviewTermsActivity
import org.matrix.android.sdk.api.session.identity.SharedState
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.terms.TermsService
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import javax.inject.Inject

class DiscoverySettingsFragment @Inject constructor(
        private val controller: DiscoverySettingsController,
        val viewModelFactory: DiscoverySettingsViewModel.Factory
) : VectorBaseFragment(), DiscoverySettingsController.Listener {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    private val viewModel by fragmentViewModel(DiscoverySettingsViewModel::class)

    lateinit var sharedViewModel: DiscoverySharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(DiscoverySharedViewModel::class.java)

        controller.listener = this
        recyclerView.configureWith(controller)

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
        recyclerView.cleanup()
        controller.listener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_discovery_category)

        // If some 3pids are pending, we can try to check if they have been verified here
        viewModel.handle(DiscoverySettingsAction.Refresh)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ReviewTermsActivity.TERMS_REQUEST_CODE) {
            if (Activity.RESULT_OK == resultCode) {
                viewModel.handle(DiscoverySettingsAction.RetrieveBinding)
            } else {
                // add some error?
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun openIdentityServerTerms() = withState(viewModel) { state ->
        if (state.termsNotSigned) {
            navigator.openTerms(
                    this,
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
            AlertDialog.Builder(requireActivity())
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

            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.disconnect_identity_server)
                    .setMessage(message)
                    .setPositiveButton(R.string.disconnect) { _, _ -> viewModel.handle(DiscoverySettingsAction.DisconnectIdentityServer) }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    override fun onTapRetryToRetrieveBindings() {
        viewModel.handle(DiscoverySettingsAction.RetrieveBinding)
    }

    private fun navigateToChangeIdentityServerFragment() {
        parentFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.anim_slide_in_bottom, R.anim.anim_slide_out_bottom, R.anim.anim_slide_in_bottom, R.anim.anim_slide_out_bottom)
                .replace(R.id.vector_settings_page, SetIdentityServerFragment::class.java, null)
                .addToBackStack(null)
                .commit()
    }
}
