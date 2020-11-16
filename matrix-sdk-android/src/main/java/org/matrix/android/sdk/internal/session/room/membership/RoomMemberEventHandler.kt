/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.membership

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.kotlin.where
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.extensions.orTrue
import org.matrix.android.sdk.api.util.JsonDict
import org.matrix.android.sdk.internal.database.model.UserEntity
import org.matrix.android.sdk.internal.database.model.UserEntityFields
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber
import javax.inject.Inject

internal class RoomMemberEventHandler @Inject constructor(
        @UserId
        private val userId: String,
        @SessionDatabase
        private val realmConfiguration: RealmConfiguration,
        private val taskExecutor: TaskExecutor,
        private val getProfileInfoTask: GetProfileInfoTask
) {

    fun handle(realm: Realm, roomId: String, event: Event): Boolean {
        if (event.type != EventType.STATE_ROOM_MEMBER) {
            return false
        }
        val userId = event.stateKey ?: return false
        val roomMember = event.content.toModel<RoomMemberContent>()
        return handle(realm, roomId, userId, roomMember)
    }

    fun handle(realm: Realm, roomId: String, userId: String, roomMember: RoomMemberContent?): Boolean {
        if (roomMember == null) {
            return false
        }
        val roomMemberEntity = RoomMemberEntityFactory.create(roomId, userId, roomMember)
        realm.insertOrUpdate(roomMemberEntity)

        if (roomMember.membership.isActive() && shouldUpdateUserEntity(realm, userId, roomMember)) {
            updateUserEntity(realm, userId, roomMember)
            if (userId == this.userId) {
                fetchUserProfile(userId) // To fix #1715 (myroomnick changes the global name)
            }
        }
        return true
    }

    private fun shouldUpdateUserEntity(realm: Realm, userId: String, roomMember: RoomMemberContent?): Boolean {
        return realm
                .where<UserEntity>()
                .equalTo(UserEntityFields.USER_ID, userId)
                .findFirst()
                ?.let {
                    it.displayName != roomMember?.displayName
                            || it.avatarUrl != roomMember.avatarUrl
                }
                .orTrue()
    }

    private fun updateUserEntity(realm: Realm, userId: String, roomMember: RoomMemberContent) {
        val userEntity = UserEntityFactory.create(userId, roomMember)
        realm.insertOrUpdate(userEntity)
    }

    private fun fetchUserProfile(userId: String) {
        val params = GetProfileInfoTask.Params(userId)
        getProfileInfoTask
                .configureWith(params) {
                    this.callback = object : MatrixCallback<JsonDict> {
                        override fun onSuccess(data: JsonDict) {
                            val displayName = data["displayname"] as? String
                            val avatarUrl = data["avatar_url"] as? String
                            val userEntity = UserEntity(userId, displayName ?: "", avatarUrl ?: "")
                            Realm.getInstance(realmConfiguration).use { realm ->
                                realm.executeTransaction {
                                    it.insertOrUpdate(userEntity)
                                }
                            }
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "Couldn't fetch user profile")
                        }
                    }
                }
                .executeBy(taskExecutor)
    }
}
