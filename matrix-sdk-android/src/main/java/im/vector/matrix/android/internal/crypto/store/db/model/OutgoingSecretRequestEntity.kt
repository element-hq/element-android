// /*
// * Copyright (c) 2020 New Vector Ltd
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package im.vector.matrix.android.internal.crypto.store.db.model
//
// import im.vector.matrix.android.internal.crypto.OutgoingSecretRequest
// import im.vector.matrix.android.internal.crypto.ShareRequestState
// import im.vector.matrix.android.internal.crypto.store.db.deserializeFromRealm
// import im.vector.matrix.android.internal.crypto.store.db.serializeForRealm
// import io.realm.RealmObject
// import io.realm.annotations.PrimaryKey
//
// internal open class OutgoingSecretRequestEntity(
//        @PrimaryKey var requestId: String? = null,
//        var cancellationTxnId: String? = null,
//        // Serialized Json
//        var recipientsData: String? = null,
//        // RoomKeyRequestBody fields
//        var secretName: String? = null,
//        // State
//        var state: Int = 0
// ) : RealmObject() {
//
//    /**
//     * Convert to OutgoingRoomKeyRequest
//     */
//    fun toOutgoingSecretRequest(): OutgoingSecretRequest {
//        val cancellationTxnId = this.cancellationTxnId
//        return OutgoingSecretRequest(
//                secretName,
//                getRecipients() ?: emptyList(),
//                requestId!!,
//                ShareRequestState.from(state)
//        ).apply {
//            this.cancellationTxnId = cancellationTxnId
//        }
//    }
//
//    private fun getRecipients(): List<Map<String, String>>? {
//        return try {
//            deserializeFromRealm(recipientsData)
//        } catch (failure: Throwable) {
//            null
//        }
//    }
//
//    fun putRecipients(recipients: List<Map<String, String>>?) {
//        recipientsData = serializeForRealm(recipients)
//    }
// }
