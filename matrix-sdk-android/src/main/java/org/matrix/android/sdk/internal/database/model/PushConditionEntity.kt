/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject

internal open class PushConditionEntity(
        var kind: String = "",
        var key: String? = null,
        var pattern: String? = null,
        var iz: String? = null
) : RealmObject() {

    companion object
}
