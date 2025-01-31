/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.failure

import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.session.room.alias.RoomAliasError

sealed class CreateRoomFailure : Failure.FeatureFailure() {
    data class CreatedWithTimeout(val roomID: String) : CreateRoomFailure()
    data class CreatedWithFederationFailure(val matrixError: MatrixError) : CreateRoomFailure()
    data class AliasError(val aliasError: RoomAliasError) : CreateRoomFailure()
}
