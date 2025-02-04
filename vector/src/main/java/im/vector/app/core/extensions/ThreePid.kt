/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.matrix.android.sdk.api.extensions.ensurePrefix
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.identity.ThreePid

fun ThreePid.getFormattedValue(): String {
    return when (this) {
        is ThreePid.Email -> email
        is ThreePid.Msisdn -> {
            tryOrNull(message = "Unable to parse the phone number") {
                PhoneNumberUtil.getInstance().parse(msisdn.ensurePrefix("+"), null)
            }
                    ?.let {
                        PhoneNumberUtil.getInstance().format(it, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                    }
                    ?: msisdn
        }
    }
}
