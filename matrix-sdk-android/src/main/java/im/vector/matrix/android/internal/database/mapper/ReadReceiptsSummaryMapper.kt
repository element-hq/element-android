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

package im.vector.matrix.android.internal.database.mapper

import im.vector.matrix.android.api.session.room.model.ReadReceipt
import im.vector.matrix.android.internal.database.model.ReadReceiptsSummaryEntity
import im.vector.matrix.android.internal.database.model.UserEntity
import im.vector.matrix.android.internal.database.query.where
import im.vector.matrix.android.internal.di.SessionDatabase
import io.realm.Realm
import io.realm.RealmConfiguration
import javax.inject.Inject

internal class ReadReceiptsSummaryMapper @Inject constructor(@SessionDatabase private val realmConfiguration: RealmConfiguration){

    fun map(readReceiptsSummaryEntity: ReadReceiptsSummaryEntity): List<ReadReceipt> {
        return Realm.getInstance(realmConfiguration).use { realm ->
            readReceiptsSummaryEntity.readReceipts.mapNotNull {
                val user = UserEntity.where(realm, it.userId).findFirst()
                           ?: return@mapNotNull null
                ReadReceipt(user.asDomain(), it.originServerTs.toLong())
            }
        }
    }

}
