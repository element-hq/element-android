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
import im.vector.matrix.android.api.pushrules.PushRuleService
import im.vector.matrix.android.api.session.cache.CacheService
import im.vector.matrix.android.api.session.group.GroupService
import im.vector.matrix.android.api.session.pushers.PushersService
import im.vector.matrix.android.api.session.room.RoomDirectoryService
import im.vector.matrix.android.api.session.room.RoomService
import im.vector.matrix.android.api.session.signout.SignOutService
import im.vector.matrix.android.api.session.sync.FilterService
import im.vector.matrix.android.api.session.user.UserService
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.database.model.SessionRealmModule
import im.vector.matrix.android.internal.session.cache.ClearCacheTask
import im.vector.matrix.android.internal.session.cache.RealmCacheService
import im.vector.matrix.android.internal.session.cache.RealmClearCacheTask
import im.vector.matrix.android.internal.session.filter.*
import im.vector.matrix.android.internal.session.group.DefaultGroupService
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.notification.BingRuleWatcher
import im.vector.matrix.android.internal.session.notification.DefaultProcessEventForPushTask
import im.vector.matrix.android.internal.session.notification.DefaultPushRuleService
import im.vector.matrix.android.internal.session.notification.ProcessEventForPushTask
import im.vector.matrix.android.internal.session.pushers.*
import im.vector.matrix.android.internal.session.room.*
import im.vector.matrix.android.internal.session.room.directory.DefaultGetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.DefaultGetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.directory.GetPublicRoomTask
import im.vector.matrix.android.internal.session.room.directory.GetThirdPartyProtocolsTask
import im.vector.matrix.android.internal.session.room.membership.RoomDisplayNameResolver
import im.vector.matrix.android.internal.session.room.membership.RoomMemberDisplayNameResolver
import im.vector.matrix.android.internal.session.room.prune.EventsPruner
import im.vector.matrix.android.internal.session.signout.DefaultSignOutService
import im.vector.matrix.android.internal.session.user.DefaultUserService
import im.vector.matrix.android.internal.session.user.UserEntityUpdater
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

        scope(DefaultSession.SCOPE, name = "SessionRealmConfiguration") {
            val context = get<Context>()
            val childPath = sessionParams.credentials.userId.md5()
            val directory = File(context.filesDir, childPath)

            RealmConfiguration.Builder()
                    .directory(directory)
                    .name("disk_store.realm")
                    .modules(SessionRealmModule())
                    .deleteRealmIfMigrationNeeded()
                    .build()
        }

        scope(DefaultSession.SCOPE) {
            Monarchy.Builder()
                    .setRealmConfiguration(get("SessionRealmConfiguration"))
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
            RoomDisplayNameResolver(get(), get(), get(), sessionParams.credentials)
        }

        scope(DefaultSession.SCOPE) {
            RoomAvatarResolver(get(), get())
        }

        scope(DefaultSession.SCOPE) {
            RoomSummaryUpdater(get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultRoomService(get(), get(), get(), get()) as RoomService
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetPublicRoomTask(get()) as GetPublicRoomTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetThirdPartyProtocolsTask(get()) as GetThirdPartyProtocolsTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultRoomDirectoryService(get(), get(), get(), get()) as RoomDirectoryService
        }

        scope(DefaultSession.SCOPE) {
            DefaultGroupService(get()) as GroupService
        }

        scope(DefaultSession.SCOPE) {
            DefaultSignOutService(get(), get()) as SignOutService
        }

        scope(DefaultSession.SCOPE) {
            RealmCacheService(get("ClearTaskMainCache"), get()) as CacheService
        }

        // Give a name, because we have a clear task for crypto store as well
        scope(DefaultSession.SCOPE, name = "ClearTaskMainCache") {
            RealmClearCacheTask(get("SessionRealmConfiguration")) as ClearCacheTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultUserService(get()) as UserService
        }

        scope(DefaultSession.SCOPE) {
            SessionListeners()
        }

        scope(DefaultSession.SCOPE) {
            DefaultFilterRepository(get("SessionRealmConfiguration")) as FilterRepository
        }

        scope(DefaultSession.SCOPE) {
            DefaultSaveFilterTask(get(), get(), get()) as SaveFilterTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultFilterService(get(), get(), get()) as FilterService
        }

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(FilterApi::class.java)
        }

        scope(DefaultSession.SCOPE) {
            val retrofit: Retrofit = get()
            retrofit.create(PushRulesApi::class.java)
        }

        scope(DefaultSession.SCOPE) {
            get<DefaultPushRuleService>() as PushRuleService
        }

        scope(DefaultSession.SCOPE) {
            DefaultPushRuleService(get(), get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetPushRulesTask(get()) as GetPushRulesTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultUpdatePushRuleEnableStatusTask(get()) as UpdatePushRuleEnableStatusTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultProcessEventForPushTask(get()) as ProcessEventForPushTask
        }

        scope(DefaultSession.SCOPE) {
            BingRuleWatcher(get(), get(), get(), get(), get())
        }

        scope(DefaultSession.SCOPE) {
            val groupSummaryUpdater = GroupSummaryUpdater(get())
            val userEntityUpdater = UserEntityUpdater(get(), get(), get())
            val aggregationUpdater = EventRelationsAggregationUpdater(get(), get(), get(), get())
            //Event pruner must be the last one, because it will clear contents
            val eventsPruner = EventsPruner(get(), get(), get(), get())
            listOf<LiveEntityObserver>(groupSummaryUpdater, userEntityUpdater, aggregationUpdater, eventsPruner)
        }

        scope(DefaultSession.SCOPE) {
            get<Retrofit>().create(PushersAPI::class.java)
        }

        scope(DefaultSession.SCOPE) {
            DefaultGetPusherTask(get()) as GetPushersTask
        }
        scope(DefaultSession.SCOPE) {
            DefaultRemovePusherTask(get(), get()) as RemovePusherTask
        }

        scope(DefaultSession.SCOPE) {
            DefaultPusherService(get(), get(), get(), get(), get()) as PushersService
        }

    }


}
