/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.pin

import android.content.SharedPreferences
import androidx.core.content.edit
import im.vector.app.features.pin.lockscreen.pincode.EncryptedPinCodeStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface PinCodeStore : EncryptedPinCodeStorage {

    /**
     * Returns the remaining PIN code attempts. When this reaches 0 the PIN code access won't be available for some time.
     */
    fun getRemainingPinCodeAttemptsNumber(): Int

    /**
     * Should decrement the number of remaining PIN code attempts.
     * @return The remaining attempts.
     */
    fun onWrongPin(): Int

    /**
     * Resets the counter of attempts for PIN code and biometric access.
     */
    fun resetCounter()

    /**
     * Adds a listener to be notified when the PIN code us created or removed.
     */
    fun addListener(listener: PinCodeStoreListener)

    /**
     * Removes a listener to be notified when the PIN code us created or removed.
     */
    fun removeListener(listener: PinCodeStoreListener)
}

interface PinCodeStoreListener {
    fun onPinSetUpChange(isConfigured: Boolean)
}

@Singleton
class SharedPrefPinCodeStore @Inject constructor(private val sharedPreferences: SharedPreferences) : PinCodeStore, EncryptedPinCodeStorage {
    private val listeners = mutableSetOf<PinCodeStoreListener>()

    override suspend fun getPinCode(): String? {
        return sharedPreferences.getString(ENCODED_PIN_CODE_KEY, null)
    }

    override suspend fun savePinCode(pinCode: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit {
                putString(ENCODED_PIN_CODE_KEY, pinCode)
            }
        }
        listeners.forEach { it.onPinSetUpChange(isConfigured = true) }
    }

    override suspend fun deletePinCode() {
        withContext(Dispatchers.IO) {
            // Also reset the counters
            resetCounter()
            sharedPreferences.edit {
                remove(ENCODED_PIN_CODE_KEY)
            }
        }
        listeners.forEach { it.onPinSetUpChange(isConfigured = false) }
    }

    override suspend fun hasEncodedPin(): Boolean {
        return withContext(Dispatchers.IO) { sharedPreferences.contains(ENCODED_PIN_CODE_KEY) }
    }

    override fun getRemainingPinCodeAttemptsNumber(): Int {
        return sharedPreferences.getInt(REMAINING_PIN_CODE_ATTEMPTS_KEY, MAX_PIN_CODE_ATTEMPTS_NUMBER_BEFORE_LOGOUT)
    }

    override fun onWrongPin(): Int {
        val remaining = getRemainingPinCodeAttemptsNumber() - 1
        sharedPreferences.edit {
            putInt(REMAINING_PIN_CODE_ATTEMPTS_KEY, remaining)
        }
        return remaining
    }

    override fun resetCounter() {
        sharedPreferences.edit {
            remove(REMAINING_PIN_CODE_ATTEMPTS_KEY)
            remove(REMAINING_BIOMETRICS_ATTEMPTS_KEY)
        }
    }

    override fun addListener(listener: PinCodeStoreListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PinCodeStoreListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val ENCODED_PIN_CODE_KEY = "ENCODED_PIN_CODE_KEY"
        private const val REMAINING_PIN_CODE_ATTEMPTS_KEY = "REMAINING_PIN_CODE_ATTEMPTS_KEY"
        private const val REMAINING_BIOMETRICS_ATTEMPTS_KEY = "REMAINING_BIOMETRICS_ATTEMPTS_KEY"

        private const val MAX_PIN_CODE_ATTEMPTS_NUMBER_BEFORE_LOGOUT = 3
    }
}
