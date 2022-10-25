/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
