/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.terms

import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.session.terms.TermsService
import im.vector.matrix.android.internal.di.UnauthenticatedWithCertificate
import im.vector.matrix.android.internal.network.RetrofitFactory
import im.vector.matrix.android.internal.session.SessionScope
import okhttp3.OkHttpClient

@Module
internal abstract class TermsModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesTermsAPI(@UnauthenticatedWithCertificate unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
                             retrofitFactory: RetrofitFactory): TermsAPI {
            val retrofit = retrofitFactory.create(unauthenticatedOkHttpClient, "https://foo.bar")
            return retrofit.create(TermsAPI::class.java)
        }
    }

    @Binds
    abstract fun bindTermsService(service: DefaultTermsService): TermsService
}
