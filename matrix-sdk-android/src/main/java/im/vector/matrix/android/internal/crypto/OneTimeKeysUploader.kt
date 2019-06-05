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

import arrow.core.Try
import arrow.instances.`try`.applicativeError.handleError
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.model.MXKey
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadResponse
import im.vector.matrix.android.internal.crypto.tasks.UploadKeysTask
import im.vector.matrix.android.internal.di.MoshiProvider
import org.matrix.olm.OlmAccount
import timber.log.Timber
import java.util.*

internal class OneTimeKeysUploader(
        private val credentials: Credentials,
        private val olmDevice: MXOlmDevice,
        private val objectSigner: ObjectSigner,
        private val uploadKeysTask: UploadKeysTask
) {
    // tell if there is a OTK check in progress
    private var oneTimeKeyCheckInProgress = false

    // last OTK check timestamp
    private var lastOneTimeKeyCheck: Long = 0
    private var oneTimeKeyCount: Int? = null

    private var lastPublishedOneTimeKeys: Map<String, Map<String, String>>? = null

    /**
     * Stores the current one_time_key count which will be handled later (in a call of
     * _onSyncCompleted). The count is e.g. coming from a /sync response.
     *
     * @param currentCount the new count
     */
    fun updateOneTimeKeyCount(currentCount: Int) {
        oneTimeKeyCount = currentCount
    }


    /**
     * Check if the OTK must be uploaded.
     */
    suspend fun maybeUploadOneTimeKeys(): Try<Unit> {
        if (oneTimeKeyCheckInProgress) {
            return Try.just(Unit)
        }
        if (System.currentTimeMillis() - lastOneTimeKeyCheck < ONE_TIME_KEY_UPLOAD_PERIOD) {
            // we've done a key upload recently.
            return Try.just(Unit)
        }

        lastOneTimeKeyCheck = System.currentTimeMillis()
        oneTimeKeyCheckInProgress = true

        // We then check how many keys we can store in the Account object.
        val maxOneTimeKeys = olmDevice.getMaxNumberOfOneTimeKeys()

        // Try to keep at most half that number on the server. This leaves the
        // rest of the slots free to hold keys that have been claimed from the
        // server but we haven't received a message for.
        // If we run out of slots when generating new keys then olm will
        // discard the oldest private keys first. This will eventually clean
        // out stale private keys that won't receive a message.
        val keyLimit = Math.floor(maxOneTimeKeys / 2.0).toInt()
        val result = if (oneTimeKeyCount != null) {
            uploadOTK(oneTimeKeyCount!!, keyLimit)
        } else {
            // ask the server how many keys we have
            val uploadKeysParams = UploadKeysTask.Params(null, null, credentials.deviceId!!)
            uploadKeysTask.execute(uploadKeysParams)
                    .flatMap {
                        // We need to keep a pool of one time public keys on the server so that
                        // other devices can start conversations with us. But we can only store
                        // a finite number of private keys in the olm Account object.
                        // To complicate things further then can be a delay between a device
                        // claiming a public one time key from the server and it sending us a
                        // message. We need to keep the corresponding private key locally until
                        // we receive the message.
                        // But that message might never arrive leaving us stuck with duff
                        // private keys clogging up our local storage.
                        // So we need some kind of engineering compromise to balance all of
                        // these factors.
                        // TODO Why we do not set oneTimeKeyCount here?
                        // TODO This is not needed anymore, see https://github.com/matrix-org/matrix-js-sdk/pull/493 (TODO on iOS also)
                        val keyCount = it.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)
                        uploadOTK(keyCount, keyLimit)
                    }
        }
        return result
                .map {
                    Timber.v("## uploadKeys() : success")
                    oneTimeKeyCount = null
                    oneTimeKeyCheckInProgress = false
                }
                .handleError {
                    Timber.e(it, "## uploadKeys() : failed")
                    oneTimeKeyCount = null
                    oneTimeKeyCheckInProgress = false
                }
    }

    /**
     * Upload some the OTKs.
     *
     * @param keyCount the key count
     * @param keyLimit the limit
     */
    private suspend fun uploadOTK(keyCount: Int, keyLimit: Int): Try<Unit> {
        if (keyLimit <= keyCount) {
            // If we don't need to generate any more keys then we are done.
            return Try.just(Unit)
        }

        val keysThisLoop = Math.min(keyLimit - keyCount, ONE_TIME_KEY_GENERATION_MAX_NUMBER)
        olmDevice.generateOneTimeKeys(keysThisLoop)
        return uploadOneTimeKeys()
                .flatMap {
                    if (it.hasOneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)) {
                        uploadOTK(it.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE), keyLimit)
                    } else {
                        Timber.e("## uploadLoop() : response for uploading keys does not contain one_time_key_counts.signed_curve25519")
                        Try.raise(Exception("response for uploading keys does not contain one_time_key_counts.signed_curve25519"))
                    }
                }
    }

    /**
     * Upload my user's one time keys.
     */
    private suspend fun uploadOneTimeKeys(): Try<KeysUploadResponse> {
        val oneTimeKeys = olmDevice.getOneTimeKeys()
        val oneTimeJson = HashMap<String, Any>()

        val curve25519Map = oneTimeKeys!![OlmAccount.JSON_KEY_ONE_TIME_KEY]

        if (null != curve25519Map) {
            for (key_id in curve25519Map.keys) {
                val k = HashMap<String, Any>()
                k["key"] = curve25519Map.getValue(key_id)

                // the key is also signed
                val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, k)

                k["signatures"] = objectSigner.signObject(canonicalJson)

                oneTimeJson["signed_curve25519:$key_id"] = k
            }
        }

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        val uploadParams = UploadKeysTask.Params(null, oneTimeJson, credentials.deviceId!!)
        return uploadKeysTask
                .execute(uploadParams)
                .map {
                    lastPublishedOneTimeKeys = oneTimeKeys
                    olmDevice.markKeysAsPublished()
                    it
                }
    }

    companion object {
        // max number of keys to upload at once
        // Creating keys can be an expensive operation so we limit the
        // number we generate in one go to avoid blocking the application
        // for too long.
        private const val ONE_TIME_KEY_GENERATION_MAX_NUMBER = 5

        // frequency with which to check & upload one-time keys
        private const val ONE_TIME_KEY_UPLOAD_PERIOD = (60 * 1000).toLong() // one minute
    }
}