/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list

import com.airbnb.epoxy.EpoxyController
import timber.log.Timber

fun EpoxyController.setCollapsed(collapsed: Boolean) {
    if (this is CollapsableControllerExtension) {
        this.collapsed = collapsed
    } else {
        Timber.w("Try to collapse a controller that do not support collapse state")
    }
}

interface CollapsableControllerExtension {
    var collapsed: Boolean
}
