/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.login

import android.content.ComponentName
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.utils.openUrlInChromeCustomTab
import kotlinx.android.synthetic.main.fragment_login_signup_signin_selection.*
import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in using SSO
 * This Fragment binds a CustomTabsServiceConnection if available, then prefetch the SSO url, as it will be likely to be opened.
 */
open class LoginSignUpSignInSsoFragment @Inject constructor() : LoginSignUpSignInSelectionFragment() {

    private var ssoUrl: String? = null
    private var customTabsServiceConnection: CustomTabsServiceConnection? = null
    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null

    override fun onStart() {
        super.onStart()

        val packageName = CustomTabsClient.getPackageName(requireContext(), null)

        // packageName can be null if there are 0 or several CustomTabs compatible browsers installed on the device
        if (packageName != null) {
            customTabsServiceConnection = object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                    customTabsClient = client
                            .also { it.warmup(0L) }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                }
            }
                    .also {
                        CustomTabsClient.bindCustomTabsService(
                                requireContext(),
                                // Despite the API, packageName cannot be null
                                packageName,
                                it
                        )
                    }
        }
    }

    private fun prefetchUrl(url: String) {
        if (ssoUrl != null) return

        ssoUrl = url
        if (customTabsSession == null) {
            customTabsSession = customTabsClient?.newSession(null)
        }

        customTabsSession?.mayLaunchUrl(Uri.parse(url), null, null)
    }

    override fun onStop() {
        super.onStop()
        customTabsServiceConnection?.let { requireContext().unbindService(it) }
        customTabsServiceConnection = null
    }

    private fun setupButtons() {
        loginSignupSigninSubmit.text = getString(R.string.login_signin_sso)
        loginSignupSigninSignIn.isVisible = false
    }

    override fun submit() {
        ssoUrl?.let { openUrlInChromeCustomTab(requireContext(), customTabsSession, it) }
    }

    override fun updateWithState(state: LoginViewState) {
        setupUi(state)
        setupButtons()
        prefetchUrl(state.getSsoUrl())
    }
}
