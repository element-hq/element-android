/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.app.core.extensions.postLiveEvent
import im.vector.app.core.utils.LiveEvent
import im.vector.app.features.configuration.VectorConfiguration
import timber.log.Timber
import javax.inject.Inject

class ConfigurationViewModel @Inject constructor(
        private val vectorConfiguration: VectorConfiguration
) : ViewModel() {

    private var currentConfigurationValue: String? = null

    private val _activityRestarter = MutableLiveData<LiveEvent<Unit>>()
    val activityRestarter: LiveData<LiveEvent<Unit>>
        get() = _activityRestarter

    fun onActivityResumed() {
        if (currentConfigurationValue == null) {
            currentConfigurationValue = vectorConfiguration.getHash()
            Timber.v("Configuration: init to $currentConfigurationValue")
        } else {
            val newHash = vectorConfiguration.getHash()
            Timber.v("Configuration: newHash $newHash")

            if (newHash != currentConfigurationValue) {
                Timber.v("Configuration: recreate the Activity")
                currentConfigurationValue = newHash
                _activityRestarter.postLiveEvent(Unit)
            }
        }
    }
}
