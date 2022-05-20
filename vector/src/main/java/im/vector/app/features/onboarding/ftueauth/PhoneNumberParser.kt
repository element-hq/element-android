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

package im.vector.app.features.onboarding.ftueauth

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import javax.inject.Inject

class PhoneNumberParser @Inject constructor(
        private val phoneNumberUtil: PhoneNumberUtil
) {

    fun parseInternationalNumber(rawPhoneNumber: String): Result {
        return when {
            rawPhoneNumber.doesNotStartWith("+") -> Result.ErrorMissingInternationalCode
            else                                 -> parseNumber(rawPhoneNumber)
        }
    }

    private fun parseNumber(rawPhoneNumber: String) = try {
        val phoneNumber = phoneNumberUtil.parse(rawPhoneNumber, null)
        Result.Success(phoneNumberUtil.getRegionCodeForCountryCode(phoneNumber.countryCode), rawPhoneNumber)
    } catch (e: NumberParseException) {
        Result.ErrorInvalidNumber
    }

    sealed interface Result {
        object ErrorMissingInternationalCode : Result
        object ErrorInvalidNumber : Result
        data class Success(val countryCode: String, val phoneNumber: String) : Result
    }

    private fun String.doesNotStartWith(input: String) = !startsWith(input)
}
