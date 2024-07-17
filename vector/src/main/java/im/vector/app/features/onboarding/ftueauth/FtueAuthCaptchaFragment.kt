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

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import com.airbnb.mvrx.args
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.crawlCausesFor
import im.vector.app.databinding.FragmentFtueLoginCaptchaBinding
import im.vector.app.databinding.ViewStubWebviewBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.RegisterAction
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

@Parcelize
data class FtueAuthCaptchaFragmentArgument(
        val siteKey: String
) : Parcelable

/**
 * In this screen, the user is asked to confirm they are not a robot.
 */
@AndroidEntryPoint
class FtueAuthCaptchaFragment :
        AbstractFtueAuthFragment<FragmentFtueLoginCaptchaBinding>() {

    @Inject lateinit var captchaWebview: CaptchaWebview

    private val params: FtueAuthCaptchaFragmentArgument by args()
    private var webViewBinding: ViewStubWebviewBinding? = null
    private var isWebViewLoaded = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueLoginCaptchaBinding {
        return FragmentFtueLoginCaptchaBinding.inflate(inflater, container, false).also {
            it.loginCaptchaWebViewStub.setOnInflateListener { _, inflated ->
                webViewBinding = ViewStubWebviewBinding.bind(inflated)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inflateWebViewOrShowError()
    }

    private fun inflateWebViewOrShowError() {
        views.loginCaptchaWebViewStub.inflateWebView(onError = {
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(CommonStrings.dialog_title_error)
                    .setMessage(it.localizedMessage)
                    .setPositiveButton(CommonStrings.ok) { _, _ ->
                        requireActivity().recreate()
                    }
                    .show()
        })
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }

    override fun updateWithState(state: OnboardingViewState) {
        if (!isWebViewLoaded && webViewBinding != null) {
            captchaWebview.setupWebView(this, webViewBinding!!.root, views.loginCaptchaProgress, params.siteKey, state) {
                viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.CaptchaDone(it)))
            }
            isWebViewLoaded = true
        }
    }
}

private fun ViewStub.inflateWebView(onError: (Throwable) -> Unit) {
    try {
        inflate()
    } catch (e: Exception) {
        val isMissingWebView = e.crawlCausesFor { it.message?.contains("MissingWebViewPackageException").orFalse() }
        if (isMissingWebView) {
            onError(MissingWebViewException(e))
        } else {
            onError(e)
        }
    }
}

private class MissingWebViewException(cause: Throwable) : IllegalStateException("Failed to load WebView provider: No WebView installed", cause)
