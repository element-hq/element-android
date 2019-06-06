/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.core.platform

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.riotredesign.core.utils.LiveEvent
import im.vector.riotredesign.features.configuration.VectorConfiguration
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

class ConfigurationViewModel : ViewModel(), KoinComponent {

    private val vectorConfiguration: VectorConfiguration by inject()

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

                _activityRestarter.postValue(LiveEvent(Unit))
            }
        }
    }
}
