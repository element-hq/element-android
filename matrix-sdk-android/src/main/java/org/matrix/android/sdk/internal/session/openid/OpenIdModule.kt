/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.openid

import dagger.Binds
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
internal abstract class OpenIdModule {

    @Module
    companion object {
        @JvmStatic
        @Provides
        fun providesOpenIdAPI(retrofit: Retrofit): OpenIdAPI {
            return retrofit.create(OpenIdAPI::class.java)
        }
    }

    @Binds
    abstract fun bindGetOpenIdTokenTask(task: DefaultGetOpenIdTokenTask): GetOpenIdTokenTask
}
