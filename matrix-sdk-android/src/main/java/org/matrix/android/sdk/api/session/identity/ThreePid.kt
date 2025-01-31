/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.identity

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.internal.session.profile.ThirdPartyIdentifier

sealed class ThreePid(open val value: String) {
    @JsonClass(generateAdapter = true)
    data class Email(val email: String) : ThreePid(email)

    @JsonClass(generateAdapter = true)
    data class Msisdn(val msisdn: String) : ThreePid(msisdn)
}

internal fun ThreePid.toMedium(): String {
    return when (this) {
        is ThreePid.Email -> ThirdPartyIdentifier.MEDIUM_EMAIL
        is ThreePid.Msisdn -> ThirdPartyIdentifier.MEDIUM_MSISDN
    }
}

@Throws(NumberParseException::class)
internal fun ThreePid.Msisdn.getCountryCode(): String {
    return with(PhoneNumberUtil.getInstance()) {
        getRegionCodeForCountryCode(parse("+$msisdn", null).countryCode)
    }
}
