/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.profile

import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.internal.database.model.PendingThreePidEntity
import javax.inject.Inject

internal class PendingThreePidMapper @Inject constructor() {

    fun map(entity: PendingThreePidEntity): PendingThreePid {
        return PendingThreePid(
                threePid = entity.email?.let { ThreePid.Email(it) }
                        ?: entity.msisdn?.let { ThreePid.Msisdn(it) }
                        ?: error("Invalid data"),
                clientSecret = entity.clientSecret,
                sendAttempt = entity.sendAttempt,
                sid = entity.sid,
                submitUrl = entity.submitUrl
        )
    }

    fun map(domain: PendingThreePid): PendingThreePidEntity {
        return PendingThreePidEntity(
                email = domain.threePid.takeIf { it is ThreePid.Email }?.value,
                msisdn = domain.threePid.takeIf { it is ThreePid.Msisdn }?.value,
                clientSecret = domain.clientSecret,
                sendAttempt = domain.sendAttempt,
                sid = domain.sid,
                submitUrl = domain.submitUrl
        )
    }
}
