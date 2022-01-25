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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.openLocation
import im.vector.app.databinding.FragmentLocationPreviewBinding
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import javax.inject.Inject

class LocationPreviewFragment @Inject constructor(
        private val locationPinProvider: LocationPinProvider
) : VectorBaseFragment<FragmentLocationPreviewBinding>() {

    private val args: LocationSharingArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationPreviewBinding {
        return FragmentLocationPreviewBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.mapView.initialize {
            if (isAdded) {
                onMapReady()
            }
        }
    }

    override fun onPause() {
        views.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        views.mapView.onStop()
        super.onStop()
    }

    override fun getMenuRes() = R.menu.menu_location_preview

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share_external -> {
                onShareLocationExternal()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onShareLocationExternal() {
        val location = args.initialLocationData ?: return
        openLocation(requireActivity(), location.latitude, location.longitude)
    }

    private fun onMapReady() {
        if (!isAdded) return

        val location = args.initialLocationData ?: return
        val userId = args.locationOwnerId

        locationPinProvider.create(userId) { pinDrawable ->
            views.mapView.apply {
                zoomToLocation(location.latitude, location.longitude, INITIAL_MAP_ZOOM)
                deleteAllPins()
                addPinToMap(userId, pinDrawable)
                updatePinLocation(userId, location.latitude, location.longitude)
            }
        }
    }
}
