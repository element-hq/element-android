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

package im.vector.app.features.location.domain.usecase

import im.vector.app.features.location.LocationData
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.Session
import javax.inject.Inject

/**
 * Use case to check if 2 locations can be considered as equal.
 */
class CompareLocationsUseCase @Inject constructor(
        private val session: Session
) {

    // TODO unit test
    /**
     * Compare the 2 given locations.
     * @return true when they are really close and could be considered as the same location, false otherwise
     */
    suspend fun execute(location1: LocationData, location2: LocationData): Boolean =
            withContext(session.coroutineDispatchers.io) {
                // TODO implement real comparison
                location1 == location2
            }
}
