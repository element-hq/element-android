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

package im.vector.riotx.features.login

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import butterknife.OnClick
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.utils.openPlayStore
import im.vector.riotx.core.utils.toast
import javax.inject.Inject

/**
 * In this screen, the user is viewing an introduction to what he can do with this application
 */
class LoginSplashFragment @Inject constructor() : AbstractLoginFragment() {

    override fun getLayoutResId() = R.layout.fragment_login_splash

    @OnClick(R.id.loginSplashSubmit)
    fun getStarted() {
        openPlayStore(requireActivity(), "im.vector.app")
    }

    @OnClick(R.id.loginSplashUninstall)
    fun uninstall() {
        @Suppress("DEPRECATION")
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
        intent.data = Uri.parse("package:" + BuildConfig.APPLICATION_ID)
        try {
            startActivity(intent)
        } catch (anfe: ActivityNotFoundException) {
            requireActivity().toast(R.string.error_no_external_application_found)
        }
    }

    override fun resetViewModel() {
        // Nothing to do
    }
}
