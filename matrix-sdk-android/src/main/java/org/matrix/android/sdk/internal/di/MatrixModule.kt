/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.di

import android.content.Context
import android.content.res.Resources
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.matrix.android.sdk.api.MatrixCoroutineDispatchers
import org.matrix.android.sdk.internal.util.createBackgroundHandler
import org.matrix.olm.OlmManager
import java.io.File
import java.util.concurrent.Executors

@Module
internal object MatrixModule {

    @JvmStatic
    @Provides
    @MatrixScope
    fun providesMatrixCoroutineDispatchers(): MatrixCoroutineDispatchers {
        return MatrixCoroutineDispatchers(
                io = Dispatchers.IO,
                computation = Dispatchers.Default,
                main = Dispatchers.Main,
                crypto = createBackgroundHandler("Matrix-Crypto_Thread").asCoroutineDispatcher(),
                dmVerif = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        )
    }

    @JvmStatic
    @Provides
    fun providesResources(context: Context): Resources {
        return context.resources
    }

    @JvmStatic
    @Provides
    @CacheDirectory
    fun providesCacheDir(context: Context): File {
        return context.cacheDir
    }

    @JvmStatic
    @Provides
    @MatrixScope
    fun providesOlmManager(): OlmManager {
        return OlmManager()
    }
}
