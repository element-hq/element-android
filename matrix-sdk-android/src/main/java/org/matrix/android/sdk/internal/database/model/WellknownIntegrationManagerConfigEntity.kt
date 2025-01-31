/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class WellknownIntegrationManagerConfigEntity(
        @PrimaryKey var id: Long = 0,
        var apiUrl: String = "",
        var uiUrl: String = ""
) : RealmObject() {

    companion object
}
