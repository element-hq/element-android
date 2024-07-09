/*
 * Copyright (c) 2024 New Vector Ltd
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

package org.matrix.android.sdk.internal.session.media

import com.zhuinden.monarchy.Monarchy
import io.realm.Realm
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.database.query.get
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
class DefaultIsAuthenticatedMediaSupported @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) : IsAuthenticatedMediaSupported {

    private val canUseAuthenticatedMedia: Boolean by lazy {
        canUseAuthenticatedMedia()
    }

    override fun invoke(): Boolean {
        return canUseAuthenticatedMedia
    }

    private fun canUseAuthenticatedMedia(): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            HomeServerCapabilitiesEntity.get(realm)?.canUseAuthenticatedMedia ?: false
        }
    }
}
