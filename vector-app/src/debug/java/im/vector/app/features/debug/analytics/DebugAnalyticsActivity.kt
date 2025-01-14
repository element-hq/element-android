/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.analytics

import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding

@AndroidEntryPoint
class DebugAnalyticsActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(
                    views.simpleFragmentContainer,
                    DebugAnalyticsFragment::class.java
            )
        }
    }
}
