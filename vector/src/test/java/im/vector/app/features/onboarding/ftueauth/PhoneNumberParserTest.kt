/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import im.vector.app.test.fakes.FakePhoneNumberUtil
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

private const val AN_INTERNATIONAL_PHONE_NUMBER = "+4411111111111"
private const val A_NON_INTERNATIONAL_PHONE_NUMBER = "111111111111"
private const val AN_INVALID_INTERNATIONAL_NUMBER = "+abc"
private const val A_COUNTRY_CODE = "GB"
private const val A_COUNTRY_CALLING_CODE = 44

class PhoneNumberParserTest {

    private val fakePhoneNumberUtil = FakePhoneNumberUtil()
    private val phoneNumberParser = PhoneNumberParser(fakePhoneNumberUtil.instance)

    @Test
    fun `given a calling code and country code are successfully read when parsing, then returns success with country code`() {
        fakePhoneNumberUtil.givenCountryCallingCodeFor(AN_INTERNATIONAL_PHONE_NUMBER, callingCode = A_COUNTRY_CALLING_CODE)
        fakePhoneNumberUtil.givenRegionCodeFor(A_COUNTRY_CALLING_CODE, countryCode = A_COUNTRY_CODE)

        val result = phoneNumberParser.parseInternationalNumber(AN_INTERNATIONAL_PHONE_NUMBER)

        result shouldBeEqualTo PhoneNumberParser.Result.Success(A_COUNTRY_CODE, AN_INTERNATIONAL_PHONE_NUMBER)
    }

    @Test
    fun `given a non internation phone number, when parsing, then returns MissingInternationCode error`() {
        val result = phoneNumberParser.parseInternationalNumber(A_NON_INTERNATIONAL_PHONE_NUMBER)

        result shouldBeEqualTo PhoneNumberParser.Result.ErrorMissingInternationalCode
    }

    @Test
    fun `given an invalid phone number, when parsing, then returns ErrorInvalidNumber error`() {
        fakePhoneNumberUtil.givenFailsToParse(AN_INVALID_INTERNATIONAL_NUMBER)

        val result = phoneNumberParser.parseInternationalNumber(AN_INVALID_INTERNATIONAL_NUMBER)

        result shouldBeEqualTo PhoneNumberParser.Result.ErrorInvalidNumber
    }
}
