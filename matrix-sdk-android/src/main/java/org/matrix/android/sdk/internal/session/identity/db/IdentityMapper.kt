/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.identity.db

import org.matrix.android.sdk.internal.session.identity.data.IdentityData
import org.matrix.android.sdk.internal.session.identity.data.IdentityPendingBinding

internal object IdentityMapper {

    fun map(entity: IdentityDataEntity): IdentityData {
        return IdentityData(
                identityServerUrl = entity.identityServerUrl,
                token = entity.token,
                hashLookupPepper = entity.hashLookupPepper,
                hashLookupAlgorithm = entity.hashLookupAlgorithm.toList(),
                userConsent = entity.userConsent
        )
    }

    fun map(entity: IdentityPendingBindingEntity): IdentityPendingBinding {
        return IdentityPendingBinding(
                clientSecret = entity.clientSecret,
                sendAttempt = entity.sendAttempt,
                sid = entity.sid
        )
    }
}
