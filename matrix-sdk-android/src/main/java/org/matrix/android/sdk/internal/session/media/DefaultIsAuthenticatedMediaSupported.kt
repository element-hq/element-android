/*
 * Copyright (C) 2024 The Matrix.org Foundation C.I.C.
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
import io.realm.RealmResults
import org.matrix.android.sdk.internal.database.RealmLiveEntityObserver
import org.matrix.android.sdk.internal.database.model.HomeServerCapabilitiesEntity
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.SessionScope
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultIsAuthenticatedMediaSupported @Inject constructor(
        @SessionDatabase private val monarchy: Monarchy,
) :
        IsAuthenticatedMediaSupported,
        RealmLiveEntityObserver<HomeServerCapabilitiesEntity>(monarchy.realmConfiguration) {

    override fun invoke(): Boolean {
        return canUseAuthenticatedMedia
    }

    override val query = Monarchy.Query {
        it.where(HomeServerCapabilitiesEntity::class.java)
    }

    override fun onChange(results: RealmResults<HomeServerCapabilitiesEntity>) {
        canUseAuthenticatedMedia = results.canUseAuthenticatedMedia()
        Timber.d("canUseAuthenticatedMedia: $canUseAuthenticatedMedia")
    }

    private var canUseAuthenticatedMedia = getInitialValue()

    private fun getInitialValue(): Boolean {
        return Realm.getInstance(monarchy.realmConfiguration).use { realm ->
            query.createQuery(realm).findAll().canUseAuthenticatedMedia()
        }
    }

    private fun RealmResults<HomeServerCapabilitiesEntity>.canUseAuthenticatedMedia(): Boolean {
        return firstOrNull()?.canUseAuthenticatedMedia ?: false
    }
}
