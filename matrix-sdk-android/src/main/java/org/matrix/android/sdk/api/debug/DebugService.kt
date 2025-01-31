/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2022 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.debug

import io.realm.RealmConfiguration

/**
 * Useful methods to access to some private data managed by the SDK.
 */
interface DebugService {
    /**
     * Get all the available Realm Configuration.
     */
    fun getAllRealmConfigurations(): List<RealmConfiguration>

    /**
     * Get info on DB size.
     */
    fun getDbUsageInfo(): String
}
