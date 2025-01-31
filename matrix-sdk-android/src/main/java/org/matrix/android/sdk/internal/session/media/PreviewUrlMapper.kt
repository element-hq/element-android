/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.media

import org.matrix.android.sdk.api.session.media.PreviewUrlData
import org.matrix.android.sdk.internal.database.model.PreviewUrlCacheEntity

/**
 * PreviewUrlCacheEntity -> PreviewUrlData.
 */
internal fun PreviewUrlCacheEntity.toDomain() = PreviewUrlData(
        url = urlFromServer ?: url,
        siteName = siteName,
        title = title,
        description = description,
        mxcUrl = mxcUrl,
        imageWidth = imageWidth,
        imageHeight = imageHeight
)
