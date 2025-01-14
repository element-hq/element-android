/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login.terms

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentLoginTermsBinding
import im.vector.app.features.login.AbstractLoginFragment
import im.vector.app.features.login.LoginAction
import im.vector.app.features.login.LoginViewState
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms
import javax.inject.Inject

@Parcelize
data class LoginTermsFragmentArgument(
        val localizedFlowDataLoginTerms: List<LocalizedFlowDataLoginTerms>
) : Parcelable

/**
 * LoginTermsFragment displays the list of policies the user has to accept.
 */
class LoginTermsFragment :
        AbstractLoginFragment<FragmentLoginTermsBinding>(),
        PolicyController.PolicyControllerListener {

    @Inject lateinit var policyController: PolicyController

    private val params: LoginTermsFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginTermsBinding {
        return FragmentLoginTermsBinding.inflate(inflater, container, false)
    }

    private var loginTermsViewState: LoginTermsViewState = LoginTermsViewState(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        views.loginTermsPolicyList.configureWith(policyController)
        policyController.listener = this

        val list = ArrayList<LocalizedFlowDataLoginTermsChecked>()

        params.localizedFlowDataLoginTerms
                .forEach {
                    list.add(LocalizedFlowDataLoginTermsChecked(it))
                }

        loginTermsViewState = LoginTermsViewState(list)
    }

    private fun setupViews() {
        views.loginTermsSubmit.debouncedClicks { submit() }
    }

    override fun onDestroyView() {
        views.loginTermsPolicyList.cleanup()
        policyController.listener = null
        super.onDestroyView()
    }

    private fun renderState() {
        policyController.setData(loginTermsViewState.localizedFlowDataLoginTermsChecked)

        // Button is enabled only if all checkboxes are checked
        views.loginTermsSubmit.isEnabled = loginTermsViewState.allChecked()
    }

    override fun setChecked(localizedFlowDataLoginTerms: LocalizedFlowDataLoginTerms, isChecked: Boolean) {
        if (isChecked) {
            loginTermsViewState.check(localizedFlowDataLoginTerms)
        } else {
            loginTermsViewState.uncheck(localizedFlowDataLoginTerms)
        }

        renderState()
    }

    override fun openPolicy(localizedFlowDataLoginTerms: LocalizedFlowDataLoginTerms) {
        localizedFlowDataLoginTerms.localizedUrl
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    openUrlInChromeCustomTab(requireContext(), null, it)
                }
    }

    private fun submit() {
        loginViewModel.handle(LoginAction.AcceptTerms)
    }

    override fun updateWithState(state: LoginViewState) {
        policyController.homeServer = state.homeServerUrlFromUser.toReducedUrl()
        renderState()
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
