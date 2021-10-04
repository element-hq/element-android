/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomdirectory.createroom

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import javax.inject.Inject

class RoomAliasErrorFormatter @Inject constructor(
        private val stringProvider: StringProvider
) {
    fun format(roomAliasError: RoomAliasError?): String? {
        return when (roomAliasError) {
            is RoomAliasError.AliasIsBlank      -> R.string.create_room_alias_empty
            is RoomAliasError.AliasNotAvailable -> R.string.create_room_alias_already_in_use
            is RoomAliasError.AliasInvalid      -> R.string.create_room_alias_invalid
            else                                -> null
        }
                ?.let { stringProvider.getString(it) }
    }
}
