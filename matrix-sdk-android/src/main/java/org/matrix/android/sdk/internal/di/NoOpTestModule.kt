/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.internal.session.MockHttpInterceptor
import org.matrix.android.sdk.internal.session.TestInterceptor
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.DefaultBackgroundDetectionObserver

@Module
internal object NoOpTestModule {

    @Provides
    @JvmStatic
    @MockHttpInterceptor
    fun providesTestInterceptor(): TestInterceptor? = null

    @Provides
    @JvmStatic
    @MatrixScope
    fun providesBackgroundDetectionObserver(): BackgroundDetectionObserver {
        return DefaultBackgroundDetectionObserver()
    }
}
