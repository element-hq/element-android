/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.thirdparty

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.thirdparty.ThirdPartyService
import org.matrix.android.sdk.internal.session.SessionScope
import retrofit2.Retrofit

@Module
internal abstract class ThirdPartyModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesThirdPartyAPI(retrofit: Retrofit): ThirdPartyAPI {
            return retrofit.create(ThirdPartyAPI::class.java)
        }
    }

    @Binds
    abstract fun bindThirdPartyService(service: DefaultThirdPartyService): ThirdPartyService

    @Binds
    abstract fun bindGetThirdPartyProtocolsTask(task: DefaultGetThirdPartyProtocolsTask): GetThirdPartyProtocolsTask

    @Binds
    abstract fun bindGetThirdPartyUserTask(task: DefaultGetThirdPartyUserTask): GetThirdPartyUserTask
}
