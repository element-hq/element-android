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

/**
 * Clients can store custom config data for their account on their homeserver.
 * This account data will be synced between different devices and can persist across installations on a particular device.
 * Users may only view the account data for their own account.
 * The account_data may be either global or scoped to a particular rooms.
 */
internal open class UserAccountDataEntity(
        @Index var type: String? = null,
        var contentStr: String? = null
) : RealmObject() {

    companion object
}
