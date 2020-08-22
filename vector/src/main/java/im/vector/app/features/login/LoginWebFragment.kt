/*
 * Copyright 2019 New Vector Ltd
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

@file:Suppress("DEPRECATION")

package im.vector.app.features.login

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.activityViewModel
import im.vector.app.R
import im.vector.app.core.extensions.appendParamToUrl
import im.vector.app.core.utils.AssetReader
import im.vector.app.features.signout.soft.SoftLogoutAction
import im.vector.app.features.signout.soft.SoftLogoutViewModel
import org.matrix.android.sdk.api.auth.LOGIN_FALLBACK_PATH
import org.matrix.android.sdk.api.auth.REGISTER_FALLBACK_PATH
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.internal.di.MoshiProvider
import kotlinx.android.synthetic.main.fragment_login_web.*
import timber.log.Timber
import java.net.URLDecoder
import javax.inject.Inject

/**
 * This screen is displayed when the application does not support login flow or registration flow
 * of the homeserver, as a fallback to login or to create an account
 */
class LoginWebFragment @Inject constructor(
        private val assetReader: AssetReader
) : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_web

    private var isWebViewLoaded = false
    private var isForSessionRecovery = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(loginWebToolbar)
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
        loginWebToolbar.title = when (state.signMode) {
            SignMode.SignIn -> getString(R.string.login_signin)
            else            -> getString(R.string.login_signup)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(state: LoginViewState) {
        loginWebWebView.settings.javaScriptEnabled = true

        // Enable local storage to support SSO with Firefox accounts
        loginWebWebView.settings.domStorageEnabled = true
        loginWebWebView.settings.databaseEnabled = true

        // Due to https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html, we hack
        // the user agent to bypass the limitation of Google, as a quick fix (a proper solution will be to use the SSO SDK)
        loginWebWebView.settings.userAgentString = "Mozilla/5.0 Google"

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
        val url = buildString {
            append(state.homeServerUrl?.trim { it == '/' })
            if (state.signMode == SignMode.SignIn) {
                append(LOGIN_FALLBACK_PATH)
                state.deviceId?.takeIf { it.isNotBlank() }?.let {
                    // But https://github.com/matrix-org/synapse/issues/5755
                    appendParamToUrl("device_id", it)
                }
            } else {
                // MODE_REGISTER
                append(REGISTER_FALLBACK_PATH)
            }
        }

        loginWebWebView.loadUrl(url)

        loginWebWebView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler,
                                            error: SslError) {
                AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.ssl_could_not_verify)
                        .setPositiveButton(R.string.ssl_trust) { _, _ -> handler.proceed() }
                        .setNegativeButton(R.string.ssl_do_not_trust) { _, _ -> handler.cancel() }
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

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                super.onReceivedError(view, errorCode, description, failingUrl)

                loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnWebLoginError(errorCode, description, failingUrl)))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                loginWebToolbar.subtitle = url
            }

            override fun onPageFinished(view: WebView, url: String) {
                // avoid infinite onPageFinished call
                if (url.startsWith("http")) {
                    // Generic method to make a bridge between JS and the UIWebView
                    val mxcJavascriptSendObjectMessage = assetReader.readAssetFile("sendObject.js")
                    view.loadUrl(mxcJavascriptSendObjectMessage)

                    if (state.signMode == SignMode.SignIn) {
                        // The function the fallback page calls when the login is complete
                        val mxcJavascriptOnLogin = assetReader.readAssetFile("onLogin.js")
                        view.loadUrl(mxcJavascriptOnLogin)
                    } else {
                        // MODE_REGISTER
                        // The function the fallback page calls when the registration is complete
                        val mxcJavascriptOnRegistered = assetReader.readAssetFile("onRegistered.js")
                        view.loadUrl(mxcJavascriptOnRegistered)
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
             * </pre>
             * @param view
             * @param url
             * @return
             */
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                if (url == null) return super.shouldOverrideUrlLoading(view, url as String?)

                if (url.startsWith("js:")) {
                    var json = url.substring(3)
                    var javascriptResponse: JavascriptResponse? = null

                    try {
                        // URL decode
                        json = URLDecoder.decode(json, "UTF-8")
                        val adapter = MoshiProvider.providesMoshi().adapter(JavascriptResponse::class.java)
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
            val softLogoutViewModel: SoftLogoutViewModel by activityViewModel()
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
            toolbarButton               -> super.onBackPressed(toolbarButton)
            loginWebWebView.canGoBack() -> loginWebWebView.goBack().run { true }
            else                        -> super.onBackPressed(toolbarButton)
        }
    }
}
