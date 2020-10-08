/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
