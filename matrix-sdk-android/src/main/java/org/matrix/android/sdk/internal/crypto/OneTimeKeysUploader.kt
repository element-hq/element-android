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

package org.matrix.android.sdk.internal.crypto

import android.content.Context
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.model.MXKey
import org.matrix.android.sdk.internal.crypto.model.rest.KeysUploadResponse
import org.matrix.android.sdk.internal.crypto.tasks.UploadKeysTask
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.JsonCanonicalizer
import org.matrix.olm.OlmAccount
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.min

// The spec recommend a 5mn delay, but due to federation
// or server downtime we give it a bit more time (1 hour)
private const val FALLBACK_KEY_FORGET_DELAY = 60 * 60_000L

@SessionScope
internal class OneTimeKeysUploader @Inject constructor(
        private val olmDevice: MXOlmDevice,
        private val objectSigner: ObjectSigner,
        private val uploadKeysTask: UploadKeysTask,
        context: Context
) {
    // tell if there is a OTK check in progress
    private var oneTimeKeyCheckInProgress = false

    // last OTK check timestamp
    private var lastOneTimeKeyCheck: Long = 0
    private var oneTimeKeyCount: Int? = null

    // Simple storage to remember when was uploaded the last fallback key
    private val storage = context.getSharedPreferences("OneTimeKeysUploader_${olmDevice.deviceEd25519Key.hashCode()}", Context.MODE_PRIVATE)

    /**
     * Stores the current one_time_key count which will be handled later (in a call of
     * _onSyncCompleted). The count is e.g. coming from a /sync response.
     *
     * @param currentCount the new count
     */
    fun updateOneTimeKeyCount(currentCount: Int) {
        oneTimeKeyCount = currentCount
    }

    fun needsNewFallback() {
        if (olmDevice.generateFallbackKeyIfNeeded()) {
            // As we generated a new one, it's already forgetting one
            // so we can clear the last publish time
            // (in case the network calls fails after to avoid calling forgetKey)
            saveLastFallbackKeyPublishTime(0L)
        }
    }

    /**
     * Check if the OTK must be uploaded.
     */
    suspend fun maybeUploadOneTimeKeys() {
        if (oneTimeKeyCheckInProgress) {
            Timber.v("maybeUploadOneTimeKeys: already in progress")
            return
        }
        if (System.currentTimeMillis() - lastOneTimeKeyCheck < ONE_TIME_KEY_UPLOAD_PERIOD) {
            // we've done a key upload recently.
            Timber.v("maybeUploadOneTimeKeys: executed too recently")
            return
        }

        oneTimeKeyCheckInProgress = true

        val oneTimeKeyCountFromSync = oneTimeKeyCount
                ?: fetchOtkCount() // we don't have count from sync so get from server
                ?: return Unit.also {
                    oneTimeKeyCheckInProgress = false
                    Timber.w("maybeUploadOneTimeKeys: Failed to get otk count from server")
                }

        Timber.d("maybeUploadOneTimeKeys: otk count $oneTimeKeyCountFromSync , unpublished fallback key ${olmDevice.hasUnpublishedFallbackKey()}")

        lastOneTimeKeyCheck = System.currentTimeMillis()

        // We then check how many keys we can store in the Account object.
        val maxOneTimeKeys = olmDevice.getMaxNumberOfOneTimeKeys()

        // Try to keep at most half that number on the server. This leaves the
        // rest of the slots free to hold keys that have been claimed from the
        // server but we haven't received a message for.
        // If we run out of slots when generating new keys then olm will
        // discard the oldest private keys first. This will eventually clean
        // out stale private keys that won't receive a message.
        val keyLimit = floor(maxOneTimeKeys / 2.0).toInt()

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
        tryOrNull("Unable to upload OTK") {
            val uploadedKeys = uploadOTK(oneTimeKeyCountFromSync, keyLimit)
            Timber.v("## uploadKeys() : success, $uploadedKeys key(s) sent")
        }
        oneTimeKeyCheckInProgress = false

        // Check if we need to forget a fallback key
        val latestPublishedTime = getLastFallbackKeyPublishTime()
        if (latestPublishedTime != 0L && System.currentTimeMillis() - latestPublishedTime > FALLBACK_KEY_FORGET_DELAY) {
            // This should be called once you are reasonably certain that you will not receive any more messages
            // that use the old fallback key
            Timber.d("## forgetFallbackKey()")
            olmDevice.forgetFallbackKey()
        }
    }

    private suspend fun fetchOtkCount(): Int? {
        return tryOrNull("Unable to get OTK count") {
            val result = uploadKeysTask.execute(UploadKeysTask.Params(null, null, null))
            result.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)
        }
    }

    /**
     * Upload some the OTKs.
     *
     * @param keyCount the key count
     * @param keyLimit the limit
     * @return the number of uploaded keys
     */
    private suspend fun uploadOTK(keyCount: Int, keyLimit: Int): Int {
        if (keyLimit <= keyCount && !olmDevice.hasUnpublishedFallbackKey()) {
            // If we don't need to generate any more keys then we are done.
            return 0
        }
        var keysThisLoop = 0
        if (keyLimit > keyCount) {
            // Creating keys can be an expensive operation so we limit the
            // number we generate in one go to avoid blocking the application
            // for too long.
            keysThisLoop = min(keyLimit - keyCount, ONE_TIME_KEY_GENERATION_MAX_NUMBER)
            olmDevice.generateOneTimeKeys(keysThisLoop)
        }

        // We check before sending if there is an unpublished key in order to saveLastFallbackKeyPublishTime if needed
        val hadUnpublishedFallbackKey = olmDevice.hasUnpublishedFallbackKey()
        val response = uploadOneTimeKeys(olmDevice.getOneTimeKeys())
        olmDevice.markKeysAsPublished()
        if (hadUnpublishedFallbackKey) {
            // It had an unpublished fallback key that was published just now
            saveLastFallbackKeyPublishTime(System.currentTimeMillis())
        }

        if (response.hasOneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE)) {
            // Maybe upload other keys
            return keysThisLoop +
                    uploadOTK(response.oneTimeKeyCountsForAlgorithm(MXKey.KEY_SIGNED_CURVE_25519_TYPE), keyLimit) +
                    (if (hadUnpublishedFallbackKey) 1 else 0)
        } else {
            Timber.e("## uploadOTK() : response for uploading keys does not contain one_time_key_counts.signed_curve25519")
            throw Exception("response for uploading keys does not contain one_time_key_counts.signed_curve25519")
        }
    }

    private fun saveLastFallbackKeyPublishTime(timeMillis: Long) {
        storage.edit().putLong("last_fb_key_publish", timeMillis).apply()
    }

    private fun getLastFallbackKeyPublishTime(): Long {
        return storage.getLong("last_fb_key_publish", 0)
    }

    /**
     * Upload curve25519 one time keys.
     */
    private suspend fun uploadOneTimeKeys(oneTimeKeys: Map<String, Map<String, String>>?): KeysUploadResponse {
        val oneTimeJson = mutableMapOf<String, Any>()

        val curve25519Map = oneTimeKeys?.get(OlmAccount.JSON_KEY_ONE_TIME_KEY).orEmpty()

        curve25519Map.forEach { (key_id, value) ->
            val k = mutableMapOf<String, Any>()
            k["key"] = value

            // the key is also signed
            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, k)

            k["signatures"] = objectSigner.signObject(canonicalJson)

            oneTimeJson["signed_curve25519:$key_id"] = k
        }

        val fallbackJson = mutableMapOf<String, Any>()
        val fallbackCurve25519Map = olmDevice.getFallbackKey()?.get(OlmAccount.JSON_KEY_ONE_TIME_KEY).orEmpty()
        fallbackCurve25519Map.forEach { (key_id, key) ->
            val k = mutableMapOf<String, Any>()
            k["key"] = key
            k["fallback"] = true
            val canonicalJson = JsonCanonicalizer.getCanonicalJson(Map::class.java, k)
            k["signatures"] = objectSigner.signObject(canonicalJson)

            fallbackJson["signed_curve25519:$key_id"] = k
        }

        // For now, we set the device id explicitly, as we may not be using the
        // same one as used in login.
        val uploadParams = UploadKeysTask.Params(
                deviceKeys = null,
                oneTimeKeys = oneTimeJson,
                fallbackKeys = fallbackJson.takeIf { fallbackJson.isNotEmpty() }
        )
        return uploadKeysTask.executeRetry(uploadParams, 3)
    }

    companion object {
        // max number of keys to upload at once
        // Creating keys can be an expensive operation so we limit the
        // number we generate in one go to avoid blocking the application
        // for too long.
        private const val ONE_TIME_KEY_GENERATION_MAX_NUMBER = 5

        // frequency with which to check & upload one-time keys
        private const val ONE_TIME_KEY_UPLOAD_PERIOD = (60_000).toLong() // one minute
    }
}
