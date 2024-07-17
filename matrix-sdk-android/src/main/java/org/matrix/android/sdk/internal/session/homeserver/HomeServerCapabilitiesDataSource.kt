/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.homeserver

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import io.realm.kotlin.where
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.api.util.toOptional
import org.matrix.android.sdk.internal.database.mapper.HomeServerCapabilitiesMapper
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import javax.inject.Inject

internal class HomeServerCapabilitiesDataSource @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) {
    fun getHomeServerCapabilities(): HomeServerCapabilities? {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.let {
                HomeServerCapabilitiesMapper.map(it)
            }
        }
    }

    fun getHomeServerCapabilitiesLive(): LiveData<Optional<HomeServerCapabilities>> {
        val liveData = monarchy.findAllMappedWithChanges(
                { realm: Realm -> realm.where<HomeServerCapabilitiesEntity>() },
                { HomeServerCapabilitiesMapper.map(it) }
        )
        return liveData.map {
            it.firstOrNull().toOptional()
        }
    }
}
