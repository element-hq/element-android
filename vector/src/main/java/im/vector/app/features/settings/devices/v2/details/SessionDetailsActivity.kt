/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.lib.core.utils.compat.getParcelableExtraCompat

/**
 * Display the details info about a Session.
 */
@AndroidEntryPoint
class SessionDetailsActivity : SimpleFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            addFragment(
                    container = views.container,
                    fragmentClass = SessionDetailsFragment::class.java,
                    params = intent.getParcelableExtraCompat(Mavericks.KEY_ARG)
            )
        }
    }

    companion object {
        fun newIntent(context: Context, deviceId: String): Intent {
            return Intent(context, SessionDetailsActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, SessionDetailsArgs(deviceId))
            }
        }
    }
}
