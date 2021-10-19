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

package im.vector.app.features.displayname

import org.matrix.android.sdk.api.util.MatrixItem

fun MatrixItem.getBestName(): String {
    // Note: this code is copied from [DisplayNameResolver] in the SDK
    return if (this is MatrixItem.GroupItem || this is MatrixItem.RoomAliasItem) {
        // Best name is the id, and we keep the displayName of the room for the case we need the first letter
        id
    } else {
        displayName
                ?.takeIf { it.isNotBlank() }
                ?: VectorMatrixItemDisplayNameFallbackProvider.getDefaultName(this)
    }
}
