/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.alias

import im.vector.app.core.platform.VectorViewModelAction
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility

sealed class RoomAliasAction : VectorViewModelAction {
    // Canonical
    object ToggleManualPublishForm : RoomAliasAction()
    data class SetNewAlias(val alias: String) : RoomAliasAction()
    object ManualPublishAlias : RoomAliasAction()
    data class PublishAlias(val alias: String) : RoomAliasAction()
    data class UnpublishAlias(val alias: String) : RoomAliasAction()
    data class SetCanonicalAlias(val canonicalAlias: String?) : RoomAliasAction()

    // Room directory
    data class SetRoomDirectoryVisibility(val roomDirectoryVisibility: RoomDirectoryVisibility) : RoomAliasAction()

    // Local
    data class RemoveLocalAlias(val alias: String) : RoomAliasAction()
    object ToggleAddLocalAliasForm : RoomAliasAction()
    data class SetNewLocalAliasLocalPart(val aliasLocalPart: String) : RoomAliasAction()
    object AddLocalAlias : RoomAliasAction()

    // Retry to fetch data in error
    object Retry : RoomAliasAction()
}
