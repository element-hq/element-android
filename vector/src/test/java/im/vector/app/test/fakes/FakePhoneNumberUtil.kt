/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import io.mockk.every
import io.mockk.mockk

class FakePhoneNumberUtil {

    val instance = mockk<PhoneNumberUtil>()

    fun givenCountryCallingCodeFor(phoneNumber: String, callingCode: Int) {
        every { instance.parse(phoneNumber, null) } returns Phonenumber.PhoneNumber().setCountryCode(callingCode)
    }

    fun givenRegionCodeFor(callingCode: Int, countryCode: String) {
        every { instance.getRegionCodeForCountryCode(callingCode) } returns countryCode
    }

    fun givenFailsToParse(phoneNumber: String) {
        every { instance.parse(phoneNumber, null) } throws NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "")
    }
}
