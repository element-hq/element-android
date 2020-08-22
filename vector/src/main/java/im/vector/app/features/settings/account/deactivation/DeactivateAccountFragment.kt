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

import android.content.Context
import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.showPassword
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.settings.VectorSettingsActivity
import kotlinx.android.synthetic.main.fragment_deactivate_account.*
import javax.inject.Inject

class DeactivateAccountFragment @Inject constructor(
        val viewModelFactory: DeactivateAccountViewModel.Factory
) : VectorBaseFragment() {

    private val viewModel: DeactivateAccountViewModel by fragmentViewModel()

    override fun getLayoutResId() = R.layout.fragment_deactivate_account

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.deactivate_account_title)
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

        setupUi()
        setupViewListeners()
        observeViewEvents()
    }

    private fun setupUi() {
        deactivateAccountPassword.textChanges()
                .subscribe {
                    deactivateAccountPasswordTil.error = null
                    deactivateAccountSubmit.isEnabled = it.isNotEmpty()
                }
                .disposeOnDestroyView()
    }

    private fun setupViewListeners() {
        deactivateAccountPasswordReveal.setOnClickListener {
            viewModel.handle(DeactivateAccountAction.TogglePassword)
        }

        deactivateAccountSubmit.debouncedClicks {
            viewModel.handle(DeactivateAccountAction.DeactivateAccount(
                    deactivateAccountPassword.text.toString(),
                    deactivateAccountEraseCheckbox.isChecked))
        }
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is DeactivateAccountViewEvents.Loading      -> {
                    settingsActivity?.ignoreInvalidTokenError = true
                    showLoadingDialog(it.message)
                }
                DeactivateAccountViewEvents.EmptyPassword   -> {
                    settingsActivity?.ignoreInvalidTokenError = false
                    deactivateAccountPasswordTil.error = getString(R.string.error_empty_field_your_password)
                }
                DeactivateAccountViewEvents.InvalidPassword -> {
                    settingsActivity?.ignoreInvalidTokenError = false
                    deactivateAccountPasswordTil.error = getString(R.string.settings_fail_to_update_password_invalid_current_password)
                }
                is DeactivateAccountViewEvents.OtherFailure -> {
                    settingsActivity?.ignoreInvalidTokenError = false
                    displayErrorDialog(it.throwable)
                }
                DeactivateAccountViewEvents.Done            ->
                    MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCredentials = true, isAccountDeactivated = true))
            }.exhaustive
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        deactivateAccountPassword.showPassword(state.passwordShown)
        deactivateAccountPasswordReveal.setImageResource(if (state.passwordShown) R.drawable.ic_eye_closed else R.drawable.ic_eye)
    }
}
