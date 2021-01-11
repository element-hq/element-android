/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.call.webrtc

import kotlinx.coroutines.delay
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol

private const val PSTN_VECTOR_KEY = "im.vector.protocol.pstn"
private const val PSTN_MATRIX_KEY = "m.protocol.pstn"

suspend fun Session.getSupportedPSTN(maxTries: Int): String? {
    val thirdPartyProtocols: Map<String, ThirdPartyProtocol> = try {
        getThirdPartyProtocols()
    } catch (failure: Throwable) {
        if (maxTries == 1) {
            return null
        } else {
            // Wait for 10s before trying again
            delay(10_000L)
            return getSupportedPSTN(maxTries - 1)
        }
    }
    return when {
        thirdPartyProtocols.containsKey(PSTN_VECTOR_KEY) -> PSTN_VECTOR_KEY
        thirdPartyProtocols.containsKey(PSTN_MATRIX_KEY) -> PSTN_MATRIX_KEY
        else                                             -> null
    }
}

