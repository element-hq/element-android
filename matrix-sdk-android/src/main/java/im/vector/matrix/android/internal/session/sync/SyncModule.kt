/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.matrix.android.internal.session.sync

import im.vector.matrix.android.internal.session.DefaultSession
import im.vector.matrix.android.internal.session.sync.job.SyncThread
import org.koin.dsl.module.module
import retrofit2.Retrofit


internal class SyncModule {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(SyncAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            ReadReceiptHandler()
        }

        scope(DefaultSession.SCOPE) {
            RoomTagHandler()
        }

        scope(DefaultSession.SCOPE) {
            RoomSyncHandler(get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            GroupSyncHandler(get())
        }

        scope(DefaultSession.SCOPE) {
            UserAccountDataSyncHandler(get())
        }

        scope(DefaultSession.SCOPE) {
            SyncResponseHandler(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultSyncTask(get(), get(), get()) as SyncTask
        }

        scope(DefaultSession.SCOPE) {
            SyncTokenStore(get())
        }

        scope(DefaultSession.SCOPE) {
            SyncThread(get(), get(), get(), get(), get())
        }

    }
}
