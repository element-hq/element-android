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

import android.util.Patterns
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.matrix.android.sdk.api.extensions.ensurePrefix

fun Boolean.toOnOff() = if (this) "ON" else "OFF"

inline fun <T> T.ooi(block: (T) -> Unit): T = also(block)

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

/**
 * Useful to append a String at the end of a filename but before the extension if any
 * Ex:
 * - "file.txt".insertBeforeLast("_foo") will return "file_foo.txt"
 * - "file".insertBeforeLast("_foo") will return "file_foo"
 * - "fi.le.txt".insertBeforeLast("_foo") will return "fi.le_foo.txt"
 * - null.insertBeforeLast("_foo") will return "_foo"
 */
fun String?.insertBeforeLast(insert: String, delimiter: String = "."): String {
    if (this == null) return insert
    val idx = lastIndexOf(delimiter)
    return if (idx == -1) {
        this + insert
    } else {
        replaceRange(idx, idx, insert)
    }
}

inline fun <reified R> Any?.takeAs(): R? {
    return takeIf { it is R } as R?
}
