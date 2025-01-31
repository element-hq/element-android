/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database

import io.realm.DefaultCompactOnLaunchCallback

class RealmCompactOnLaunch : DefaultCompactOnLaunchCallback() {
    /**
     * Forces all RealmCompactOnLaunch instances to be equal.
     * Avoids Realm throwing when multiple instances of this class are used.
     */
    override fun equals(other: Any?) = other is RealmCompactOnLaunch
    override fun hashCode() = 0x1000
}
