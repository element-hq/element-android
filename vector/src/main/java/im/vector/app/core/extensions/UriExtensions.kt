/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.net.Uri

const val IGNORED_SCHEMA = "ignored"

fun Uri.isIgnored() = scheme == IGNORED_SCHEMA

fun createIgnoredUri(path: String): Uri = Uri.parse("$IGNORED_SCHEMA://$path")
