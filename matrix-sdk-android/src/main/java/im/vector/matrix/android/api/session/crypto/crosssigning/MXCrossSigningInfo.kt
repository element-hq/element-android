/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.matrix.android.api.session.crypto.crosssigning

import im.vector.matrix.android.internal.crypto.model.rest.CrossSigningKeyInfo

data class MXCrossSigningInfo(

        /**
         * the user id
         */
//        @Json(name = "user_id")
        var userId: String,

//        @Json(name = "user_keys")
        var crossSigningKeys: List<CrossSigningKeyInfo> = ArrayList(),

        val isTrusted: Boolean = false

) {

    fun masterKey(): CrossSigningKeyInfo? = crossSigningKeys
            .firstOrNull { it.usages?.contains(CrossSigningKeyInfo.KeyUsage.MASTER.value) == true }


    fun userKey(): CrossSigningKeyInfo? = crossSigningKeys
            .firstOrNull { it.usages?.contains(CrossSigningKeyInfo.KeyUsage.USER_SIGNING.value) == true }


    fun selfSigningKey(): CrossSigningKeyInfo? = crossSigningKeys
            .firstOrNull { it.usages?.contains(CrossSigningKeyInfo.KeyUsage.SELF_SIGNING.value) == true }

}
