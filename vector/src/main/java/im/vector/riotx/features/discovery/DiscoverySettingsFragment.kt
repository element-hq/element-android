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
package im.vector.riotx.features.discovery

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.identity.ThreePid
import im.vector.riotx.R
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.discovery.change.SetIdentityServerFragment
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

        sharedViewModel.navigateEvent.observe(viewLifecycleOwner, Observer {
            if (it.peekContent().first == DiscoverySharedViewModel.NEW_IDENTITY_SERVER_SET_REQUEST) {
                viewModel.handle(DiscoverySettingsAction.ChangeIdentityServer(it.peekContent().second))
            }
        })

        viewModel.observeViewEvents {
            when (it) {
                is DiscoverySettingsViewEvents.Failure -> {
                    // TODO Snackbar.make(view, throwable.toString(), Snackbar.LENGTH_LONG).show()
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

        //If some 3pids are pending, we can try to check if they have been verified here
        viewModel.handle(DiscoverySettingsAction.Refresh)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        /* TODO
        if (requestCode == TERMS_REQUEST_CODE) {
            if (Activity.RESULT_OK == resultCode) {
                viewModel.refreshModel()
            } else {
                //add some error?
            }
        }

         */
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onSelectIdentityServer() = withState(viewModel) { state ->
        if (state.termsNotSigned) {
            /*
            TODO
            ReviewTermsActivity.intent(requireContext(),
                    TermsManager.ServiceType.IdentityService,
                    SetIdentityServerViewModel.sanitatizeBaseURL(state.identityServer() ?: ""),
                    null).also {
                startActivityForResult(it, TERMS_REQUEST_CODE)
            }

             */
        }
    }

    override fun onTapRevokeEmail(email: String) {
        viewModel.handle(DiscoverySettingsAction.RevokeThreePid(ThreePid.Email(email)))
    }

    override fun onTapShareEmail(email: String) {
        viewModel.handle(DiscoverySettingsAction.ShareThreePid(ThreePid.Email(email)))
    }

    override fun checkEmailVerification(email: String, bind: Boolean) {
        viewModel.handle(DiscoverySettingsAction.FinalizeBind3pid(ThreePid.Email(email), bind))
    }

    override fun checkMsisdnVerification(msisdn: String, code: String, bind: Boolean) {
        viewModel.handle(DiscoverySettingsAction.SubmitMsisdnToken(msisdn, code, bind))
    }

    override fun onTapRevokeMsisdn(msisdn: String) {
        viewModel.handle(DiscoverySettingsAction.RevokeThreePid(ThreePid.Msisdn(msisdn)))
    }

    override fun onTapShareMsisdn(msisdn: String) {
        viewModel.handle(DiscoverySettingsAction.ShareThreePid(ThreePid.Msisdn(msisdn)))
    }

    override fun onTapChangeIdentityServer() = withState(viewModel) { state ->
        //we should prompt if there are bound items with current is
        val pidList = state.emailList().orEmpty() + state.phoneNumbersList().orEmpty()
        val hasBoundIds = pidList.any { it.isShared() == PidInfo.SharedState.SHARED }

        if (hasBoundIds) {
            //we should prompt
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
        //we should prompt if there are bound items with current is
        withState(viewModel) { state ->
            val pidList = state.emailList().orEmpty() + state.phoneNumbersList().orEmpty()
            val hasBoundIds = pidList.any { it.isShared() == PidInfo.SharedState.SHARED }

            if (hasBoundIds) {
                //we should prompt
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.disconnect_identity_server)
                        .setMessage(getString(R.string.settings_discovery_disconnect_with_bound_pid, state.identityServer(), state.identityServer()))
                        .setPositiveButton(R.string._continue) { _, _ -> viewModel.handle(DiscoverySettingsAction.ChangeIdentityServer(null)) }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
            } else {
                viewModel.handle(DiscoverySettingsAction.ChangeIdentityServer(null))
            }
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
