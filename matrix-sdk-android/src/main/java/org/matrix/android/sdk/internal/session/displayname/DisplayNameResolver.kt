/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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
 *
 */

package org.matrix.android.sdk.internal.session.displayname

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.util.MatrixItem
import javax.inject.Inject

internal class DisplayNameResolver @Inject constructor(
        private val matrixConfiguration: MatrixConfiguration
) {
    fun getBestName(matrixItem: MatrixItem): String {
        return if (matrixItem is MatrixItem.GroupItem || matrixItem is MatrixItem.RoomAliasItem) {
            // Best name is the id, and we keep the displayName of the room for the case we need the first letter
            matrixItem.id
        } else {
            matrixItem.displayName?.takeIf { it.isNotBlank() }
                    ?: matrixConfiguration.matrixItemDisplayNameFallbackProvider?.getDefaultName(matrixItem)
                    ?: matrixItem.id
        }
    }
}
