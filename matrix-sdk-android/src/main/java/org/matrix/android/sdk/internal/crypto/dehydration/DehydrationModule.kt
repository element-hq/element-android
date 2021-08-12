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

package org.matrix.android.sdk.internal.crypto.dehydration

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.session.directory.DirectoryAPI
import org.matrix.android.sdk.internal.session.room.RoomAPI
import retrofit2.Retrofit

@Module
internal abstract class DehydrationModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesDehydrationAPI(retrofit: Retrofit): DehydrationApi {
            return retrofit.create(DehydrationApi::class.java)
        }
    }

    @Binds
    abstract fun bindSetDehydratedDeviceTask(factory: DefaultSetDehydratedDeviceTaskFactory): SetDehydratedDeviceTask

    @Binds
    abstract fun bindGetDehydratedDeviceTask(factory: DefaultGetDehydratedDeviceTaskFactory): GetDehydratedDeviceTask

    @Binds
    abstract fun bindClaimDehydratedDeviceTask(factory: DefaultClaimDehydratedDeviceTaskFactory): ClaimDehydratedDeviceTask
}
