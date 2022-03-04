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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mapbox.mapboxsdk.maps.MapView
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLocationSharingBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.location.option.LocationSharingOption
import org.matrix.android.sdk.api.util.MatrixItem
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * We should consider using SupportMapFragment for a out of the box lifecycle handling
 */
class LocationSharingFragment @Inject constructor(
        private val urlMapProvider: UrlMapProvider,
        private val avatarRenderer: AvatarRenderer,
        private val matrixItemColorProvider: MatrixItemColorProvider
) : VectorBaseFragment<FragmentLocationSharingBinding>() {

    private val viewModel: LocationSharingViewModel by fragmentViewModel()

    // Keep a ref to handle properly the onDestroy callback
    private var mapView: WeakReference<MapView>? = null

    private var hasRenderedUserAvatar = false
    private var hasUpdatedPin = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLocationSharingBinding {
        return FragmentLocationSharingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = WeakReference(views.mapView)
        views.mapView.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            views.mapView.initialize(urlMapProvider.getMapUrl())
        }

        initOptionsPicker()

        viewModel.observeViewEvents {
            when (it) {
                LocationSharingViewEvents.LocationNotAvailableError -> handleLocationNotAvailableError()
                LocationSharingViewEvents.Close                     -> activity?.finish()
            }.exhaustive
        }
    }

    override fun onResume() {
        super.onResume()
        views.mapView.onResume()
    }

    override fun onPause() {
        views.mapView.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        views.mapView.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        views.mapView.onStart()
    }

    override fun onStop() {
        views.mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        views.mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView?.get()?.onDestroy()
        mapView?.clear()
        super.onDestroy()
    }

    override fun invalidate() = withState(viewModel) { state ->
        updateMap(state)
        updateUserAvatar(state.userItem)
        if(!hasUpdatedPin && state.pinDrawable != null) {
            hasUpdatedPin = true
            updateStaticPin(state.pinDrawable)
        }
        views.shareLocationGpsLoading.isGone = state.lastKnownUserLocation != null
    }

    private fun handleLocationNotAvailableError() {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.location_not_available_dialog_title)
                .setMessage(R.string.location_not_available_dialog_content)
                .setPositiveButton(R.string.ok) { _, _ ->
                    activity?.finish()
                }
                .setCancelable(false)
                .show()
    }

    private fun initOptionsPicker() {
        // TODO
        //  move pin creation into the Fragment
        //  create a useCase to compare pinnedLocation and userLocation
        //  change the pin dynamically depending on the current chosen location: cf. LocationPinProvider
        //  reset map to user location when clicking on reset icon
        //  need changes in the event sent when this is a pin drop location?
        //  need changes in the parsing of events when receiving pin drop location?: should it be shown with user avatar or with pin?
        // set no option at start
        views.shareLocationOptionsPicker.render()
        views.shareLocationOptionsPicker.optionPinned.debouncedClicks {
            // TODO
        }
        views.shareLocationOptionsPicker.optionUserCurrent.debouncedClicks {
            viewModel.handle(LocationSharingAction.OnShareLocation)
        }
        views.shareLocationOptionsPicker.optionUserLive.debouncedClicks {
            // TODO
        }
    }

    private fun updateMap(state: LocationSharingViewState) {
        // first, update the options view
        // TODO compute distance between userLocation and location at center of map
        val isUserLocation = true
        if (isUserLocation) {
            // TODO activate USER_LIVE option when implemented
            views.shareLocationOptionsPicker.render(
                    LocationSharingOption.USER_CURRENT
            )
        } else {
            views.shareLocationOptionsPicker.render(
                    LocationSharingOption.PINNED
            )
        }
        // then, update the map using the height of the options view after it has been rendered
        views.shareLocationOptionsPicker.post {
            val mapState = state
                    .toMapState()
                    .copy(logoMarginBottom = views.shareLocationOptionsPicker.height)
            views.mapView.render(mapState)
        }
    }

    private fun updateUserAvatar(userItem: MatrixItem.UserItem?) {
        userItem?.takeUnless { hasRenderedUserAvatar }
                ?.let {
                    hasRenderedUserAvatar = true
                    avatarRenderer.render(it, views.shareLocationOptionsPicker.optionUserCurrent.iconView)
                    val tintColor = matrixItemColorProvider.getColor(it)
                    views.shareLocationOptionsPicker.optionUserCurrent.setIconBackgroundTint(tintColor)
                }
    }

    private fun updateStaticPin(drawable: Drawable) {
        views.shareLocationPin.setImageDrawable(drawable)
    }
}
