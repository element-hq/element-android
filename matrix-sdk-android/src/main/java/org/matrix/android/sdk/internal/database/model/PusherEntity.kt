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
package org.matrix.android.sdk.internal.database.model

import io.realm.RealmObject
import org.matrix.android.sdk.api.session.pushers.PusherState

// TODO
//        at java.lang.Thread.run(Thread.java:764)
//     Caused by: java.lang.IllegalArgumentException: 'value' is not a valid managed object.
//        at io.realm.ProxyState.checkValidObject(ProxyState.java:213)
//        at io.realm.im_vector_matrix_android_internal_database_model_PusherEntityRealmProxy
//            .realmSet$data(im_vector_matrix_android_internal_database_model_PusherEntityRealmProxy.java:413)
//        at org.matrix.android.sdk.internal.database.model.PusherEntity.setData(PusherEntity.kt:16)
//        at org.matrix.android.sdk.internal.session.pushers.AddHttpPusherWorker$doWork$$inlined$fold$lambda$2.execute(AddHttpPusherWorker.kt:70)
//        at io.realm.Realm.executeTransaction(Realm.java:1493)
internal open class PusherEntity(
        var pushKey: String = "",
        var kind: String? = null,
        var appId: String = "",
        var appDisplayName: String? = null,
        var deviceDisplayName: String? = null,
        var profileTag: String? = null,
        var lang: String? = null,
        var data: PusherDataEntity? = null
) : RealmObject() {
    private var stateStr: String = PusherState.UNREGISTERED.name

    var state: PusherState
        get() {
            try {
                return PusherState.valueOf(stateStr)
            } catch (e: Exception) {
                // can this happen?
                return PusherState.UNREGISTERED
            }
        }
        set(value) {
            stateStr = value.name
        }

    companion object
}

internal fun PusherEntity.deleteOnCascade() {
    data?.deleteFromRealm()
    deleteFromRealm()
}
