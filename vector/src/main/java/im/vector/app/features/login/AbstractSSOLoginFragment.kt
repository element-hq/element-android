/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.content.ComponentName
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.viewbinding.ViewBinding
import com.airbnb.mvrx.withState
import im.vector.app.core.utils.openUrlInChromeCustomTab
import org.matrix.android.sdk.api.auth.SSOAction

abstract class AbstractSSOLoginFragment<VB : ViewBinding> : AbstractLoginFragment<VB>() {

    // For sso
    private var customTabsServiceConnection: CustomTabsServiceConnection? = null
    private var customTabsClient: CustomTabsClient? = null
    private var customTabsSession: CustomTabsSession? = null

    override fun onStart() {
        super.onStart()
        val hasSSO = withState(loginViewModel) { it.loginMode.hasSso() }
        if (hasSSO) {
            val packageName = CustomTabsClient.getPackageName(requireContext(), null)

            // packageName can be null if there are 0 or several CustomTabs compatible browsers installed on the device
            if (packageName != null) {
                customTabsServiceConnection = object : CustomTabsServiceConnection() {
                    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                        customTabsClient = client
                                .also { it.warmup(0L) }
                        prefetchIfNeeded()
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
    }

    override fun onStop() {
        super.onStop()
        val hasSSO = withState(loginViewModel) { it.loginMode.hasSso() }
        if (hasSSO) {
            customTabsServiceConnection?.let { requireContext().unbindService(it) }
            customTabsServiceConnection = null
        }
    }

    private fun prefetchUrl(url: String) {
        if (customTabsSession == null) {
            customTabsSession = customTabsClient?.newSession(null)
        }

        customTabsSession?.mayLaunchUrl(Uri.parse(url), null, null)
    }

    fun openInCustomTab(ssoUrl: String) {
        openUrlInChromeCustomTab(requireContext(), customTabsSession, ssoUrl)
    }

    private fun prefetchIfNeeded() {
        withState(loginViewModel) { state ->
            if (state.loginMode.hasSso() && state.loginMode.ssoState().isFallback()) {
                // in this case we can prefetch (not other cases for privacy concerns)
                loginViewModel.getSsoUrl(
                        redirectUrl = SSORedirectRouterActivity.VECTOR_REDIRECT_URL,
                        deviceId = state.deviceId,
                        providerId = null,
                        action = if (state.signMode == SignMode.SignUp) SSOAction.REGISTER else SSOAction.LOGIN
                )
                        ?.let { prefetchUrl(it) }
            }
        }
    }
}
