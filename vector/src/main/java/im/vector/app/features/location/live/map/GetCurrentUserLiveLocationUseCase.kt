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

import im.vector.app.features.location.LocationData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetCurrentUserLiveLocationUseCase @Inject constructor() {

    // TODO add unit tests
    fun execute(): Flow<List<UserLiveLocationViewState>> {
        // TODO get room and call SDK to get the correct flow
        return flow {
            val user1 = UserLiveLocationViewState(
                    userId = "user1",
                    locationData = LocationData(
                            latitude = 48.863447,
                            longitude = 2.328608,
                            uncertainty = null
                    )
            )
            val user2 = UserLiveLocationViewState(
                    userId = "user2",
                    locationData = LocationData(
                            latitude = 48.843816,
                            longitude = 2.359235,
                            uncertainty = null
                    )
            )
            emit(listOf(user1, user2))
        }
    }
}
