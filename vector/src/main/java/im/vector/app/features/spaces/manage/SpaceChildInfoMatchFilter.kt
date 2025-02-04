/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces.manage

import androidx.core.util.Predicate
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.SpaceChildInfo

class SpaceChildInfoMatchFilter : Predicate<SpaceChildInfo> {
    var filter: String = ""

    override fun test(spaceChildInfo: SpaceChildInfo): Boolean {
        if (filter.isEmpty()) {
            // No filter
            return true
        }
        // if filter is "Jo Do", it should match "John Doe"
        return filter.split(" ").all {
            spaceChildInfo.name?.contains(it, ignoreCase = true).orFalse() ||
                    spaceChildInfo.topic?.contains(it, ignoreCase = true).orFalse()
        }
    }
}
