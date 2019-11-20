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

package im.vector.riotx.features.login.terms

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.appcompat.app.AlertDialog
import butterknife.OnClick
import com.airbnb.mvrx.args
import im.vector.riotx.R
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.utils.openUrlInExternalBrowser
import im.vector.riotx.features.login.AbstractLoginFragment
import im.vector.riotx.features.login.LoginAction
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_login_terms.*
import org.matrix.androidsdk.rest.model.login.LocalizedFlowDataLoginTerms
import javax.inject.Inject

@Parcelize
data class LoginTermsFragmentArgument(
        val localizedFlowDataLoginTerms: List<LocalizedFlowDataLoginTerms>
) : Parcelable

/**
 * LoginTermsFragment displays the list of policies the user has to accept
 */
class LoginTermsFragment @Inject constructor(
        private val policyController: PolicyController,
        private val errorFormatter: ErrorFormatter
) : AbstractLoginFragment(),
        PolicyController.PolicyControllerListener {

    private val params: LoginTermsFragmentArgument by args()

    override fun getLayoutResId() = R.layout.fragment_login_terms

    private var loginTermsViewState: LoginTermsViewState = LoginTermsViewState(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginTermsPolicyList.setController(policyController)
        policyController.homeServer = loginViewModel.getHomeServerUrlSimple()
        policyController.listener = this

        val list = ArrayList<LocalizedFlowDataLoginTermsChecked>()

        params.localizedFlowDataLoginTerms
                .forEach {
                    list.add(LocalizedFlowDataLoginTermsChecked(it))
                }

        loginTermsViewState = LoginTermsViewState(list)

        renderState()
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
                    openUrlInExternalBrowser(requireContext(), it)

                    // This code crashed, because user is not authenticated yet
                    //val intent = VectorWebViewActivity.getIntent(requireContext(),
                    //        localizedFlowDataLoginTerms.localizedUrl!!,
                    //        localizedFlowDataLoginTerms.localizedName!!,
                    //        WebViewMode.DEFAULT)
                    //startActivity(intent)
                }
    }

    @OnClick(R.id.loginTermsSubmit)
    internal fun submit() {
        loginViewModel.handle(LoginAction.AcceptTerms)
    }

    override fun onRegistrationError(throwable: Throwable) {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(throwable))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }
}
