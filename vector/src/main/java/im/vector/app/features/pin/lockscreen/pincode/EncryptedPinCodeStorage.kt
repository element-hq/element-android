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
