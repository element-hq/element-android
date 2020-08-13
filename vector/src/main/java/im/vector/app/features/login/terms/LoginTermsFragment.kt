/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.login.terms

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import butterknife.OnClick
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.features.login.AbstractLoginFragment
import im.vector.app.features.login.LoginAction
import im.vector.app.features.login.LoginViewState
import org.matrix.android.sdk.internal.auth.registration.LocalizedFlowDataLoginTerms
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_terms.*
import javax.inject.Inject

@Parcelize
data class LoginTermsFragmentArgument(
        val localizedFlowDataLoginTerms: List<LocalizedFlowDataLoginTerms>
) : Parcelable

/**
 * LoginTermsFragment displays the list of policies the user has to accept
 */
class LoginTermsFragment @Inject constructor(
        private val policyController: PolicyController
) : AbstractLoginFragment(),
        PolicyController.PolicyControllerListener {

    private val params: LoginTermsFragmentArgument by args()

    override fun getLayoutResId() = R.layout.fragment_login_terms

    private var loginTermsViewState: LoginTermsViewState = LoginTermsViewState(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginTermsPolicyList.configureWith(policyController)
        policyController.listener = this

        val list = ArrayList<LocalizedFlowDataLoginTermsChecked>()

        params.localizedFlowDataLoginTerms
                .forEach {
                    list.add(LocalizedFlowDataLoginTermsChecked(it))
                }

        loginTermsViewState = LoginTermsViewState(list)
    }

    override fun onDestroyView() {
        loginTermsPolicyList.cleanup()
        policyController.listener = null
        super.onDestroyView()
    }

    private fun renderState() {
        policyController.setData(loginTermsViewState.localizedFlowDataLoginTermsChecked)

        // Button is enabled only if all checkboxes are checked
        loginTermsSubmit.isEnabled = loginTermsViewState.allChecked()
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

    @OnClick(R.id.loginTermsSubmit)
    internal fun submit() {
        loginViewModel.handle(LoginAction.AcceptTerms)
    }

    override fun updateWithState(state: LoginViewState) {
        policyController.homeServer = state.homeServerUrl.toReducedUrl()
        renderState()
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
