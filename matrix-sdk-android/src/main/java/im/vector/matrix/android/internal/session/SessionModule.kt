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
import dagger.Module
import dagger.Provides
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.auth.data.SessionParams
import im.vector.matrix.android.internal.database.LiveEntityObserver
import im.vector.matrix.android.internal.database.model.SessionRealmModule
import im.vector.matrix.android.internal.session.filter.FilterApi
import im.vector.matrix.android.internal.session.group.GroupSummaryUpdater
import im.vector.matrix.android.internal.session.room.EventRelationsAggregationUpdater
import im.vector.matrix.android.internal.session.room.prune.EventsPruner
import im.vector.matrix.android.internal.session.user.UserEntityUpdater
import im.vector.matrix.android.internal.util.md5
import io.realm.RealmConfiguration
import retrofit2.Retrofit
import java.io.File
import javax.inject.Named

@Module
internal object SessionModule {

    @Provides
    @SessionScope
    fun providesCredentials(sessionParams: SessionParams): Credentials {
        return sessionParams.credentials
    }

    @Provides
    @SessionScope
    @Named("SessionRealmConfiguration")
    fun providesRealmConfiguration(sessionParams: SessionParams, context: Context): RealmConfiguration {
        val childPath = sessionParams.credentials.userId.md5()
        val directory = File(context.filesDir, childPath)

        return RealmConfiguration.Builder()
                .directory(directory)
                .name("disk_store.realm")
                .modules(SessionRealmModule())
                .deleteRealmIfMigrationNeeded()
                .build()
    }

    @Provides
    @SessionScope
    fun providesMonarchy(@Named("SessionRealmConfiguration")
                         realmConfiguration: RealmConfiguration): Monarchy {
        return Monarchy.Builder()
                .setRealmConfiguration(realmConfiguration)
                .build()
    }

    @Provides
    @SessionScope
    fun providesRetrofit(sessionParams: SessionParams, retrofitBuilder: Retrofit.Builder): Retrofit {
        return retrofitBuilder
                .baseUrl(sessionParams.homeServerConnectionConfig.homeServerUri.toString())
                .build()
    }

    @Provides
    @SessionScope
    fun providesFilterAPI(retrofit: Retrofit): FilterApi {
        return retrofit.create(FilterApi::class.java)
    }

    @Provides
    @SessionScope
    fun providesLiveEntityObservers(groupSummaryUpdater: GroupSummaryUpdater,
                                    userEntityUpdater: UserEntityUpdater,
                                    aggregationUpdater: EventRelationsAggregationUpdater,
                                    eventsPruner: EventsPruner): List<LiveEntityObserver> {
        return listOf<LiveEntityObserver>(groupSummaryUpdater, userEntityUpdater, aggregationUpdater, eventsPruner)
    }

}
