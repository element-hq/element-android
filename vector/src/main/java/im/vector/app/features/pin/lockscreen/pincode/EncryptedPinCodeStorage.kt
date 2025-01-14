/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.pincode

/**
 * Should be implemented by any class that provides access to the encrypted PIN code.
 * All methods are suspending in case there are async IO operations involved.
 */
interface EncryptedPinCodeStorage {
    /**
     * Returns the encrypted PIN code.
     */
    suspend fun getPinCode(): String?

    /**
     * Saves the encrypted PIN code to some persistable storage.
     */
    suspend fun savePinCode(pinCode: String)

    /**
     * Deletes the PIN code from some persistable storage.
     */
    suspend fun deletePinCode()

    /**
     * Returns whether the encrypted PIN code is stored or not.
     */
    suspend fun hasEncodedPin(): Boolean
}
