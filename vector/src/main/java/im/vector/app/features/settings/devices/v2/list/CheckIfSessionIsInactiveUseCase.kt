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

package im.vector.app.features.settings.devices.v2.list

import im.vector.app.core.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckIfSessionIsInactiveUseCase @Inject constructor(
        private val clock: Clock,
) {

    fun execute(lastSeenTsMillis: Long?): Boolean {
        return if (lastSeenTsMillis == null || lastSeenTsMillis <= 0) {
            // in these situations we cannot say anything about the inactivity of the session
            false
        } else {
            val diffMilliseconds = clock.epochMillis() - lastSeenTsMillis
            diffMilliseconds >= TimeUnit.DAYS.toMillis(SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS.toLong())
        }
    }
}
