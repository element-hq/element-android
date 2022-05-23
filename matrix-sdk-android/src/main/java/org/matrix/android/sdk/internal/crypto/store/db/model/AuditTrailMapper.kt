/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.crypto.store.db.model

import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.crypto.model.AuditTrail
import org.matrix.android.sdk.api.session.crypto.model.ForwardInfo
import org.matrix.android.sdk.api.session.crypto.model.IncomingKeyRequestInfo
import org.matrix.android.sdk.api.session.crypto.model.TrailType
import org.matrix.android.sdk.api.session.crypto.model.UnknownInfo
import org.matrix.android.sdk.api.session.crypto.model.WithheldInfo
import org.matrix.android.sdk.internal.di.MoshiProvider

internal object AuditTrailMapper {

    fun map(entity: AuditTrailEntity): AuditTrail? {
        val contentJson = entity.contentJson ?: return null
        return when (entity.type) {
            TrailType.OutgoingKeyForward.name -> {
                val info = tryOrNull {
                    MoshiProvider.providesMoshi().adapter(ForwardInfo::class.java).fromJson(contentJson)
                } ?: return null
                AuditTrail(
                        ageLocalTs = entity.ageLocalTs ?: 0,
                        type = TrailType.OutgoingKeyForward,
                        info = info
                )
            }
            TrailType.OutgoingKeyWithheld.name -> {
                val info = tryOrNull {
                    MoshiProvider.providesMoshi().adapter(WithheldInfo::class.java).fromJson(contentJson)
                } ?: return null
                AuditTrail(
                        ageLocalTs = entity.ageLocalTs ?: 0,
                        type = TrailType.OutgoingKeyWithheld,
                        info = info
                )
            }
            TrailType.IncomingKeyRequest.name -> {
                val info = tryOrNull {
                    MoshiProvider.providesMoshi().adapter(IncomingKeyRequestInfo::class.java).fromJson(contentJson)
                } ?: return null
                AuditTrail(
                        ageLocalTs = entity.ageLocalTs ?: 0,
                        type = TrailType.IncomingKeyRequest,
                        info = info
                )
            }
            TrailType.IncomingKeyForward.name -> {
                val info = tryOrNull {
                    MoshiProvider.providesMoshi().adapter(ForwardInfo::class.java).fromJson(contentJson)
                } ?: return null
                AuditTrail(
                        ageLocalTs = entity.ageLocalTs ?: 0,
                        type = TrailType.IncomingKeyForward,
                        info = info
                )
            }
            else -> {
                AuditTrail(
                        ageLocalTs = entity.ageLocalTs ?: 0,
                        type = TrailType.Unknown,
                        info = UnknownInfo
                )
            }
        }
    }
}
