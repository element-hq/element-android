/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.homeserver

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilities
import im.vector.matrix.android.api.session.homeserver.HomeServerCapabilitiesService
import im.vector.matrix.android.internal.database.mapper.HomeServerCapabilitiesMapper
import im.vector.matrix.android.internal.database.model.HomeServerCapabilitiesEntity
import im.vector.matrix.android.internal.database.query.get
import im.vector.matrix.android.internal.di.SessionDatabase
import io.realm.Realm
import javax.inject.Inject

internal class DefaultHomeServerCapabilitiesService @Inject constructor(@SessionDatabase private val monarchy: Monarchy) : HomeServerCapabilitiesService {

    override fun getHomeServerCapabilities(): HomeServerCapabilities {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.let {
                HomeServerCapabilitiesMapper.map(it)
            }
        }
                ?: HomeServerCapabilities()
    }
}
