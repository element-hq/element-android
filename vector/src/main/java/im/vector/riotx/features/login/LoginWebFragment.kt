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

package im.vector.riotx.features.login

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.riotx.R
import kotlinx.android.synthetic.main.fragment_login_web.*
import timber.log.Timber
import java.net.URLDecoder
import javax.inject.Inject

/**
 * This screen is displayed for SSO login and also when the application does not support login flow or registration flow
 * of the homeserfver, as a fallback to login or to create an account
 */
class LoginWebFragment @Inject constructor() : AbstractLoginFragment() {

    private lateinit var homeServerUrl: String
    private lateinit var signMode: SignMode

    override fun getLayoutResId() = R.layout.fragment_login_web

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        homeServerUrl = loginViewModel.getHomeServerUrl()
        signMode = loginViewModel.signMode.takeIf { it != SignMode.Unknown } ?: error("Developer error: Invalid sign mode")

        setupToolbar(loginWebToolbar)
        setupTitle()
        setupWebView()
    }

    private fun setupTitle() {
        loginWebToolbar.title = when (signMode) {
            SignMode.SignIn -> getString(R.string.login_signin)
            else            -> getString(R.string.login_signup)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        loginWebWebView.settings.javaScriptEnabled = true

        // Due to https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html, we hack
        // the user agent to bypass the limitation of Google, as a quick fix (a proper solution will be to use the SSO SDK)
        loginWebWebView.settings.userAgentString = "Mozilla/5.0 Google"

        if (!homeServerUrl.endsWith("/")) {
            homeServerUrl += "/"
        }

        // AppRTC requires third party cookies to work
        val cookieManager = android.webkit.CookieManager.getInstance()

        // clear the cookies must be cleared
        if (cookieManager == null) {
            launchWebView()
        } else {
            if (!cookieManager.hasCookies()) {
                launchWebView()
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                try {
                    cookieManager.removeAllCookie()
                } catch (e: Exception) {
                    Timber.e(e, " cookieManager.removeAllCookie() fails")
                }

                launchWebView()
            } else {
                try {
                    cookieManager.removeAllCookies { launchWebView() }
                } catch (e: Exception) {
                    Timber.e(e, " cookieManager.removeAllCookie() fails")
                    launchWebView()
                }
            }
        }
    }

    private fun launchWebView() {
        if (signMode == SignMode.SignIn) {
            loginWebWebView.loadUrl(homeServerUrl + "_matrix/static/client/login/")
        } else {
            // MODE_REGISTER
            loginWebWebView.loadUrl(homeServerUrl + "_matrix/static/client/register/")
        }

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

                loginSharedActionViewModel.post(LoginNavigation.OnWebLoginError(errorCode, description, failingUrl))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                loginWebToolbar.subtitle = url
            }

            override fun onPageFinished(view: WebView, url: String) {
                // avoid infinite onPageFinished call
                if (url.startsWith("http")) {
                    // Generic method to make a bridge between JS and the UIWebView
                    val mxcJavascriptSendObjectMessage = "javascript:window.sendObjectMessage = function(parameters) {" +
                            " var iframe = document.createElement('iframe');" +
                            " iframe.setAttribute('src', 'js:' + JSON.stringify(parameters));" +
                            " document.documentElement.appendChild(iframe);" +
                            " iframe.parentNode.removeChild(iframe); iframe = null;" +
                            " };"

                    view.loadUrl(mxcJavascriptSendObjectMessage)

                    if (signMode == SignMode.SignIn) {
                        // The function the fallback page calls when the login is complete
                        val mxcJavascriptOnRegistered = "javascript:window.matrixLogin.onLogin = function(response) {" +
                                " sendObjectMessage({ 'action': 'onLogin', 'credentials': response });" +
                                " };"

                        view.loadUrl(mxcJavascriptOnRegistered)
                    } else {
                        // MODE_REGISTER
                        // The function the fallback page calls when the registration is complete
                        val mxcJavascriptOnRegistered = "javascript:window.matrixRegistration.onRegistered" +
                                " = function(homeserverUrl, userId, accessToken) {" +
                                " sendObjectMessage({ 'action': 'onRegistered'," +
                                " 'homeServer': homeserverUrl," +
                                " 'userId': userId," +
                                " 'accessToken': accessToken });" +
                                " };"

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
                if (null != url && url.startsWith("js:")) {
                    var json = url.substring(3)
                    var parameters: Map<String, Any>? = null

                    try {
                        // URL decode
                        json = URLDecoder.decode(json, "UTF-8")

                        val adapter = MoshiProvider.providesMoshi().adapter(Map::class.java)

                        @Suppress("UNCHECKED_CAST")
                        parameters = adapter.fromJson(json) as JsonDict?
                    } catch (e: Exception) {
                        Timber.e(e, "## shouldOverrideUrlLoading() : fromJson failed")
                    }

                    // succeeds to parse parameters
                    if (parameters != null) {
                        val action = parameters["action"] as String

                        if (signMode == SignMode.SignIn) {
                            try {
                                if (action == "onLogin") {
                                    @Suppress("UNCHECKED_CAST")
                                    val credentials = parameters["credentials"] as Map<String, String>

                                    val userId = credentials["user_id"]
                                    val accessToken = credentials["access_token"]
                                    val homeServer = credentials["home_server"]
                                    val deviceId = credentials["device_id"]

                                    // check if the parameters are defined
                                    if (null != homeServer && null != userId && null != accessToken) {
                                        val safeCredentials = Credentials(
                                                userId = userId,
                                                accessToken = accessToken,
                                                homeServer = homeServer,
                                                deviceId = deviceId,
                                                refreshToken = null
                                        )

                                        loginViewModel.handle(LoginAction.WebLoginSuccess(safeCredentials))
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "## shouldOverrideUrlLoading() : failed")
                            }
                        } else {
                            // MODE_REGISTER
                            // check the required parameters
                            if (action == "onRegistered") {
                                // TODO The keys are very strange, this code comes from Riot-Android...
                                if (parameters.containsKey("homeServer")
                                        && parameters.containsKey("userId")
                                        && parameters.containsKey("accessToken")) {
                                    // We cannot parse Credentials here because of https://github.com/matrix-org/synapse/issues/4756
                                    // Build on object manually
                                    val credentials = Credentials(
                                            userId = parameters["userId"] as String,
                                            accessToken = parameters["accessToken"] as String,
                                            homeServer = parameters["homeServer"] as String,
                                            // TODO We need deviceId on RiotX...
                                            deviceId = "TODO",
                                            refreshToken = null
                                    )

                                    loginViewModel.handle(LoginAction.WebLoginSuccess(credentials))
                                }
                            }
                        }
                    }
                    return true
                }

                return super.shouldOverrideUrlLoading(view, url)
            }
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onBackPressed(): Boolean {
        return if (loginWebWebView.canGoBack()) {
            loginWebWebView.goBack()
            true
        } else {
            super.onBackPressed()
        }
    }
}
