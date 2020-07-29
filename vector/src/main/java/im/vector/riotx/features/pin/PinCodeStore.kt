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

package im.vector.riotx.features.pin

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

interface PinCodeStore {

    fun storeEncodedPin(encodePin: String)

    fun deleteEncodedPin()

    fun getEncodedPin(): String?
}

class SharedPrefPinCodeStore @Inject constructor(private val sharedPreferences: SharedPreferences) : PinCodeStore {

    override fun storeEncodedPin(encodePin: String) {
        sharedPreferences.edit {
            putString(ENCODED_PIN_CODE_KEY, encodePin)
        }
    }

    override fun deleteEncodedPin() {
        sharedPreferences.edit {
            remove(ENCODED_PIN_CODE_KEY)
        }
    }

    override fun getEncodedPin(): String? {
        return sharedPreferences.getString(ENCODED_PIN_CODE_KEY, null)
    }

    companion object {
        const val ENCODED_PIN_CODE_KEY = "ENCODED_PIN_CODE_KEY"
    }
}
