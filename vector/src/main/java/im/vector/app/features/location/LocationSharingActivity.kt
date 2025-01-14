/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLocationSharingBinding
import im.vector.app.features.location.preview.LocationPreviewFragment
import im.vector.lib.core.utils.compat.getParcelableCompat
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationSharingArgs(
        val roomId: String,
        val mode: LocationSharingMode,
        val initialLocationData: LocationData?,
        val locationOwnerId: String?
) : Parcelable

@AndroidEntryPoint
class LocationSharingActivity : VectorBaseActivity<ActivityLocationSharingBinding>() {

    override fun getBinding() = ActivityLocationSharingBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        val locationSharingArgs: LocationSharingArgs? = intent?.extras?.getParcelableCompat(EXTRA_LOCATION_SHARING_ARGS)
        if (locationSharingArgs == null) {
            finish()
            return
        }
        setupToolbar(views.toolbar)
                .setTitle(locationSharingArgs.mode.titleRes)
                .allowBack()

        if (isFirstCreation()) {
            when (locationSharingArgs.mode) {
                LocationSharingMode.STATIC_SHARING -> {
                    addFragment(
                            views.fragmentContainer,
                            LocationSharingFragment::class.java,
                            locationSharingArgs
                    )
                }
                LocationSharingMode.PREVIEW -> {
                    addFragment(
                            views.fragmentContainer,
                            LocationPreviewFragment::class.java,
                            locationSharingArgs
                    )
                }
            }
        }
    }

    companion object {

        private const val EXTRA_LOCATION_SHARING_ARGS = "EXTRA_LOCATION_SHARING_ARGS"

        fun getIntent(context: Context, locationSharingArgs: LocationSharingArgs): Intent {
            return Intent(context, LocationSharingActivity::class.java).apply {
                putExtra(EXTRA_LOCATION_SHARING_ARGS, locationSharingArgs)
            }
        }
    }
}
