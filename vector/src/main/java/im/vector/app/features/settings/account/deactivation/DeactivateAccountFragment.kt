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

package im.vector.app.features.settings.account.deactivation

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.fragmentViewModel
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentDeactivateAccountBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.auth.ReAuthActivity
import im.vector.app.features.settings.VectorSettingsActivity
import org.matrix.android.sdk.api.auth.data.LoginFlowTypes
import javax.inject.Inject

class DeactivateAccountFragment @Inject constructor() : VectorBaseFragment<FragmentDeactivateAccountBinding>() {

    private val viewModel: DeactivateAccountViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentDeactivateAccountBinding {
        return FragmentDeactivateAccountBinding.inflate(inflater, container, false)
    }

    private val reAuthActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            when (activityResult.data?.extras?.getString(ReAuthActivity.RESULT_FLOW_TYPE)) {
                LoginFlowTypes.SSO      -> {
                    viewModel.handle(DeactivateAccountAction.SsoAuthDone)
                }
                LoginFlowTypes.PASSWORD -> {
                    val password = activityResult.data?.extras?.getString(ReAuthActivity.RESULT_VALUE) ?: ""
                    viewModel.handle(DeactivateAccountAction.PasswordAuthDone(password))
                }
                else                    -> {
                    viewModel.handle(DeactivateAccountAction.ReAuthCancelled)
                }
            }
        } else {
            viewModel.handle(DeactivateAccountAction.ReAuthCancelled)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.DeactivateAccount
    }

    override fun onResume() {
        super.onResume()
        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.deactivate_account_title)
    }

    private var settingsActivity: VectorSettingsActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        settingsActivity = context as? VectorSettingsActivity
    }

    override fun onDetach() {
        super.onDetach()
        settingsActivity = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewListeners()
        observeViewEvents()
    }

    private fun setupViewListeners() {
        views.deactivateAccountSubmit.debouncedClicks {
            viewModel.handle(DeactivateAccountAction.DeactivateAccount(
                    views.deactivateAccountEraseCheckbox.isChecked)
            )
        }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is DeactivateAccountViewEvents.Loading       -> {
                    settingsActivity?.ignoreInvalidTokenError = true
                    showLoadingDialog(it.message)
                }
                DeactivateAccountViewEvents.InvalidAuth      -> {
                    dismissLoadingDialog()
                    settingsActivity?.ignoreInvalidTokenError = false
                }
                is DeactivateAccountViewEvents.OtherFailure  -> {
                    settingsActivity?.ignoreInvalidTokenError = false
                    dismissLoadingDialog()
                    displayErrorDialog(it.throwable)
                }
                DeactivateAccountViewEvents.Done             -> {
                    MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCredentials = true, isAccountDeactivated = true))
                }
                is DeactivateAccountViewEvents.RequestReAuth -> {
                    ReAuthActivity.newIntent(requireContext(),
                            it.registrationFlowResponse,
                            it.lastErrorCode,
                            getString(R.string.deactivate_account_title)).let { intent ->
                        reAuthActivityResultLauncher.launch(intent)
                    }
                }
            }
        }
    }
}
