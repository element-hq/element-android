/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.lib.multipicker.utils

internal fun String?.isMimeTypeImage() = this?.startsWith("image/") == true
internal fun String?.isMimeTypeVideo() = this?.startsWith("video/") == true
internal fun String?.isMimeTypeAudio() = this?.startsWith("audio/") == true
