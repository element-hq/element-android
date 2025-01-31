/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.homeserver

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.wellknown.WellknownModule
import retrofit2.Retrofit

@Module(includes = [WellknownModule::class])
internal abstract class HomeServerCapabilitiesModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesCapabilitiesAPI(retrofit: Retrofit): CapabilitiesAPI {
            return retrofit.create(CapabilitiesAPI::class.java)
        }
    }

    @Binds
    abstract fun bindGetHomeServerCapabilitiesTask(task: DefaultGetHomeServerCapabilitiesTask): GetHomeServerCapabilitiesTask
}
