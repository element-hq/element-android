/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
