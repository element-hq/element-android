/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import io.realm.annotations.RealmModule

/**
 * Realm module for identity server classes.
 */
@RealmModule(
        library = true,
        classes = [
            IdentityDataEntity::class,
            IdentityPendingBindingEntity::class
        ]
)
internal class IdentityRealmModule
