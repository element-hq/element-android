/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.terms

import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import org.matrix.android.sdk.api.session.terms.TermsService
import org.matrix.android.sdk.internal.di.UnauthenticatedWithCertificate
import org.matrix.android.sdk.internal.network.RetrofitFactory
import org.matrix.android.sdk.internal.session.SessionScope

@Module
internal abstract class TermsModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesTermsAPI(
                @UnauthenticatedWithCertificate unauthenticatedOkHttpClient: Lazy<OkHttpClient>,
                retrofitFactory: RetrofitFactory
        ): TermsAPI {
            val retrofit = retrofitFactory.create(unauthenticatedOkHttpClient, "https://foo.bar")
            return retrofit.create(TermsAPI::class.java)
        }
    }

    @Binds
    abstract fun bindTermsService(service: DefaultTermsService): TermsService
}
