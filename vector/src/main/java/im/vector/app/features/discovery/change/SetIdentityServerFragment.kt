/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.discovery.change

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.colorizeMatchingText
import im.vector.app.databinding.FragmentSetIdentityServerBinding
import im.vector.app.features.discovery.DiscoverySharedViewModel
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.session.terms.TermsService
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@AndroidEntryPoint
class SetIdentityServerFragment :
        VectorBaseFragment<FragmentSetIdentityServerBinding>() {

    @Inject lateinit var colorProvider: ColorProvider

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSetIdentityServerBinding {
        return FragmentSetIdentityServerBinding.inflate(inflater, container, false)
    }

    private val viewModel by fragmentViewModel(SetIdentityServerViewModel::class)

    lateinit var sharedViewModel: DiscoverySharedViewModel

    override fun invalidate() = withState(viewModel) { state ->
        if (state.defaultIdentityServerUrl.isNullOrEmpty()) {
            // No default
            views.identityServerSetDefaultNotice.isVisible = false
            views.identityServerSetDefaultSubmit.isVisible = false
            views.identityServerSetDefaultAlternative.setText(CommonStrings.identity_server_set_alternative_notice_no_default)
        } else {
            views.identityServerSetDefaultNotice.text = getString(
                    CommonStrings.identity_server_set_default_notice,
                    state.homeServerUrl.toReducedUrl(),
                    state.defaultIdentityServerUrl.toReducedUrl()
            )
                    .toSpannable()
                    .colorizeMatchingText(
                            state.defaultIdentityServerUrl.toReducedUrl(),
                            colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_tertiary)
                    )

            views.identityServerSetDefaultNotice.isVisible = true
            views.identityServerSetDefaultSubmit.isVisible = true
            views.identityServerSetDefaultSubmit.text =
                    getString(CommonStrings.identity_server_set_default_submit, state.defaultIdentityServerUrl.toReducedUrl())
            views.identityServerSetDefaultAlternative.setText(CommonStrings.identity_server_set_alternative_notice)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(DiscoverySharedViewModel::class.java)

        views.identityServerSetDefaultAlternativeTextInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.handle(SetIdentityServerAction.UseCustomIdentityServer(views.identityServerSetDefaultAlternativeTextInput.text.toString()))
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        views.identityServerSetDefaultAlternativeTextInput
                .textChanges()
                .onEach {
                    views.identityServerSetDefaultAlternativeTil.error = null
                    views.identityServerSetDefaultAlternativeSubmit.isEnabled = it.isNotEmpty()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.identityServerSetDefaultSubmit.debouncedClicks {
            viewModel.handle(SetIdentityServerAction.UseDefaultIdentityServer)
        }

        views.identityServerSetDefaultAlternativeSubmit.debouncedClicks {
            viewModel.handle(SetIdentityServerAction.UseCustomIdentityServer(views.identityServerSetDefaultAlternativeTextInput.text.toString()))
        }

        viewModel.observeViewEvents {
            when (it) {
                is SetIdentityServerViewEvents.Loading -> showLoading(it.message)
                is SetIdentityServerViewEvents.Failure -> handleFailure(it)
                is SetIdentityServerViewEvents.OtherFailure -> showFailure(it.failure)
                is SetIdentityServerViewEvents.NoTerms -> {
                    MaterialAlertDialogBuilder(requireActivity())
                            .setTitle(CommonStrings.settings_discovery_no_terms_title)
                            .setMessage(CommonStrings.settings_discovery_no_terms)
                            .setPositiveButton(CommonStrings._continue) { _, _ ->
                                processIdentityServerChange()
                            }
                            .setNegativeButton(CommonStrings.action_cancel, null)
                            .show()
                    Unit
                }
                is SetIdentityServerViewEvents.TermsAccepted -> processIdentityServerChange()
                is SetIdentityServerViewEvents.ShowTerms -> {
                    navigator.openTerms(
                            requireContext(),
                            termsActivityResultLauncher,
                            TermsService.ServiceType.IdentityService,
                            it.identityServerUrl,
                            null
                    )
                }
            }
        }
    }

    private fun handleFailure(failure: SetIdentityServerViewEvents.Failure) {
        val message = getString(failure.errorMessageId)
        if (failure.forDefault) {
            // Display the error in a dialog
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(CommonStrings.dialog_title_error)
                    .setMessage(message)
                    .setPositiveButton(CommonStrings.ok, null)
                    .show()
        } else {
            // Display the error inlined
            views.identityServerSetDefaultAlternativeTil.error = message
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.identity_server)
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
