/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import timber.log.Timber
import java.util.Locale

/**
 * Convert a string to an UTF8 String.
 *
 * @param s the string to convert
 * @return the utf-8 string
 */
internal fun convertToUTF8(s: String): String {
    return try {
        val bytes = s.toByteArray(Charsets.UTF_8)
        String(bytes)
    } catch (e: Exception) {
        Timber.e(e, "## convertToUTF8()  failed")
        s
    }
}

/**
 * Convert a string from an UTF8 String.
 *
 * @param s the string to convert
 * @return the utf-16 string
 */
internal fun convertFromUTF8(s: String): String {
    return try {
        val bytes = s.toByteArray()
        String(bytes, Charsets.UTF_8)
    } catch (e: Exception) {
        Timber.e(e, "## convertFromUTF8()  failed")
        s
    }
}

/**
 * Returns whether a string contains an occurrence of another, as a standalone word, regardless of case.
 *
 * @param subString the string to search for
 * @return whether a match was found
 */
internal fun String.caseInsensitiveFind(subString: String): Boolean {
    // add sanity checks
    if (subString.isEmpty() || isEmpty()) {
        return false
    }

    try {
        val regex = Regex("(\\W|^)" + Regex.escape(subString) + "(\\W|$)", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(this)
    } catch (e: Exception) {
        Timber.e(e, "## caseInsensitiveFind() : failed")
    }

    return false
}

internal val spaceChars = "[\u00A0\u2000-\u200B\u2800\u3000]".toRegex()

/**
 * Strip all the UTF-8 chars which are actually spaces.
 */
internal fun String.replaceSpaceChars(replacement: String = "") = replace(spaceChars, replacement)

// String.capitalize is now deprecated
internal fun String.safeCapitalize(): String {
    return replaceFirstChar { char ->
        if (char.isLowerCase()) {
            char.titlecase(Locale.getDefault())
        } else {
            char.toString()
        }
    }
}

internal fun String.removeInvalidRoomNameChars() = "[^a-z0-9._%#@=+-]".toRegex().replace(this, "")
