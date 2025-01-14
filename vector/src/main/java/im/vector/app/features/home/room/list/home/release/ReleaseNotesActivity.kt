/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReleaseNotesActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    @Inject lateinit var orientationLocker: ScreenOrientationLocker
    @Inject lateinit var releaseNotesPreferencesStore: ReleaseNotesPreferencesStore

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        orientationLocker.lockPhonesToPortrait(this)
        if (isFirstCreation()) {
            addFragment(views.simpleFragmentContainer, ReleaseNotesFragment::class.java)
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            releaseNotesPreferencesStore.setAppLayoutOnboardingShown(true)
        }
    }
}
