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

package org.matrix.android.sdk.internal.session.space

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.space.peeking.DefaultPeekSpaceTask
import org.matrix.android.sdk.internal.session.space.peeking.PeekSpaceTask
import retrofit2.Retrofit

@Module
internal abstract class SpaceModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesSpacesAPI(retrofit: Retrofit): SpaceApi {
            return retrofit.create(SpaceApi::class.java)
        }
    }

    @Binds
    abstract fun bindResolveSpaceTask(task: DefaultResolveSpaceInfoTask): ResolveSpaceInfoTask

    @Binds
    abstract fun bindPeekSpaceTask(task: DefaultPeekSpaceTask): PeekSpaceTask

    @Binds
    abstract fun bindJoinSpaceTask(task: DefaultJoinSpaceTask): JoinSpaceTask
}
