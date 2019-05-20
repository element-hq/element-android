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

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.internal.crypto.model.MXKey
import im.vector.matrix.android.internal.crypto.model.rest.KeysUploadResponse
import im.vector.matrix.android.internal.crypto.tasks.UploadKeysTask
import im.vector.matrix.android.internal.di.MoshiProvider
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.configureWith
import org.matrix.olm.OlmAccount
import timber.log.Timber
import java.util.*

internal class OneTimeKeysUploader(
        private val mCredentials: Credentials,
        private val mOlmDevice: MXOlmDevice,
        private val mObjectSigner: ObjectSigner,
        private val mUploadKeysTask: UploadKeysTask,
        private val mTaskExecutor: TaskExecutor
) {
    // tell if there is a OTK check in progress
    private var mOneTimeKeyCheckInProgress = false

    // last OTK check timestamp
    private var mLastOneTimeKeyCheck: Long = 0

    private var mOneTimeKeyCount: Int? = null

    var mLastPublishedOneTimeKeys: Map<String, Map<String, String>>? = null
        private set

    /**
     * Stores the current one_time_key count which will be handled later (in a call of
     * _onSyncCompleted). The count is e.g. coming from a /sync response.
     *
     * @param currentCount the new count
     */
    fun updateOneTimeKeyCount(currentCount: Int) {
        mOneTimeKeyCount = currentCount
    }


    /**
     * Check if the OTK must be uploaded.
     *
     * @param callback the asynchronous callback
     */
    fun maybeUploadOneTimeKeys(callback: MatrixCallback<Unit>? = null) {
        if (mOneTimeKeyCheckInProgress) {
            callback?.onSuccess(Unit)
            return
        }

        if (System.currentTimeMillis() - mLastOneTimeKeyCheck < ONE_TIME_KEY_UPLOAD_PERIOD) {
            // we've done a key upload recently.
            callback?.onSuccess(Unit)
            return
        }

        mLastOneTimeKeyCheck = System.currentTimeMillis()

        mOneTimeKeyCheckInProgress = true

        // We then check how many keys we can store in the Account object.
        val maxOneTimeKeys = mOlmDevice.getMaxNumberOfOneTimeKeys()

        // Try to keep at most half that number on the server. This leaves the
        // rest of the slots free to hold keys that have been claimed from the
        // server but we haven't received a message for.
        // If we run out of slots when generating new keys then olm will
        // discard the oldest private keys first. This will eventually clean
        // out stale private keys that won't receive a message.
        val keyLimit = Math.floor(maxOneTimeKeys / 2.0).toInt()

        if (null != mOneTimeKeyCount) {
            uploadOTK(mOneTimeKeyCount!!, keyLimit, callback)
        } else {
            // ask the server how many keys we have
            mUploadKeysTask
                    .configureWith(UploadKeysTask.Params(null, null, mCredentials.deviceId!!))
                    .dispatchTo(object : MatrixCallback<KeysUploadResponse> {

                        override fun onSuccess(data: KeysUploadResponse) {
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
                            // TODO Why we do not set mOneTimeKeyCount here?
                            // TODO This is not needed anymore, see https://github.com/matrix-org/matrix-js-sdk/pull/493 (TODO on iOS also)
                            val keyCount = data.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)
                            uploadOTK(keyCount, keyLimit, callback)
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure, "## uploadKeys() : failed")

                            mOneTimeKeyCount = null
                            mOneTimeKeyCheckInProgress = false

                            callback?.onFailure(failure)
                        }
                    })
                    .executeBy(mTaskExecutor)
        }
    }

    /**
     * Upload some the OTKs.
     *
     * @param keyCount the key count
     * @param keyLimit the limit
     * @param callback the asynchronous callback
     */
    private fun uploadOTK(keyCount: Int, keyLimit: Int, callback: MatrixCallback<Unit>?) {
        uploadLoop(keyCount, keyLimit, object : MatrixCallback<Unit> {
            private fun uploadKeysDone(errorMessage: String?) {
                if (null != errorMessage) {
                    Timber.e("## maybeUploadOneTimeKeys() : failed $errorMessage")
                }
                mOneTimeKeyCount = null
                mOneTimeKeyCheckInProgress = false
            }

            override fun onSuccess(data: Unit) {
                Timber.d("## maybeUploadOneTimeKeys() : succeeded")
                uploadKeysDone(null)

                callback?.onSuccess(Unit)
            }

            override fun onFailure(failure: Throwable) {
                uploadKeysDone(failure.message)

                callback?.onFailure(failure)
            }
        })

    }

    /**
     * Upload my user's one time keys.
     * This method must called on getEncryptingThreadHandler() thread.
     * The callback will called on UI thread.
     *
     * @param callback the asynchronous callback
     */
    private fun uploadOneTimeKeys(callback: MatrixCallback<KeysUploadResponse>?) {
        val oneTimeKeys = mOlmDevice.getOneTimeKeys()
        val oneTimeJson = HashMap<String, Any>()

        val curve25519Map = oneTimeKeys!![OlmAccount.JSON_KEY_ONE_TIME_KEY]

        if (null != curve25519Map) {
            for (key_id in curve25519Map.keys) {
                val k = HashMap<String, Any>()
                k["key"] = curve25519Map.getValue(key_id)

                // the key is also signed
                val canonicalJson = MoshiProvider.getCanonicalJson(Map::class.java, k)

                k["signatures"] = mObjectSigner.signObject(canonicalJson)

                oneTimeJson["signed_curve25519:$key_id"] = k
            }
        }

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        mUploadKeysTask
                .configureWith(UploadKeysTask.Params(null, oneTimeJson, mCredentials.deviceId!!))
                .dispatchTo(object : MatrixCallback<KeysUploadResponse> {
                    override fun onSuccess(data: KeysUploadResponse) {
                        mLastPublishedOneTimeKeys = oneTimeKeys
                        mOlmDevice.markKeysAsPublished()

                        callback?.onSuccess(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        callback?.onFailure(failure)
                    }
                })
                .executeBy(mTaskExecutor)
    }

    /**
     * OTK upload loop
     *
     * @param keyCount the number of key to generate
     * @param keyLimit the limit
     * @param callback the asynchronous callback
     */
    private fun uploadLoop(keyCount: Int, keyLimit: Int, callback: MatrixCallback<Unit>) {
        if (keyLimit <= keyCount) {
            // If we don't need to generate any more keys then we are done.
            callback.onSuccess(Unit)
            return
        }

        val keysThisLoop = Math.min(keyLimit - keyCount, ONE_TIME_KEY_GENERATION_MAX_NUMBER)

        mOlmDevice.generateOneTimeKeys(keysThisLoop)

        uploadOneTimeKeys(object : MatrixCallback<KeysUploadResponse> {
            override fun onSuccess(data: KeysUploadResponse) {
                if (data.hasOneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)) {
                    uploadLoop(data.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE), keyLimit, callback)
                } else {
                    Timber.e("## uploadLoop() : response for uploading keys does not contain one_time_key_counts.signed_curve25519")
                    callback.onFailure(
                            Exception("response for uploading keys does not contain one_time_key_counts.signed_curve25519"))
                }
            }

            override fun onFailure(failure: Throwable) {
                callback.onFailure(failure)
            }
        })
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