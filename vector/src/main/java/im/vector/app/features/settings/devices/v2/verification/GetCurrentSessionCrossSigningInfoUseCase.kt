/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.verification

import im.vector.app.core.di.ActiveSessionHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.crosssigning.MXCrossSigningInfo
import org.matrix.android.sdk.api.util.Optional
import org.matrix.android.sdk.flow.flow
import javax.inject.Inject

class GetCurrentSessionCrossSigningInfoUseCase @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
) {

    fun execute(): Flow<CurrentSessionCrossSigningInfo> {
        return activeSessionHolder.getSafeActiveSession()
                ?.let { session ->
                    session.flow().liveCrossSigningInfo(session.myUserId)
                            .map { convertToSigningInfo(session.sessionParams.deviceId.orEmpty(), it) }
                } ?: emptyFlow()
    }

    private fun convertToSigningInfo(deviceId: String, mxCrossSigningInfo: Optional<MXCrossSigningInfo>): CurrentSessionCrossSigningInfo {
        return CurrentSessionCrossSigningInfo(
                deviceId = deviceId,
                isCrossSigningInitialized = mxCrossSigningInfo.getOrNull() != null,
                isCrossSigningVerified = mxCrossSigningInfo.getOrNull()?.isTrusted().orFalse()
        )
    }
}
