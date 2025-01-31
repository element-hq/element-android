/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model.message

/**
 * Define what particular asset is being referred to.
 * We don't use enum type since it is not limited to a specific set of values.
 * The way this type should be interpreted in client side is described in
 * [MSC3488](https://github.com/matrix-org/matrix-doc/blob/matthew/location/proposals/3488-location.md)
 */
object LocationAssetType {
    /**
     * Used for user location sharing.
     **/
    const val SELF = "m.self"

    /**
     * Used for pin drop location sharing.
     **/
    const val PIN = "m.pin"
}
