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
