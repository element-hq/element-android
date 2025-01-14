/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
            else -> parseNumber(rawPhoneNumber)
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
