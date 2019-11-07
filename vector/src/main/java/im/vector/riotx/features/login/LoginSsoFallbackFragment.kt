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
import com.airbnb.mvrx.activityViewModel
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.util.JsonDict
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.riotx.R
import im.vector.riotx.core.platform.OnBackPressed
import im.vector.riotx.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_login_sso_fallback.*
import timber.log.Timber
import java.net.URLDecoder
import javax.inject.Inject

/**
 * Only login is supported for the moment
 */
class LoginSsoFallbackFragment @Inject constructor() : VectorBaseFragment(), OnBackPressed {

    private val viewModel: LoginViewModel by activityViewModel()

    var homeServerUrl: String = ""

    enum class Mode {
        MODE_LOGIN,
        // Not supported in RiotX for the moment
        MODE_REGISTER
    }

    // Mode (MODE_LOGIN or MODE_REGISTER)
    private var mMode = Mode.MODE_LOGIN

    override fun getLayoutResId() = R.layout.fragment_login_sso_fallback

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar(login_sso_fallback_toolbar)
        login_sso_fallback_toolbar.title = getString(R.string.login)

        setupWebview()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebview() {
        login_sso_fallback_webview.settings.javaScriptEnabled = true

        // Due to https://developers.googleblog.com/2016/08/modernizing-oauth-interactions-in-native-apps.html, we hack
        // the user agent to bypass the limitation of Google, as a quick fix (a proper solution will be to use the SSO SDK)
        login_sso_fallback_webview.settings.userAgentString = "Mozilla/5.0 Google"

        homeServerUrl = viewModel.getHomeServerUrl()

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
        if (mMode == Mode.MODE_LOGIN) {
            login_sso_fallback_webview.loadUrl(homeServerUrl + "_matrix/static/client/login/")
        } else {
            // MODE_REGISTER
            login_sso_fallback_webview.loadUrl(homeServerUrl + "_matrix/static/client/register/")
        }

        login_sso_fallback_webview.webViewClient = object : WebViewClient() {
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
                        .show()
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                super.onReceivedError(view, errorCode, description, failingUrl)

                // on error case, close this fragment
                viewModel.handle(LoginActions.NavigateTo(LoginActivity.Navigation.GoBack))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                login_sso_fallback_toolbar.subtitle = url
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

                    if (mMode == Mode.MODE_LOGIN) {
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

                        if (mMode == Mode.MODE_LOGIN) {
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

                                        viewModel.handle(LoginActions.SsoLoginSuccess(safeCredentials))
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

                                    viewModel.handle(LoginActions.SsoLoginSuccess(credentials))
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

    override fun onBackPressed(): Boolean {
        return if (login_sso_fallback_webview.canGoBack()) {
            login_sso_fallback_webview.goBack()
            true
        } else {
            false
        }
    }
}
