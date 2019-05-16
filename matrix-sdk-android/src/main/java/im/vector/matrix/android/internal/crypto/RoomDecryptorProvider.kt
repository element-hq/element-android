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

package im.vector.matrix.android.internal.crypto

import android.text.TextUtils
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.algorithms.IMXDecrypting
import im.vector.matrix.android.internal.crypto.tasks.SendToDeviceTask
import im.vector.matrix.android.internal.task.TaskExecutor
import timber.log.Timber
import java.util.*

internal class RoomDecryptorProvider(
        val mCredentials: Credentials,
        val olmDevice: MXOlmDevice,
        val deviceListManager: DeviceListManager,
        val mSendToDeviceTask: SendToDeviceTask,
        val mTaskExecutor: TaskExecutor
) {

    // A map from algorithm to MXDecrypting instance, for each room
    private val mRoomDecryptors: MutableMap<String, MutableMap<String /* algorithm */, IMXDecrypting>>/* room id */ = HashMap()

    /**
     * Get a decryptor for a given room and algorithm.
     * If we already have a decryptor for the given room and algorithm, return
     * it. Otherwise try to instantiate it.
     *
     * @param roomId    the room id
     * @param algorithm the crypto algorithm
     * @return the decryptor
     * TODO do not provide cryptoManager?
     */
    fun getOrCreateRoomDecryptor(cryptoManager: CryptoManager, roomId: String?, algorithm: String?): IMXDecrypting? {
        // sanity check
        if (TextUtils.isEmpty(algorithm)) {
            Timber.e("## getRoomDecryptor() : null algorithm")
            return null
        }

        if (null == mRoomDecryptors) {
            Timber.e("## getRoomDecryptor() : null mRoomDecryptors")
            return null
        }

        var alg: IMXDecrypting? = null

        if (!TextUtils.isEmpty(roomId)) {
            synchronized(mRoomDecryptors) {
                if (!mRoomDecryptors.containsKey(roomId)) {
                    mRoomDecryptors[roomId!!] = HashMap()
                }

                alg = mRoomDecryptors[roomId]!![algorithm]
            }

            if (null != alg) {
                return alg
            }
        }

        val decryptingClass = MXCryptoAlgorithms.decryptorClassForAlgorithm(algorithm)

        if (null != decryptingClass) {
            try {
                val ctor = decryptingClass.constructors[0]
                alg = ctor.newInstance() as IMXDecrypting

                if (null != alg) {
                    alg!!.initWithMatrixSession(mCredentials, cryptoManager, olmDevice, deviceListManager, mSendToDeviceTask, mTaskExecutor)

                    if (!TextUtils.isEmpty(roomId)) {
                        synchronized(mRoomDecryptors) {
                            mRoomDecryptors[roomId]!!.put(algorithm!!, alg!!)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "## getRoomDecryptor() : fail to load the class")
                return null
            }

        }

        return alg
    }

    fun getRoomDecryptor(roomId: String?, algorithm: String?): IMXDecrypting? {
        if (roomId == null || algorithm == null) {
            return null
        }

        return mRoomDecryptors[roomId]?.get(algorithm)
    }
}