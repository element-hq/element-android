/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.logger

/**
 * Parent class for custom logger tags. Can be used with Timber :
 *
 * val loggerTag = LoggerTag("MyTag", LoggerTag.VOIP)
 * Timber.tag(loggerTag.value).v("My log message")
 */
open class LoggerTag(name: String, parentTag: LoggerTag? = null) {

    object SYNC : LoggerTag("SYNC")
    object VOIP : LoggerTag("VOIP")
    object CRYPTO : LoggerTag("CRYPTO")

    val value: String = if (parentTag == null) {
        name
    } else {
        "${parentTag.value}/$name"
    }
}
