/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.presence.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.presence.PresenceService
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.presence.PresenceAPI
import org.matrix.android.sdk.internal.session.presence.service.DefaultPresenceService
import org.matrix.android.sdk.internal.session.presence.service.task.DefaultGetPresenceTask
import org.matrix.android.sdk.internal.session.presence.service.task.DefaultSetPresenceTask
import org.matrix.android.sdk.internal.session.presence.service.task.GetPresenceTask
import org.matrix.android.sdk.internal.session.presence.service.task.SetPresenceTask
import retrofit2.Retrofit

@Module
internal abstract class PresenceModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesPresenceAPI(retrofit: Retrofit): PresenceAPI {
            return retrofit.create(PresenceAPI::class.java)
        }
    }

    @Binds
    abstract fun bindPresenceService(service: DefaultPresenceService): PresenceService

    @Binds
    abstract fun bindSetPresenceTask(task: DefaultSetPresenceTask): SetPresenceTask

    @Binds
    abstract fun bindGetPresenceTask(task: DefaultGetPresenceTask): GetPresenceTask
}
