/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.room.model

import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM

sealed class RoomEncryptionAlgorithm {

    abstract class SupportedAlgorithm(val alg: String) : RoomEncryptionAlgorithm()

    object Megolm : SupportedAlgorithm(MXCRYPTO_ALGORITHM_MEGOLM)

    data class UnsupportedAlgorithm(val name: String?) : RoomEncryptionAlgorithm()
}
