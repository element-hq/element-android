/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.ui.consent

import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import javax.inject.Inject

/**
 * Simple container for AnalyticsOptInFragment.
 */
@AndroidEntryPoint
class AnalyticsOptInActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    @Inject lateinit var orientationLocker: ScreenOrientationLocker

    private val viewModel: AnalyticsConsentViewModel by viewModel()

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        orientationLocker.lockPhonesToPortrait(this)
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, AnalyticsOptInFragment::class.java)
        }

        viewModel.observeViewEvents {
            when (it) {
                AnalyticsOptInViewEvents.OnDataSaved -> finish()
            }
        }
    }
}
