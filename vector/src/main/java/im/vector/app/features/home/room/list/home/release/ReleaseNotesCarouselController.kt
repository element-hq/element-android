/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home.release

import com.airbnb.epoxy.TypedEpoxyController
import javax.inject.Inject

class ReleaseNotesCarouselController @Inject constructor() : TypedEpoxyController<ReleaseCarouselData>() {
    override fun buildModels(data: ReleaseCarouselData) {
        data.items.forEachIndexed { index, item ->
            releaseCarouselItem {
                id(index)
                item(item)
            }
        }
    }
}
