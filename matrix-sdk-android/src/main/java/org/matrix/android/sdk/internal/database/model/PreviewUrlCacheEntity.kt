/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

internal open class PreviewUrlCacheEntity(
        @PrimaryKey
        var url: String = "",

        var urlFromServer: String? = null,
        var siteName: String? = null,
        var title: String? = null,
        var description: String? = null,
        var mxcUrl: String? = null,
        var imageWidth: Int? = null,
        var imageHeight: Int? = null,
        var lastUpdatedTimestamp: Long = 0L
) : RealmObject() {

    companion object
}
