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

package im.vector.app.core.extensions

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.matrix.android.sdk.api.extensions.ensurePrefix
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.identity.ThreePid

fun ThreePid.getFormattedValue(): String {
    return when (this) {
        is ThreePid.Email  -> email
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
