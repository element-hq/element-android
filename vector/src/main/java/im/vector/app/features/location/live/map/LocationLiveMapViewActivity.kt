/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.location.live.map

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLocationSharingBinding
import kotlinx.parcelize.Parcelize

@Parcelize
data class LocationLiveMapViewArgs(
        val roomId: String
) : Parcelable

@AndroidEntryPoint
class LocationLiveMapViewActivity : VectorBaseActivity<ActivityLocationSharingBinding>() {

    override fun getBinding() = ActivityLocationSharingBinding.inflate(layoutInflater)

    override fun initUiAndData() {
        val mapViewArgs: LocationLiveMapViewArgs? = intent?.extras?.getParcelable(EXTRA_LOCATION_LIVE_MAP_VIEW_ARGS)
        if (mapViewArgs == null) {
            finish()
            return
        }
        setupToolbar(views.toolbar)
                .setTitle(getString(R.string.location_activity_title_preview))
                .allowBack()

        if (isFirstCreation()) {
            addFragment(
                    views.fragmentContainer,
                    LocationLiveMapViewFragment::class.java,
                    mapViewArgs
            )
        }
    }

    companion object {

        private const val EXTRA_LOCATION_LIVE_MAP_VIEW_ARGS = "EXTRA_LOCATION_LIVE_MAP_VIEW_ARGS"

        fun getIntent(context: Context, locationLiveMapViewArgs: LocationLiveMapViewArgs): Intent {
            return Intent(context, LocationLiveMapViewActivity::class.java).apply {
                putExtra(EXTRA_LOCATION_LIVE_MAP_VIEW_ARGS, locationLiveMapViewArgs)
            }
        }
    }
}
