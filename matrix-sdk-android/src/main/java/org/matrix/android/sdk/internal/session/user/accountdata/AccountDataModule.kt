/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.user.accountdata

import dagger.Binds
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
internal abstract class AccountDataModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        fun providesAccountDataAPI(retrofit: Retrofit): AccountDataAPI {
            return retrofit.create(AccountDataAPI::class.java)
        }
    }

    @Binds
    abstract fun bindUpdateUserAccountDataTask(task: DefaultUpdateUserAccountDataTask): UpdateUserAccountDataTask

    @Binds
    abstract fun bindSaveBreadcrumbsTask(task: DefaultSaveBreadcrumbsTask): SaveBreadcrumbsTask

    @Binds
    abstract fun bindUpdateBreadcrumbsTask(task: DefaultUpdateBreadcrumbsTask): UpdateBreadcrumbsTask
}
