/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.send.pills

import org.matrix.android.sdk.api.session.room.send.MatrixItemSpan

internal data class MentionLinkSpec(
        val span: MatrixItemSpan,
        val start: Int,
        val end: Int
)
