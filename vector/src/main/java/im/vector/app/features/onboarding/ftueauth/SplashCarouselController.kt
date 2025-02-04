/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import com.airbnb.epoxy.TypedEpoxyController
import javax.inject.Inject

class SplashCarouselController @Inject constructor() : TypedEpoxyController<SplashCarouselState>() {
    override fun buildModels(data: SplashCarouselState) {
        data.items.forEachIndexed { index, item ->
            splashCarouselItem {
                id(index)
                item(item)
            }
        }
    }
}
