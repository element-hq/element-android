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

package im.vector.app.features.roomprofile.permissions

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import javax.inject.Inject

class RoleFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(role: Role): String {
        return when (role) {
            Role.Admin     -> stringProvider.getString(R.string.power_level_admin)
            Role.Moderator -> stringProvider.getString(R.string.power_level_moderator)
            Role.Default   -> stringProvider.getString(R.string.power_level_default)
            is Role.Custom -> stringProvider.getString(R.string.power_level_custom, role.value)
        }
    }
}
