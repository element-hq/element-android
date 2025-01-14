/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.rename

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.lib.core.utils.compat.getParcelableExtraCompat

/**
 * Display the screen to rename a Session.
 */
@AndroidEntryPoint
class RenameSessionActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            addFragment(
                    container = views.simpleFragmentContainer,
                    fragmentClass = RenameSessionFragment::class.java,
                    params = intent.getParcelableExtraCompat(Mavericks.KEY_ARG)
            )
        }
    }

    companion object {
        fun newIntent(context: Context, deviceId: String): Intent {
            return Intent(context, RenameSessionActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, RenameSessionArgs(deviceId))
            }
        }
    }
}
