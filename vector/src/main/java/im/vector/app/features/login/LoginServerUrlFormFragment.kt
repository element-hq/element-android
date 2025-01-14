/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentLoginServerUrlFormBinding
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.failure.Failure
import reactivecircus.flowbinding.android.widget.textChanges
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * In this screen, the user is prompted to enter a homeserver url.
 */
@AndroidEntryPoint
class LoginServerUrlFormFragment :
        AbstractLoginFragment<FragmentLoginServerUrlFormBinding>() {

    @Inject lateinit var buildMeta: BuildMeta

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginServerUrlFormBinding {
        return FragmentLoginServerUrlFormBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupHomeServerField()
    }

    private fun setupViews() {
        views.loginServerUrlFormLearnMore.debouncedClicks { learnMore() }
        views.loginServerUrlFormClearHistory.debouncedClicks { clearHistory() }
        views.loginServerUrlFormSubmit.debouncedClicks { submit() }
    }

    private fun setupHomeServerField() {
        views.loginServerUrlFormHomeServerUrl.textChanges()
                .onEach {
                    views.loginServerUrlFormHomeServerUrlTil.error = null
                    views.loginServerUrlFormSubmit.isEnabled = it.isNotBlank()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.loginServerUrlFormHomeServerUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                views.loginServerUrlFormHomeServerUrl.dismissDropDown()
                submit()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setupUi(state: LoginViewState) {
        when (state.serverType) {
            ServerType.EMS -> {
                views.loginServerUrlFormIcon.isVisible = true
                views.loginServerUrlFormTitle.text = getString(CommonStrings.login_connect_to_modular)
                views.loginServerUrlFormText.text = getString(CommonStrings.login_server_url_form_modular_text)
                views.loginServerUrlFormLearnMore.isVisible = true
                views.loginServerUrlFormHomeServerUrlTil.hint = getText(CommonStrings.login_server_url_form_modular_hint)
                views.loginServerUrlFormNotice.text = getString(CommonStrings.login_server_url_form_modular_notice)
            }
            else -> {
                views.loginServerUrlFormIcon.isVisible = false
                views.loginServerUrlFormTitle.text = getString(CommonStrings.login_server_other_title)
                views.loginServerUrlFormText.text = getString(CommonStrings.login_connect_to_a_custom_server)
                views.loginServerUrlFormLearnMore.isVisible = false
                views.loginServerUrlFormHomeServerUrlTil.hint = getText(CommonStrings.login_server_url_form_other_hint)
                views.loginServerUrlFormNotice.text = getString(CommonStrings.login_server_url_form_common_notice)
            }
        }
        val completions = state.knownCustomHomeServersUrls + if (buildMeta.isDebug) listOf("http://10.0.2.2:8080") else emptyList()
        views.loginServerUrlFormHomeServerUrl.setAdapter(
                ArrayAdapter(
                        requireContext(),
                        R.layout.item_completion_homeserver,
                        completions
                )
        )
        views.loginServerUrlFormHomeServerUrlTil.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                .takeIf { completions.isNotEmpty() }
                ?: TextInputLayout.END_ICON_NONE
    }

    private fun learnMore() {
        openUrlInChromeCustomTab(requireActivity(), null, EMS_LINK)
    }

    private fun clearHistory() {
        loginViewModel.handle(LoginAction.ClearHomeServerHistory)
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetHomeServerUrl)
    }

    @SuppressLint("SetTextI18n")
    private fun submit() {
        cleanupUi()

        // Static check of homeserver url, empty, malformed, etc.
        val serverUrl = views.loginServerUrlFormHomeServerUrl.text.toString().trim().ensureProtocol()

        when {
            serverUrl.isBlank() -> {
                views.loginServerUrlFormHomeServerUrlTil.error = getString(CommonStrings.login_error_invalid_home_server)
            }
            else -> {
                views.loginServerUrlFormHomeServerUrl.setText(serverUrl, false /* to avoid completion dialog flicker*/)
                loginViewModel.handle(LoginAction.UpdateHomeServer(serverUrl))
            }
        }
    }

    private fun cleanupUi() {
        views.loginServerUrlFormSubmit.hideKeyboard()
        views.loginServerUrlFormHomeServerUrlTil.error = null
    }

    override fun onError(throwable: Throwable) {
        views.loginServerUrlFormHomeServerUrlTil.error = if (throwable is Failure.NetworkConnection &&
                throwable.ioException is UnknownHostException) {
            // Invalid homeserver?
            getString(CommonStrings.login_error_homeserver_not_found)
        } else {
            errorFormatter.toHumanReadable(throwable)
        }
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)

        views.loginServerUrlFormClearHistory.isInvisible = state.knownCustomHomeServersUrls.isEmpty()
    }
}
