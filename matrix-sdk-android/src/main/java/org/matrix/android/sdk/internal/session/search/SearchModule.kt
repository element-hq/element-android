/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.search

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.search.SearchService
import org.matrix.android.sdk.internal.session.SessionScope
import retrofit2.Retrofit

@Module
internal abstract class SearchModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesSearchAPI(retrofit: Retrofit): SearchAPI {
            return retrofit.create(SearchAPI::class.java)
        }
    }

    @Binds
    abstract fun bindSearchService(service: DefaultSearchService): SearchService

    @Binds
    abstract fun bindSearchTask(task: DefaultSearchTask): SearchTask
}
