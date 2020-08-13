/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.pin

import android.content.SharedPreferences
import androidx.core.content.edit
import com.beautycoder.pflockscreen.security.PFResult
import com.beautycoder.pflockscreen.security.PFSecurityManager
import com.beautycoder.pflockscreen.security.callbacks.PFPinCodeHelperCallback
import org.matrix.android.sdk.api.extensions.orFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface PinCodeStore {

    suspend fun storeEncodedPin(encodePin: String)

    suspend fun deleteEncodedPin()

    fun getEncodedPin(): String?

    suspend fun hasEncodedPin(): Boolean
}

class SharedPrefPinCodeStore @Inject constructor(private val sharedPreferences: SharedPreferences) : PinCodeStore {

    override suspend fun storeEncodedPin(encodePin: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            putString(ENCODED_PIN_CODE_KEY, encodePin)
        }
    }

    override suspend fun deleteEncodedPin() = withContext(Dispatchers.IO) {
        sharedPreferences.edit {
            remove(ENCODED_PIN_CODE_KEY)
        }
        awaitPinCodeCallback<Boolean> {
            PFSecurityManager.getInstance().pinCodeHelper.delete(it)
        }
        return@withContext
    }

    override fun getEncodedPin(): String? {
        return sharedPreferences.getString(ENCODED_PIN_CODE_KEY, null)
    }

    override suspend fun hasEncodedPin(): Boolean = withContext(Dispatchers.IO) {
        val hasEncodedPin = getEncodedPin()?.isNotBlank().orFalse()
        if (!hasEncodedPin) {
            return@withContext false
        }
        val result = awaitPinCodeCallback<Boolean> {
            PFSecurityManager.getInstance().pinCodeHelper.isPinCodeEncryptionKeyExist(it)
        }
        result.error == null && result.result
    }

    private suspend inline fun <T> awaitPinCodeCallback(crossinline callback: (PFPinCodeHelperCallback<T>) -> Unit) = suspendCoroutine<PFResult<T>> { cont ->
        callback(PFPinCodeHelperCallback<T> { result -> cont.resume(result) })
    }

    companion object {
        private const val ENCODED_PIN_CODE_KEY = "ENCODED_PIN_CODE_KEY"
    }
}
