/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.interfaces

/**
 * Can be implemented by any object containing a timestamp.
 * This interface can be use to sort such object
 */
interface DatedObject {
    val date: Long
}
