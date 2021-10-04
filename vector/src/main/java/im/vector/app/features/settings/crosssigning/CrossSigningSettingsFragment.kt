/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.settings.crosssigning

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.auth.ReAuthActivity
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes

import javax.inject.Inject

/**
 * This Fragment is only used when user activates developer mode from the settings
 */
class CrossSigningSettingsFragment @Inject constructor(
        private val controller: CrossSigningSettingsController,
        val viewModelFactory: CrossSigningSettingsViewModel.Factory
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        CrossSigningSettingsController.InteractionListener {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: CrossSigningSettingsViewModel by fragmentViewModel()

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO -> {
                    viewModel.handle(CrossSigningSettingsAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(CrossSigningSettingsAction.PasswordAuthDone(password))
                }
                else -> {
                    viewModel.handle(CrossSigningSettingsAction.ReAuthCancelled)
                }
            }
//            activityResult.data?.extras?.getString(ReAuthActivity.RESULT_TOKEN)?.let { token ->
//            }
        } else {
            viewModel.handle(CrossSigningSettingsAction.ReAuthCancelled)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        viewModel.observeViewEvents { event ->
            when (event) {
                is CrossSigningSettingsViewEvents.Failure -> {
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(errorFormatter.toHumanReadable(event.throwable))
                            .setPositiveButton(R.string.ok, null)
                            .show()
                    Unit
                }
                is CrossSigningSettingsViewEvents.RequestReAuth -> {
                    ReAuthActivity.newIntent(requireContext(),
                            event.registrationFlowResponse,
                            event.lastErrorCode,
                            getString(R.string.initialize_cross_signing)).let { intent ->
                        reAuthActivityResultLauncher.launch(intent)
                    }
                }
                is CrossSigningSettingsViewEvents.ShowModalWaitingView -> {
                    views.waitingView.waitingView.isVisible = true
                    views.waitingView.waitingStatusText.setTextOrHide(event.status)
                }
                CrossSigningSettingsViewEvents.HideModalWaitingView -> {
                    views.waitingView.waitingView.isVisible = false
                }
            }.exhaustive
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.encryption_information_cross_signing_state)
    }

    override fun invalidate() = withState(viewModel) { state ->
        controller.setData(state)
    }

    private fun setupRecyclerView() {
        views.genericRecyclerView.configureWith(controller, hasFixedSize = false, disableItemAnimation = true)
        controller.interactionListener = this
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        controller.interactionListener = null
        super.onDestroyView()
    }

    override fun didTapInitializeCrossSigning() {
        viewModel.handle(CrossSigningSettingsAction.InitializeCrossSigning)
    }
}
