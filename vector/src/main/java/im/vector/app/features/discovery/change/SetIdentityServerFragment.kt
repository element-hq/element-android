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
package im.vector.app.features.discovery.change

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.features.discovery.DiscoverySharedViewModel
import kotlinx.android.synthetic.main.fragment_set_identity_server.*
import org.matrix.android.sdk.api.session.terms.TermsService
import javax.inject.Inject

class SetIdentityServerFragment @Inject constructor(
        val viewModelFactory: SetIdentityServerViewModel.Factory,
        val colorProvider: ColorProvider
) : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_set_identity_server

    private val viewModel by fragmentViewModel(SetIdentityServerViewModel::class)

    lateinit var sharedViewModel: DiscoverySharedViewModel

    override fun invalidate() = withState(viewModel) { state ->
        if (state.defaultIdentityServerUrl.isNullOrEmpty()) {
            // No default
            identityServerSetDefaultNotice.isVisible = false
            identityServerSetDefaultSubmit.isVisible = false
            identityServerSetDefaultAlternative.setText(R.string.identity_server_set_alternative_notice_no_default)
        } else {
            identityServerSetDefaultNotice.text = getString(
                    R.string.identity_server_set_default_notice,
                    state.homeServerUrl.toReducedUrl(),
                    state.defaultIdentityServerUrl.toReducedUrl()
            )
                    .toSpannable()
                    .colorizeMatchingText(state.defaultIdentityServerUrl.toReducedUrl(),
                            colorProvider.getColorFromAttribute(R.attr.riotx_text_primary_body_contrast))

            identityServerSetDefaultNotice.isVisible = true
            identityServerSetDefaultSubmit.isVisible = true
            identityServerSetDefaultSubmit.text = getString(R.string.identity_server_set_default_submit, state.defaultIdentityServerUrl.toReducedUrl())
            identityServerSetDefaultAlternative.setText(R.string.identity_server_set_alternative_notice)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(DiscoverySharedViewModel::class.java)

        identityServerSetDefaultAlternativeTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.handle(SetIdentityServerAction.UseCustomIdentityServer(identityServerSetDefaultAlternativeTextInput.text.toString()))
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        identityServerSetDefaultAlternativeTextInput
                .textChanges()
                .subscribe {
                    identityServerSetDefaultAlternativeTil.error = null
                    identityServerSetDefaultAlternativeSubmit.isEnabled = it.isNotEmpty()
                }
                .disposeOnDestroyView()

        identityServerSetDefaultSubmit.debouncedClicks {
            viewModel.handle(SetIdentityServerAction.UseDefaultIdentityServer)
        }

        identityServerSetDefaultAlternativeSubmit.debouncedClicks {
            viewModel.handle(SetIdentityServerAction.UseCustomIdentityServer(identityServerSetDefaultAlternativeTextInput.text.toString()))
        }

        viewModel.observeViewEvents {
            when (it) {
                is SetIdentityServerViewEvents.Loading       -> showLoading(it.message)
                is SetIdentityServerViewEvents.Failure       -> handleFailure(it)
                is SetIdentityServerViewEvents.OtherFailure  -> showFailure(it.failure)
                is SetIdentityServerViewEvents.NoTerms       -> {
                    AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.settings_discovery_no_terms_title)
                            .setMessage(R.string.settings_discovery_no_terms)
                            .setPositiveButton(R.string._continue) { _, _ ->
                                processIdentityServerChange()
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    Unit
                }
                is SetIdentityServerViewEvents.TermsAccepted -> processIdentityServerChange()
                is SetIdentityServerViewEvents.ShowTerms     -> {
                    navigator.openTerms(
                            requireContext(),
                            termsActivityResultLauncher,
                            TermsService.ServiceType.IdentityService,
                            it.identityServerUrl,
                            null)
                }
            }.exhaustive
        }
    }

    private fun handleFailure(failure: SetIdentityServerViewEvents.Failure) {
        val message = getString(failure.errorMessageId)
        if (failure.forDefault) {
            // Display the error in a dialog
            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok, null)
                    .show()
        } else {
            // Display the error inlined
            identityServerSetDefaultAlternativeTil.error = message
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.identity_server)
    }

    private val termsActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            processIdentityServerChange()
        } else {
            // add some error?
        }
    }

    private fun processIdentityServerChange() {
        viewModel.currentWantedUrl?.let {
            sharedViewModel.requestChangeToIdentityServer(it)
            parentFragmentManager.popBackStack()
        }
    }
}
