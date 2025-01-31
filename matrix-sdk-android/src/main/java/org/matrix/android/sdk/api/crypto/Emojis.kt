/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.crypto

import org.matrix.android.sdk.api.session.crypto.verification.EmojiRepresentation
import org.matrix.android.sdk.internal.crypto.verification.getEmojiForCode

/**
 * Provide all the emojis used for SAS verification (for debug purpose).
 */
fun getAllVerificationEmojis(): List<EmojiRepresentation> {
    return (0..63).map { getEmojiForCode(it) }
}
