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

package fr.gouv.tchap.features.platform

import android.content.Context
import com.airbnb.mvrx.MavericksViewModelFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.MavericksAssistedViewModelFactory
import im.vector.app.core.di.hiltMavericksViewModelFactory
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorViewModel
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.identity.ThreePid
import timber.log.Timber
import java.util.ArrayList
import java.util.Arrays
import java.util.Random

class PlatformViewModel @AssistedInject constructor(
        @Assisted initialState: PlatformViewState,
        private val context: Context,
        private val activeSessionHolder: ActiveSessionHolder,
        private val matrix: Matrix
) : VectorViewModel<PlatformViewState, PlatformAction, PlatformViewEvents>(initialState) {

    @AssistedFactory
    interface Factory : MavericksAssistedViewModelFactory<PlatformViewModel, PlatformViewState> {
        override fun create(initialState: PlatformViewState): PlatformViewModel
    }

    companion object : MavericksViewModelFactory<PlatformViewModel, PlatformViewState> by hiltMavericksViewModelFactory()

    /**
     * Get the Tchap platform configuration (HS/IS) for the provided email address.
     *
     * @param activity     current activity
     * @param emailAddress the email address to consider
     * @param callback     the asynchronous callback
     */
    private fun handleDiscoverTchapPlatform(action: PlatformAction.DiscoverTchapPlatform) {
        Timber.d("## discoverTchapPlatform [${action.email}]")
        // Prepare the list of the known ISes in order to run over the list until to get an answer.
        val idServerUrls: MutableList<String> = ArrayList()

        // Consider first the current identity server if any.
        var currentIdServerUrl: String? = null

        if (activeSessionHolder.hasActiveSession()) {
            currentIdServerUrl = activeSessionHolder.getActiveSession().identityService().getCurrentIdentityServerUrl()
            if (currentIdServerUrl != null) {
                idServerUrls.add(currentIdServerUrl)
            }
        }

        // Add randomly the preferred known ISes
        var currentHosts: MutableList<String> = ArrayList(Arrays.asList(*context.resources.getStringArray(R.array.preferred_identity_server_names)))
        while (!currentHosts.isEmpty()) {
            val index = Random().nextInt(currentHosts.size)
            val host: String = currentHosts.removeAt(index)
            val idServerUrl = context.getString(R.string.server_url_prefix) + host
            if (null == currentIdServerUrl || idServerUrl != currentIdServerUrl) {
                idServerUrls.add(idServerUrl)
            }
        }

        // Add randomly the other known ISes
        currentHosts = ArrayList(Arrays.asList(*context.resources.getStringArray(R.array.identity_server_names)))
        while (!currentHosts.isEmpty()) {
            val index = Random().nextInt(currentHosts.size)
            val host: String = currentHosts.removeAt(index)
            val idServerUrl = context.getString(R.string.server_url_prefix) + host
            if (null == currentIdServerUrl || idServerUrl != currentIdServerUrl) {
                idServerUrls.add(idServerUrl)
            }
        }
        handleDiscoverTchapPlatform(action.email, idServerUrls)
    }

    /**
     * Run over all the provided hosts by removing them one by one until we get the Tchap platform for the provided email address.
     *
     * @param emailAddress       the email address to consider
     * @param identityServerUrls the list of the available identity server urls
     * @param callback           the asynchronous callback
     */
    private fun handleDiscoverTchapPlatform(emailAddress: String, identityServerUrls: MutableList<String>) {
        if (identityServerUrls.isEmpty()) {
            _viewEvents.post(PlatformViewEvents.Failure(Failure.ServerError(MatrixError(
                    code = MatrixError.M_UNKNOWN,
                    message = "No host"
            ), 403)))
        }

        // Retrieve the first identity server url by removing it from the list.
        val selectedUrl: String = identityServerUrls.removeAt(0)

        viewModelScope.launch {
            try {
                val platform = matrix.threePidPlatformDiscoverService().getPlatform(selectedUrl, ThreePid.Email(emailAddress))
                _viewEvents.post(PlatformViewEvents.Success(platform))
                Timber.d("## discoverTchapPlatform succeeded (" + platform.hs.toString() + ")")
            } catch (failure: Throwable) {
                Timber.e(failure, "## discoverTchapPlatform failed ")
                if (identityServerUrls.isEmpty()) {
                    // We checked all the known hosts, return the error
                    _viewEvents.post(PlatformViewEvents.Failure(failure))
                } else {
                    // Try again
                    handleDiscoverTchapPlatform(emailAddress, identityServerUrls)
                }
            }
        }
    }

    override fun handle(action: PlatformAction) {
        when (action) {
            is PlatformAction.DiscoverTchapPlatform -> handleDiscoverTchapPlatform(action)
        }.exhaustive
    }
}
