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

package im.vector.app.features.pin.lockscreen.configuration

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class used to hold both the [defaultConfiguration] and an updated version in [currentConfiguration].
 */
@Singleton
class LockScreenConfiguratorProvider @Inject constructor(
        /** Default [LockScreenConfiguration], any derived configuration created using [updateDefaultConfiguration] will use this as a base. */
        val defaultConfiguration: LockScreenConfiguration,
) {

    private val mutableConfigurationFlow = MutableStateFlow(defaultConfiguration)

    /**
     * A [Flow] that emits any changes in configuration.
     */
    val configurationFlow: Flow<LockScreenConfiguration> = mutableConfigurationFlow

    /**
     * The current configuration to be read and used.
     */
    val currentConfiguration get() = mutableConfigurationFlow.value

    /**
     * Applies the changes in [block] to the [defaultConfiguration] to generate a new [currentConfiguration].
     */
    fun updateDefaultConfiguration(block: LockScreenConfiguration.() -> LockScreenConfiguration) {
        mutableConfigurationFlow.value = defaultConfiguration.block()
    }

    /**
     * Resets the [currentConfiguration] to the [defaultConfiguration].
     */
    fun reset() {
        mutableConfigurationFlow.value = defaultConfiguration
    }
}
