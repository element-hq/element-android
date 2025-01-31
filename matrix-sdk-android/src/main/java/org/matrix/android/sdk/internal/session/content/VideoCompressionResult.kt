/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.content

import java.io.File

internal sealed class VideoCompressionResult {
    data class Success(val compressedFile: File) : VideoCompressionResult()
    object CompressionNotNeeded : VideoCompressionResult()
    object CompressionCancelled : VideoCompressionResult()
    data class CompressionFailed(val failure: Throwable) : VideoCompressionResult()
}
