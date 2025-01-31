/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.cache

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.realm.RealmConfiguration
import org.matrix.android.sdk.api.session.cache.CacheService
import org.matrix.android.sdk.internal.di.SessionDatabase

@Module
internal abstract class CacheModule {

    @Module
    companion object {
        @JvmStatic
        @Provides
        @SessionDatabase
        fun providesClearCacheTask(@SessionDatabase realmConfiguration: RealmConfiguration): ClearCacheTask {
            return RealmClearCacheTask(realmConfiguration)
        }
    }

    @Binds
    abstract fun bindCacheService(service: DefaultCacheService): CacheService
}
