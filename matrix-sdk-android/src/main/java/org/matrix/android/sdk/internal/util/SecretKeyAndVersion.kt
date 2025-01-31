/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import javax.crypto.SecretKey

/**
 * Tuple which contains the secret key and the version of Android when the key has been generated.
 */
internal data class SecretKeyAndVersion(
        /**
         * the key.
         */
        val secretKey: SecretKey,
        /**
         * The android version when the key has been generated.
         */
        val androidVersionWhenTheKeyHasBeenGenerated: Int
)
