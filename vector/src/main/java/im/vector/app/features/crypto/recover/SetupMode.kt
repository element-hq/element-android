/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

enum class SetupMode {

    /**
     * Only setup cross signing, no 4S or megolm backup.
     */
    CROSS_SIGNING_ONLY,

    /**
     * Normal setup mode.
     */
    NORMAL,

    /**
     * Only reset the 4S passphrase/key, but do not touch
     * to existing cross-signing or megolm backup.
     * It takes the local known secrets and put them in 4S.
     */
    PASSPHRASE_RESET,

    /**
     * Resets the passphrase/key, and all missing secrets
     * are re-created. Meaning that if cross signing is setup and the secrets
     * keys are not known, cross signing will be reset (if secret is known we just keep same cross signing).
     * Same apply to megolm.
     */
    PASSPHRASE_AND_NEEDED_SECRETS_RESET,

    /**
     * Resets the passphrase/key, cross signing and megolm backup.
     */
    HARD_RESET
}
