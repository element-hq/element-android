/*
 * Copyright 2020 New Vector Ltd
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
 *
 */

package im.vector.matrix.android.internal.auth.sqlite

import com.squareup.sqldelight.db.SqlDriver
import im.vector.matrix.sqldelight.auth.AuthDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class AuthSchema @Inject constructor(@im.vector.matrix.android.internal.di.RealmAuthDatabase private val realmConfiguration: RealmConfiguration) : SqlDriver.Schema by AuthDatabase.Schema {

    override fun create(driver: SqlDriver) {
        AuthDatabase.Schema.create(driver)
        AuthDatabase(driver).apply {
            val sessionParamsQueries = this.sessionParamsQueries
            sessionParamsQueries.transaction {
                Realm.getInstance(realmConfiguration).use {
                    it.where(im.vector.matrix.android.internal.auth.realm.SessionParamsEntity::class.java).findAll().forEach { realmSessionParams ->
                        sessionParamsQueries.insert(realmSessionParams.toSqlModel())
                    }
                }
            }
        }
    }

    private fun im.vector.matrix.android.internal.auth.realm.SessionParamsEntity.toSqlModel(): im.vector.matrix.sqldelight.auth.SessionParamsEntity {
        return im.vector.matrix.sqldelight.auth.SessionParamsEntity.Impl(
                sessionId,
                userId,
                credentialsJson,
                homeServerConnectionConfigJson,
                isTokenValid
        )
    }

}
