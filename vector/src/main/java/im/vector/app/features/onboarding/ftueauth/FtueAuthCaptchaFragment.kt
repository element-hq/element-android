/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import com.airbnb.mvrx.args
import im.vector.app.databinding.FragmentFtueLoginCaptchaBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.RegisterAction
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class FtueAuthCaptchaFragmentArgument(
        val siteKey: String
) : Parcelable

/**
 * In this screen, the user is asked to confirm they are not a robot
 */
class FtueAuthCaptchaFragment @Inject constructor(
        private val captchaWebview: CaptchaWebview
) : AbstractFtueAuthFragment<FragmentFtueLoginCaptchaBinding>() {

    private val params: FtueAuthCaptchaFragmentArgument by args()
    private var isWebViewLoaded = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueLoginCaptchaBinding {
        return FragmentFtueLoginCaptchaBinding.inflate(inflater, container, false)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun updateWithState(state: OnboardingViewState) {
        if (!isWebViewLoaded) {
            captchaWebview.setupWebView(this, views.loginCaptchaWevView, views.loginCaptchaProgress, params.siteKey, state) {
                viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.CaptchaDone(it)))
            }
            isWebViewLoaded = true
        }
    }
}
