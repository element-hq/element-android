/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin.lockscreen.pincode

import im.vector.app.features.pin.lockscreen.crypto.LockScreenKeyRepository
import javax.inject.Inject

/**
 * A helper class to manage the PIN code creation, verification, removal and migration.
 */
class PinCodeHelper @Inject constructor(
        private val lockScreenKeyRepository: LockScreenKeyRepository,
        private val encryptedStorage: EncryptedPinCodeStorage,
) {

    /**
     * Returns if PIN code is available (both the key exists and the encrypted value is stored).
     */
    suspend fun isPinCodeAvailable() = lockScreenKeyRepository.hasPinCodeKey() && encryptedStorage.getPinCode() != null

    /**
     * Creates a PIN code key if needed and stores the PIN code encrypted with it.
     */
    suspend fun createPinCode(pinCode: String) {
        val encryptedValue = lockScreenKeyRepository.encryptPinCode(pinCode)
        encryptedStorage.savePinCode(encryptedValue)
    }

    /**
     * Verifies the passed [pinCode] against the encrypted one.
     */
    suspend fun verifyPinCode(pinCode: String): Boolean {
        val encryptedPinCode = encryptedStorage.getPinCode() ?: return false
        return lockScreenKeyRepository.decryptPinCode(encryptedPinCode) == pinCode
    }

    /**
     * Deletes the store PIN code as well as the associated key.
     */
    suspend fun deletePinCode() {
        encryptedStorage.deletePinCode()
        lockScreenKeyRepository.deletePinCodeKey()
    }
}
