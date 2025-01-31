/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.matrix.android.sdk.api.session.sync.SyncService
import org.matrix.android.sdk.internal.session.SessionScope
import retrofit2.Retrofit

@Module
internal abstract class SyncModule {

    @Module
    companion object {
        @Provides
        @JvmStatic
        @SessionScope
        fun providesSyncAPI(retrofit: Retrofit): SyncAPI {
            return retrofit.create(SyncAPI::class.java)
        }
    }

    @Binds
    abstract fun bindSyncService(service: DefaultSyncService): SyncService

    @Binds
    abstract fun bindSyncTask(task: DefaultSyncTask): SyncTask

    @Binds
    abstract fun bindRoomSyncEphemeralTemporaryStore(store: RoomSyncEphemeralTemporaryStoreFile): RoomSyncEphemeralTemporaryStore
}
