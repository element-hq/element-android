/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.identity

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.matrix.android.sdk.internal.session.profile.ThirdPartyIdentifier

sealed class ThreePid(open val value: String) {
    data class Email(val email: String) : ThreePid(email)
    data class Msisdn(val msisdn: String) : ThreePid(msisdn)
}

internal fun ThreePid.toMedium(): String {
    return when (this) {
        is ThreePid.Email  -> ThirdPartyIdentifier.MEDIUM_EMAIL
        is ThreePid.Msisdn -> ThirdPartyIdentifier.MEDIUM_MSISDN
    }
}

@Throws(NumberParseException::class)
internal fun ThreePid.Msisdn.getCountryCode(): String {
    return with(PhoneNumberUtil.getInstance()) {
        getRegionCodeForCountryCode(parse("+$msisdn", null).countryCode)
    }
}
