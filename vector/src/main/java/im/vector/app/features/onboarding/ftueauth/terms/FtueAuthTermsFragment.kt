/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.databinding.FragmentFtueLoginTermsBinding
import im.vector.app.features.login.terms.LocalizedFlowDataLoginTermsChecked
import im.vector.app.features.login.terms.LoginTermsViewState
import im.vector.app.features.login.terms.PolicyController
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.RegisterAction
import im.vector.app.features.onboarding.ftueauth.AbstractFtueAuthFragment
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.LocalizedFlowDataLoginTerms
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * LoginTermsFragment displays the list of policies the user has to accept.
 */
@AndroidEntryPoint
class FtueAuthTermsFragment :
        AbstractFtueAuthFragment<FragmentFtueLoginTermsBinding>(),
        PolicyController.PolicyControllerListener {

    @Inject lateinit var policyController: PolicyController

    private val params: FtueAuthTermsLegacyStyleFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueLoginTermsBinding {
        return FragmentFtueLoginTermsBinding.inflate(inflater, container, false)
    }

    private var loginTermsViewState: LoginTermsViewState = LoginTermsViewState(emptyList())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        val list = ArrayList<LocalizedFlowDataLoginTermsChecked>()
        params.localizedFlowDataLoginTerms
                .forEach {
                    list.add(LocalizedFlowDataLoginTermsChecked(it))
                }
        loginTermsViewState = LoginTermsViewState(list)
    }

    private fun setupViews() {
        views.termsSubmit.setOnClickListener { submit() }
        views.loginTermsPolicyList.setHasFixedSize(false)
        views.loginTermsPolicyList.configureWith(policyController, hasFixedSize = false, dividerDrawable = R.drawable.divider_horizontal)
        views.termsGutterStart.doOnLayout {
            val gutterSize = views.contentRoot.width * (views.termsGutterStart.layoutParams as ConstraintLayout.LayoutParams).guidePercent
            policyController.horizontalPadding = gutterSize.roundToInt()
        }
        policyController.listener = this
    }

    override fun onDestroyView() {
        views.loginTermsPolicyList.cleanup()
        policyController.listener = null
        super.onDestroyView()
    }

    private fun renderState() {
        policyController.setData(loginTermsViewState.localizedFlowDataLoginTermsChecked)

        // Button is enabled only if all checkboxes are checked
        views.termsSubmit.isEnabled = loginTermsViewState.allChecked()
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
        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.AcceptTerms))
    }

    override fun updateWithState(state: OnboardingViewState) {
        val homeserverName = state.selectedHomeserver.userFacingUrl.toReducedUrl()
        views.termsHeaderSubtitle.text = getString(CommonStrings.ftue_auth_terms_subtitle, homeserverName)
        policyController.homeServer = homeserverName
        renderState()
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }
}
