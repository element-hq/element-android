/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.comparators

import org.matrix.android.sdk.api.interfaces.DatedObject

object DatedObjectComparators {

    /**
     * Comparator to sort DatedObjects from the oldest to the latest.
     */
    val ascComparator by lazy {
        Comparator<DatedObject> { datedObject1, datedObject2 ->
            (datedObject1.date - datedObject2.date).toInt()
        }
    }

    /**
     * Comparator to sort DatedObjects from the latest to the oldest.
     */
    val descComparator by lazy {
        Comparator<DatedObject> { datedObject1, datedObject2 ->
            (datedObject2.date - datedObject1.date).toInt()
        }
    }
}
