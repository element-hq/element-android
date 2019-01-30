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

package im.vector.matrix.android.internal.session

import android.content.Context
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.session.content.DefaultContentUrlResolver
import im.vector.matrix.android.internal.session.group.DefaultGroupService
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.room.DefaultRoomService
import im.vector.matrix.android.internal.session.room.RoomAvatarResolver
import im.vector.matrix.android.internal.session.room.RoomFactory
import im.vector.matrix.android.internal.session.room.RoomSummaryUpdater
import im.vector.matrix.android.internal.session.room.members.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.members.RoomMemberDisplayNameResolver
import im.vector.matrix.android.internal.session.room.prune.EventsPruner
import im.vector.matrix.android.internal.util.md5
import io.realm.RealmConfiguration
import org.koin.dsl.module.module
import retrofit2.Retrofit
import java.io.File

internal class SessionModule(private val sessionParams: SessionParams) {

    val definition = module(override = true) {

        scope(DefaultSession.SCOPE) {
            sessionParams
        }

        scope(DefaultSession.SCOPE) {
            sessionParams.credentials
        }

        scope(DefaultSession.SCOPE) {
            val context = get<Context>()
            val childPath = sessionParams.credentials.userId.md5()
            val directory = File(context.filesDir, childPath)

            RealmConfiguration.Builder()
                    .directory(directory)
                    .name("disk_store.realm")
                    .deleteRealmIfMigrationNeeded()
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            Monarchy.Builder()
                    .setRealmConfiguration(get())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            val retrofitBuilder = get<Retrofit.Builder>()
            retrofitBuilder
                    .baseUrl(sessionParams.homeServerConnectionConfig.homeServerUri.toString())
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            RoomMemberDisplayNameResolver()
        }

        scope(DefaultSession.SCOPE) {
            RoomDisplayNameResolver(get(), get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            RoomAvatarResolver(get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            DefaultRoomService(get(), get()) as RoomService
        }

        scope(DefaultSession.SCOPE) {
            DefaultGroupService(get()) as GroupService
        }

        scope(DefaultSession.SCOPE) {
            SessionListeners()
        }

        scope(DefaultSession.SCOPE) {
            DefaultContentUrlResolver(sessionParams.homeServerConnectionConfig) as ContentUrlResolver
        }

        scope(DefaultSession.SCOPE) {
            val roomSummaryUpdater = RoomSummaryUpdater(get(), get(), get(), get(), sessionParams.credentials)
            val groupSummaryUpdater = GroupSummaryUpdater(get())
            val eventsPruner = EventsPruner(get())
            listOf<LiveEntityObserver>(roomSummaryUpdater, groupSummaryUpdater, eventsPruner)
        }


    }


}
