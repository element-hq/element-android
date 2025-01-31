/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.content

import dagger.Binds
import dagger.Module
import org.matrix.android.sdk.api.session.content.ContentUploadStateTracker
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.file.ContentDownloadStateTracker
import org.matrix.android.sdk.internal.session.download.DefaultContentDownloadStateTracker

@Module
internal abstract class ContentModule {

    @Binds
    abstract fun bindContentUploadStateTracker(tracker: DefaultContentUploadStateTracker): ContentUploadStateTracker

    @Binds
    abstract fun bindContentDownloadStateTracker(tracker: DefaultContentDownloadStateTracker): ContentDownloadStateTracker

    @Binds
    abstract fun bindContentUrlResolver(resolver: DefaultContentUrlResolver): ContentUrlResolver
}
