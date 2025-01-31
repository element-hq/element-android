/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.Index

internal open class CurrentStateEventEntity(
        var eventId: String = "",
        var root: EventEntity? = null,
        @Index var roomId: String = "",
        @Index var type: String = "",
        @Index var stateKey: String = ""
) : RealmObject() {
    companion object
}
