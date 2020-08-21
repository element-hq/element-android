/*
 * Copyright 2018 New Vector Ltd
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

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.matrix.android.sdk.api.extensions.ensurePrefix

fun Boolean.toOnOff() = if (this) "ON" else "OFF"

inline fun <T> T.ooi(block: (T) -> Unit): T = also(block)

/**
 * Apply argument to a Fragment
 */
fun <T : Fragment> T.withArgs(block: Bundle.() -> Unit) = apply { arguments = Bundle().apply(block) }

/**
 * Check if a CharSequence is an email
 */
fun CharSequence.isEmail() = Patterns.EMAIL_ADDRESS.matcher(this).matches()

/**
 * Check if a CharSequence is a phone number
 */
fun CharSequence.isMsisdn(): Boolean {
    return try {
        PhoneNumberUtil.getInstance().parse(ensurePrefix("+"), null)
        true
    } catch (e: NumberParseException) {
        false
    }
}
