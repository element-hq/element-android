/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

private const val RTL_OVERRIDE_CHAR = '\u202E'
private const val LTR_OVERRIDE_CHAR = '\u202D'

fun String.ensureEndsLeftToRight() = if (containsRtLOverride()) "$this$LTR_OVERRIDE_CHAR" else this

fun String.containsRtLOverride() = contains(RTL_OVERRIDE_CHAR)

fun String.filterDirectionOverrides() = filterNot { it == RTL_OVERRIDE_CHAR || it == LTR_OVERRIDE_CHAR }
