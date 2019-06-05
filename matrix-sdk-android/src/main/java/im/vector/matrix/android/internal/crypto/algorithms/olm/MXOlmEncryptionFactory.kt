/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.crypto.algorithms.olm

import im.vector.matrix.android.internal.crypto.DeviceListManager
import im.vector.matrix.android.internal.crypto.MXOlmDevice
import im.vector.matrix.android.internal.crypto.actions.EnsureOlmSessionsForUsersAction
import im.vector.matrix.android.internal.crypto.actions.MessageEncrypter
import im.vector.matrix.android.internal.crypto.store.IMXCryptoStore
import im.vector.matrix.android.internal.util.MatrixCoroutineDispatchers

internal class MXOlmEncryptionFactory(private val mOlmDevice: MXOlmDevice,
                                      private val mCryptoStore: IMXCryptoStore,
                                      private val mMessageEncrypter: MessageEncrypter,
                                      private val mDeviceListManager: DeviceListManager,
                                      private val coroutineDispatchers: MatrixCoroutineDispatchers,
                                      private val mEnsureOlmSessionsForUsersAction: EnsureOlmSessionsForUsersAction) {

    fun create(roomId: String): MXOlmEncryption {
        return MXOlmEncryption(
                roomId,
                mOlmDevice,
                mCryptoStore,
                mMessageEncrypter,
                mDeviceListManager,
                mEnsureOlmSessionsForUsersAction)
    }
}