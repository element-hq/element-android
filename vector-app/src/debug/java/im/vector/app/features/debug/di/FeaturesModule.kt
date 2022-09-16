/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
