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
