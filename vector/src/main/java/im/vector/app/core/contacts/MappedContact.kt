/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.contacts

import android.net.Uri

class MappedContactBuilder(
        val id: Long,
        val displayName: String
) {
    var photoURI: Uri? = null
    val msisdns = mutableListOf<MappedMsisdn>()
    val emails = mutableListOf<MappedEmail>()

    fun build(): MappedContact {
        return MappedContact(
                id = id,
                displayName = displayName,
                photoURI = photoURI,
                msisdns = msisdns,
                emails = emails
        )
    }
}

data class MappedContact(
        val id: Long,
        val displayName: String,
        val photoURI: Uri? = null,
        val msisdns: List<MappedMsisdn> = emptyList(),
        val emails: List<MappedEmail> = emptyList()
)

data class MappedEmail(
        val email: String,
        val matrixId: String?
)

data class MappedMsisdn(
        val phoneNumber: String,
        val matrixId: String?
)
