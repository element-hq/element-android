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

import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.airbnb.mvrx.activityViewModel
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.Property
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLocationSharingBinding
import im.vector.app.features.home.AvatarRenderer
import org.billcarsonfr.jsonviewer.Utils
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class LocationSharingFragment @Inject constructor(
        private val locationTracker: LocationTracker,
        private val session: Session,
        private val avatarRenderer: AvatarRenderer
) : VectorBaseFragment<FragmentLocationSharingBinding>(), LocationTracker.Callback {

    init {
        locationTracker.callback = this
    }

    private val viewModel: LocationSharingViewModel by activityViewModel()

    private val glideRequests by lazy {
        GlideApp.with(this)
    }

    private var map: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var lastZoomValue: Double = -1.0

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationSharingBinding {
        return FragmentLocationSharingBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Mapbox before inflating mapView
        Mapbox.getInstance(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMapView(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        locationTracker.stop()
    }

    private fun initMapView(savedInstanceState: Bundle?) {
        val key = BuildConfig.mapTilerKey
        val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$key"
        views.mapView.onCreate(savedInstanceState)
        views.mapView.getMapAsync { map ->
            map.setStyle(styleUrl) { style ->
                addUserPinToMap(style)
                this.symbolManager = SymbolManager(views.mapView, map, style)
                this.map = map
                // All set, start location tracker
                locationTracker.start()
            }
        }
    }

    private fun addUserPinToMap(style: Style) {
        session.getUser(session.myUserId)?.toMatrixItem()?.let {
            val size = Utils.dpToPx(44, requireContext())
            avatarRenderer.render(glideRequests, it, object : CustomTarget<Drawable>(size, size) {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val bgUserPin = ContextCompat.getDrawable(requireActivity(), R.drawable.bg_map_user_pin)!!
                    val layerDrawable = LayerDrawable(arrayOf(bgUserPin, resource))
                    val horizontalInset = Utils.dpToPx(4, requireContext())
                    val topInset = Utils.dpToPx(4, requireContext())
                    val bottomInset = Utils.dpToPx(8, requireContext())
                    layerDrawable.setLayerInset(1, horizontalInset, topInset, horizontalInset, bottomInset)

                    style.addImage(
                            USER_PIN_NAME,
                            layerDrawable
                    )
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Is it possible? Put placeholder instead?
                }
            })
        }
    }

    override fun onLocationUpdate(latitude: Double, longitude: Double) {
        lastZoomValue = if (lastZoomValue == -1.0) INITIAL_ZOOM else map?.cameraPosition?.zoom ?: INITIAL_ZOOM

        val latLng = LatLng(latitude, longitude)

        map?.cameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(lastZoomValue)
                .build()

        symbolManager?.deleteAll()

        symbolManager?.create(
                SymbolOptions()
                        .withLatLng(latLng)
                        .withIconImage(USER_PIN_NAME)
                        .withIconAnchor(Property.ICON_ANCHOR_BOTTOM)
        )
    }

    companion object {
        const val INITIAL_ZOOM = 12.0
        const val USER_PIN_NAME = "USER_PIN_NAME"
    }
}
