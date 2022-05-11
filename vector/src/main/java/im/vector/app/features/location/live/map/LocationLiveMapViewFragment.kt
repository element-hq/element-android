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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.mapboxsdk.maps.SupportMapFragment
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLiveLocationMapBinding
import im.vector.app.databinding.FragmentLocationPreviewBinding
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.UrlMapProvider
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Screen showing a map with all the current users sharing their live location in room.
 */
class LocationLiveMapViewFragment @Inject constructor(
        private val urlMapProvider: UrlMapProvider,
        private val locationPinProvider: LocationPinProvider
) : VectorBaseFragment<FragmentLiveLocationMapBinding>() {

    private val args: LocationLiveMapViewArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLiveLocationMapBinding {
        return FragmentLiveLocationMapBinding.inflate(layoutInflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
    }

    private fun setupMap() {
        val mapFragment: SupportMapFragment =
                parentFragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG) as? SupportMapFragment
                        ?: run {
                            val options = MapboxMapOptions.createFromAttributes(requireContext(), null)
                            val fragment = SupportMapFragment.newInstance(options)
                            addFragment(R.id.liveLocationMapContainer, fragment, tag = MAP_FRAGMENT_TAG)
                            fragment
                        }

        mapFragment.getMapAsync { mapBoxMap ->
            lifecycleScope.launchWhenCreated {
                mapBoxMap.setStyle(urlMapProvider.getMapUrl())
            }
        }
    }

    companion object {
        private const val MAP_FRAGMENT_TAG = "im.vector.app.features.location.live.map"
    }
}
