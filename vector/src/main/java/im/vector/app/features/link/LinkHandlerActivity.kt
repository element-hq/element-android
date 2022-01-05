/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.link

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.databinding.ActivityProgressBinding
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.permalink.PermalinkHandler
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.permalinks.PermalinkService
import timber.log.Timber
import javax.inject.Inject

/**
 * Dummy activity used to dispatch the vector URL links.
 */
@AndroidEntryPoint
class LinkHandlerActivity : VectorBaseActivity<ActivityProgressBinding>() {

    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var errorFormatter: ErrorFormatter
    @Inject lateinit var permalinkHandler: PermalinkHandler

    override fun getBinding() = ActivityProgressBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        handleIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent()
    }

    private fun handleIntent() {
        val uri = intent.data
        when {
            uri == null                                                                    -> {
                // Should not happen
                Timber.w("Uri is null")
                finish()
            }
            uri.getQueryParameter(LoginConfig.CONFIG_HS_PARAMETER) != null                 -> handleConfigUrl(uri)
            uri.toString().startsWith(PermalinkService.MATRIX_TO_URL_BASE)                 -> handleSupportedHostUrl()
            uri.toString().startsWith(PermalinkHandler.MATRIX_TO_CUSTOM_SCHEME_URL_BASE)   -> handleSupportedHostUrl()
            resources.getStringArray(R.array.permalink_supported_hosts).contains(uri.host) -> handleSupportedHostUrl()
            else                                                                           -> {
                // Other links are not yet handled, but should not come here (manifest configuration error?)
                toast(R.string.universal_link_malformed)
                finish()
            }
        }
    }

    private fun handleConfigUrl(uri: Uri) {
        if (sessionHolder.hasActiveSession()) {
            displayAlreadyLoginPopup(uri)
        } else {
            // user is not yet logged in, this is the nominal case
            startLoginActivity(uri)
        }
    }

    private fun handleSupportedHostUrl() {
        // If we are not logged in, open login screen.
        // In the future, we might want to relaunch the process after login.
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity()
            return
        }

        // We forward intent to HomeActivity (singleTask) to avoid the dueling app problem
        // https://stackoverflow.com/questions/25884954/deep-linking-and-multiple-app-instances
        intent.setClass(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    /**
     * Start the login screen with identity server and homeserver pre-filled, if any
     */
    private fun startLoginActivity(uri: Uri? = null) {
        navigator.openLogin(
                context = this,
                loginConfig = uri?.let { LoginConfig.parse(uri) },
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        finish()
    }

    /**
     * Propose to disconnect from a previous HS, when clicking on an auto config link
     */
    private fun displayAlreadyLoginPopup(uri: Uri) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_warning)
                .setMessage(R.string.error_user_already_logged_in)
                .setCancelable(false)
                .setPositiveButton(R.string.logout) { _, _ -> safeSignout(uri) }
                .setNegativeButton(R.string.action_cancel) { _, _ -> finish() }
                .show()
    }

    private fun safeSignout(uri: Uri) {
        val session = sessionHolder.getSafeActiveSession()
        if (session == null) {
            // Should not happen
            startLoginActivity(uri)
        } else {
            lifecycleScope.launch {
                try {
                    session.signOut(true)
                    Timber.d("## displayAlreadyLoginPopup(): logout succeeded")
                    sessionHolder.clearActiveSession()
                    startLoginActivity(uri)
                } catch (failure: Throwable) {
                    displayError(failure)
                }
            }
        }
    }

    private fun displayError(failure: Throwable) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(failure))
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }
                .show()
    }
}
