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

import androidx.lifecycle.LiveData
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.util.Optional
import im.vector.matrix.android.internal.crypto.crosssigning.DeviceTrustResult
import im.vector.matrix.android.internal.crypto.crosssigning.UserTrustResult
import im.vector.matrix.android.internal.crypto.model.rest.UserPasswordAuth

interface CrossSigningService {

    fun isCrossSigningVerified(): Boolean

    fun isUserTrusted(otherUserId: String): Boolean

    /**
     * Will not force a download of the key, but will verify signatures trust chain.
     * Checks that my trusted user key has signed the other user UserKey
     */
    fun checkUserTrust(otherUserId: String): UserTrustResult

    /**
     * Initialize cross signing for this user.
     * Users needs to enter credentials
     */
    fun initializeCrossSigning(authParams: UserPasswordAuth?,
                               callback: MatrixCallback<Unit>? = null)

    fun getUserCrossSigningKeys(otherUserId: String): MXCrossSigningInfo?

    fun getLiveCrossSigningKeys(userId: String): LiveData<Optional<MXCrossSigningInfo>>

    fun getMyCrossSigningKeys(): MXCrossSigningInfo?

    fun canCrossSign(): Boolean

    fun trustUser(otherUserId: String,
                  callback: MatrixCallback<Unit>)

    fun markMyMasterKeyAsTrusted()

    /**
     * Sign one of your devices and upload the signature
     */
    fun signDevice(deviceId: String,
                   callback: MatrixCallback<Unit>)

    fun checkDeviceTrust(otherUserId: String,
                         otherDeviceId: String,
                         locallyTrusted: Boolean?): DeviceTrustResult
}
