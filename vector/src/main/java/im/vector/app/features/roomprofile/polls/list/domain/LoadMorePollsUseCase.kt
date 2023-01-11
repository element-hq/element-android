/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.list.domain

import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject

// TODO add unit tests
class LoadMorePollsUseCase @Inject constructor() {

    suspend fun execute(roomId: String) {
        // TODO call repository to load more polls to be published in a flow
        Timber.d("roomId=$roomId")
        delay(5000)
    }
}
