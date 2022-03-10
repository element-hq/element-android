/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.location

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLocationSharingBinding
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
        val locationSharingArgs: LocationSharingArgs? = intent?.extras?.getParcelable(EXTRA_LOCATION_SHARING_ARGS)
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
                LocationSharingMode.PREVIEW        -> {
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
