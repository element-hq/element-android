/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.features.navigation.Navigator
import javax.inject.Inject

@AndroidEntryPoint
class SSORedirectRouterActivity : AppCompatActivity() {

    @Inject lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigator.loginSSORedirect(this, intent.data)
        finish()
    }

    companion object {
        // Note that the domain can be displayed to the user for confirmation that he trusts it. So use a human readable string
        const val VECTOR_REDIRECT_URL = "element://connect"
    }
}
