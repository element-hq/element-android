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

package im.vector.app.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.features.DefaultVectorFeatures
import im.vector.app.features.DefaultVectorOverrides
import im.vector.app.features.VectorFeatures
import im.vector.app.features.VectorOverrides

@InstallIn(SingletonComponent::class)
@Module
object FeaturesModule {

    @Provides
    fun providesFeatures(): VectorFeatures {
        return DefaultVectorFeatures()
    }

    @Provides
    fun providesOverrides(): VectorOverrides {
        return DefaultVectorOverrides()
    }
}
