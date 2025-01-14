/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.debug.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.DefaultVectorOverrides
import im.vector.app.features.VectorFeatures
import im.vector.app.features.VectorOverrides
import im.vector.app.features.debug.features.DebugVectorFeatures
import im.vector.app.features.debug.features.DebugVectorOverrides

@InstallIn(SingletonComponent::class)
@Module
interface FeaturesModule {

    @Binds
    fun bindFeatures(debugFeatures: DebugVectorFeatures): VectorFeatures

    @Binds
    fun bindOverrides(debugOverrides: DebugVectorOverrides): VectorOverrides

    companion object {

        @Provides
        fun providesDefaultVectorFeatures(): DefaultVectorFeatures {
            return DefaultVectorFeatures()
        }

        @Provides
        fun providesDebugVectorFeatures(context: Context, defaultVectorFeatures: DefaultVectorFeatures): DebugVectorFeatures {
            return DebugVectorFeatures(context, defaultVectorFeatures)
        }

        @Provides
        fun providesDefaultVectorOverrides(): DefaultVectorOverrides {
            return DefaultVectorOverrides()
        }

        @Provides
        fun providesDebugVectorOverrides(context: Context): DebugVectorOverrides {
            return DebugVectorOverrides(context)
        }
    }
}
