/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin

import android.content.Context
import android.content.Intent
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.lib.core.utils.compat.getParcelableCompat

@AndroidEntryPoint
class PinActivity : VectorBaseActivity<ActivitySimpleBinding>(), UnlockedActivity {

    companion object {
        fun newIntent(context: Context, args: PinArgs): Intent {
            return Intent(context, PinActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: PinArgs = intent?.extras?.getParcelableCompat(Mavericks.KEY_ARG) ?: return
            addFragment(views.simpleFragmentContainer, PinFragment::class.java, fragmentArgs)
        }
    }
}
