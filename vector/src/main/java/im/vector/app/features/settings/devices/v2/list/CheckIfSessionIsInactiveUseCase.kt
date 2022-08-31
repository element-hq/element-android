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

import im.vector.app.core.resources.DateProvider
import im.vector.app.core.resources.toTimestamp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CheckIfSessionIsInactiveUseCase @Inject constructor() {

    fun execute(lastSeenTs: Long): Boolean {
        val lastSeenDate = DateProvider.toLocalDateTime(lastSeenTs)
        val currentDate = DateProvider.currentLocalDateTime()
        val diffMilliseconds = currentDate.toTimestamp() - lastSeenDate.toTimestamp()
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMilliseconds)
        return diffDays >= SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS
    }
}
