/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

@file:Suppress("DEPRECATION")

package im.vector.app.features.login

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import com.airbnb.mvrx.activityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.utils.AssetReader
import im.vector.app.databinding.FragmentLoginWebBinding
import im.vector.app.features.signout.soft.SoftLogoutAction
import im.vector.app.features.signout.soft.SoftLogoutViewModel
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.util.MatrixJsonParser
import timber.log.Timber
import java.net.URLDecoder
import javax.inject.Inject

/**
 * This screen is displayed when the application does not support login flow or registration flow
 * of the homeserver, as a fallback to login or to create an account.
 */
@AndroidEntryPoint
class LoginWebFragment :
        AbstractLoginFragment<FragmentLoginWebBinding>() {

    @Inject lateinit var assetReader: AssetReader

    private val softLogoutViewModel: SoftLogoutViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginWebBinding {
        return FragmentLoginWebBinding.inflate(inflater, container, false)
    }

    private var isWebViewLoaded = false
    private var isForSessionRecovery = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(views.loginWebToolbar)
                .allowBack()
    }

    override fun updateWithState(state: LoginViewState) {
        setupTitle(state)

        isForSessionRecovery = state.deviceId?.isNotBlank() == true

        if (!isWebViewLoaded) {
            setupWebView(state)
            isWebViewLoaded = true
        }
    }

    private fun setupTitle(state: LoginViewState) {
        toolbar?.title = when (state.signMode) {
            SignMode.SignIn -> getString(CommonStrings.login_signin)
            else -> getString(CommonStrings.login_signup)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(state: LoginViewState) {
        views.loginWebWebView.settings.javaScriptEnabled = true

        // Enable local storage to support SSO with Firefox accounts
        views.loginWebWebView.settings.domStorageEnabled = true
        views.loginWebWebView.settings.databaseEnabled = true

        // Due to https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html, we hack
        // the user agent to bypass the limitation of Google, as a quick fix (a proper solution will be to use the SSO SDK)
        views.loginWebWebView.settings.userAgentString = "Mozilla/5.0 Google"

        // AppRTC requires third party cookies to work
        val cookieManager = android.webkit.CookieManager.getInstance()

        // clear the cookies
        if (cookieManager == null) {
            launchWebView(state)
        } else {
            if (!cookieManager.hasCookies()) {
                launchWebView(state)
            } else {
                try {
                    cookieManager.removeAllCookies { launchWebView(state) }
                } catch (e: Exception) {
                    Timber.e(e, " cookieManager.removeAllCookie() fails")
                    launchWebView(state)
                }
            }
        }
    }

    private fun launchWebView(state: LoginViewState) {
        val url = loginViewModel.getFallbackUrl(state.signMode == SignMode.SignIn, state.deviceId) ?: return

        views.loginWebWebView.loadUrl(url)

        views.loginWebWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                    view: WebView, handler: SslErrorHandler,
                    error: SslError
            ) {
                MaterialAlertDialogBuilder(requireActivity())
                        .setMessage(CommonStrings.ssl_could_not_verify)
                        .setPositiveButton(CommonStrings.ssl_trust) { _, _ -> handler.proceed() }
                        .setNegativeButton(CommonStrings.ssl_do_not_trust) { _, _ -> handler.cancel() }
                        .setOnKeyListener(DialogInterface.OnKeyListener { dialog, keyCode, event ->
                            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                                handler.cancel()
                                dialog.dismiss()
                                return@OnKeyListener true
                            }
                            false
                        })
                        .setCancelable(false)
                        .show()
            }

            @Deprecated("Deprecated in Java")
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                super.onReceivedError(view, errorCode, description, failingUrl)

                loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnWebLoginError(errorCode, description, failingUrl)))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                toolbar?.subtitle = url
            }

            override fun onPageFinished(view: WebView, url: String) {
                // avoid infinite onPageFinished call
                if (url.startsWith("http")) {
                    // Generic method to make a bridge between JS and the UIWebView
                    assetReader.readAssetFile("sendObject.js")?.let { view.loadUrl(it) }

                    if (state.signMode == SignMode.SignIn) {
                        // The function the fallback page calls when the login is complete
                        assetReader.readAssetFile("onLogin.js")?.let { view.loadUrl(it) }
                    } else {
                        // MODE_REGISTER
                        // The function the fallback page calls when the registration is complete
                        assetReader.readAssetFile("onRegistered.js")?.let { view.loadUrl(it) }
                    }
                }
            }

            /**
             * Example of (formatted) url for MODE_LOGIN:
             *
             * <pre>
             * js:{
             *     "action":"onLogin",
             *     "credentials":{
             *         "user_id":"@user:matrix.org",
             *         "access_token":"[ACCESS_TOKEN]",
             *         "home_server":"matrix.org",
             *         "device_id":"[DEVICE_ID]",
             *         "well_known":{
             *             "m.homeserver":{
             *                 "base_url":"https://matrix.org/"
             *                 }
             *             }
             *         }
             *    }
             * </pre>.
             * @param view
             * @param url
             * @return
             */
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                if (url == null) return super.shouldOverrideUrlLoading(view, url as String?)

                if (url.startsWith("js:")) {
                    var json = url.substring(3)
                    var javascriptResponse: JavascriptResponse? = null

                    try {
                        // URL decode
                        json = URLDecoder.decode(json, "UTF-8")
                        val adapter = MatrixJsonParser.getMoshi().adapter(JavascriptResponse::class.java)
                        javascriptResponse = adapter.fromJson(json)
                    } catch (e: Exception) {
                        Timber.e(e, "## shouldOverrideUrlLoading() : fromJson failed")
                    }

                    // succeeds to parse parameters
                    if (javascriptResponse != null) {
                        val action = javascriptResponse.action

                        if (state.signMode == SignMode.SignIn) {
                            if (action == "onLogin") {
                                javascriptResponse.credentials?.let { notifyViewModel(it) }
                            }
                        } else {
                            // MODE_REGISTER
                            // check the required parameters
                            if (action == "onRegistered") {
                                javascriptResponse.credentials?.let { notifyViewModel(it) }
                            }
                        }
                    }
                    return true
                }

                return super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    private fun notifyViewModel(credentials: Credentials) {
        if (isForSessionRecovery) {
            softLogoutViewModel.handle(SoftLogoutAction.WebLoginSuccess(credentials))
        } else {
            loginViewModel.handle(LoginAction.WebLoginSuccess(credentials))
        }
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetLogin)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return when {
            toolbarButton -> super.onBackPressed(toolbarButton)
            views.loginWebWebView.canGoBack() -> views.loginWebWebView.goBack().run { true }
            else -> super.onBackPressed(toolbarButton)
        }
    }
}
