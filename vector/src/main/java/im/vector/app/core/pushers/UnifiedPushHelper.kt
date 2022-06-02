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

package im.vector.app.core.pushers

import android.content.Context
import android.content.pm.PackageManager
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.push.fcm.FcmHelper
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber
import java.net.URL
import javax.inject.Inject

class UnifiedPushHelper @Inject constructor(
        private val context: Context,
        private val unifiedPushStore: UnifiedPushStore,
        private val stringProvider: StringProvider,
        private val vectorPreferences: VectorPreferences,
        private val matrix: Matrix,
) {
    private val up = UnifiedPush

    fun register(
            activity: FragmentActivity,
            onDoneRunnable: Runnable? = null,
    ) {
        gRegister(
                activity,
                onDoneRunnable = onDoneRunnable
        )
    }

    fun reRegister(
            activity: FragmentActivity,
            pushersManager: PushersManager,
            onDoneRunnable: Runnable? = null
    ) {
        gRegister(
                activity,
                force = true,
                pushersManager = pushersManager,
                onDoneRunnable = onDoneRunnable
        )
    }

    private fun gRegister(
            activity: FragmentActivity,
            force: Boolean = false,
            pushersManager: PushersManager? = null,
            onDoneRunnable: Runnable? = null
    ) {
        if (!BuildConfig.ALLOW_EXTERNAL_UNIFIEDPUSH_DISTRIB) {
            up.saveDistributor(context, context.packageName)
            up.registerApp(context)
            onDoneRunnable?.run()
            return
        }
        if (force) {
            // Un-register first
            unregister(pushersManager)
        }
        if (up.getDistributor(context).isNotEmpty()) {
            up.registerApp(context)
            onDoneRunnable?.run()
            return
        }

        // By default, use internal solution (fcm/background sync)
        up.saveDistributor(context, context.packageName)
        val distributors = up.getDistributors(context).toMutableList()

        val internalDistributorName = stringProvider.getString(
                if (FcmHelper.isPushSupported()) {
                    R.string.unifiedpush_distributor_fcm_fallback
                } else {
                    R.string.unifiedpush_distributor_background_sync
                }
        )

        if (distributors.size == 1 && !force) {
            up.saveDistributor(context, distributors.first())
            up.registerApp(context)
            onDoneRunnable?.run()
        } else {
            val distributorsArray = distributors.toTypedArray()
            val distributorsNameArray = distributorsArray.map {
                if (it == context.packageName) {
                    internalDistributorName
                } else {
                    try {
                        val ai = context.packageManager.getApplicationInfo(it, 0)
                        context.packageManager.getApplicationLabel(ai)
                    } catch (e: PackageManager.NameNotFoundException) {
                        it
                    }
                }
            }.toTypedArray()

            MaterialAlertDialogBuilder(activity)
                    .setTitle(stringProvider.getString(R.string.unifiedpush_getdistributors_dialog_title))
                    .setItems(distributorsNameArray) { _, which ->
                        val distributor = distributorsArray[which]
                        up.saveDistributor(context, distributor)
                        Timber.i("Saving distributor: $distributor")
                        up.registerApp(context)
                    }
                    .setOnDismissListener {
                        onDoneRunnable?.run()
                    }
                    .show()
        }
    }

    fun unregister(pushersManager: PushersManager? = null) {
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        runBlocking {
            try {
                pushersManager?.unregisterPusher(unifiedPushStore.getEndpointOrToken().orEmpty())
            } catch (e: Exception) {
                Timber.d(e, "Probably unregistering a non existing pusher")
            }
        }
        unifiedPushStore.storeUpEndpoint(null)
        unifiedPushStore.storePushGateway(null)
        up.unregisterApp(context)
    }

    @JsonClass(generateAdapter = true)
    internal data class DiscoveryResponse(
            @Json(name = "unifiedpush") val unifiedpush: DiscoveryUnifiedPush = DiscoveryUnifiedPush()
    )

    @JsonClass(generateAdapter = true)
    internal data class DiscoveryUnifiedPush(
            @Json(name = "gateway") val gateway: String = ""
    )

    suspend fun storeCustomOrDefaultGateway(
            endpoint: String,
            onDoneRunnable: Runnable? = null
    ) {
        // if we use the embedded distributor,
        // register app_id type upfcm on sygnal
        // the pushkey if FCM key
        if (up.getDistributor(context) == context.packageName) {
            unifiedPushStore.storePushGateway(stringProvider.getString(R.string.pusher_http_url))
            onDoneRunnable?.run()
            return
        }
        // else, unifiedpush, and pushkey is an endpoint
        val gateway = stringProvider.getString(R.string.default_push_gateway_http_url)
        val parsed = URL(endpoint)
        val custom = "${parsed.protocol}://${parsed.host}/_matrix/push/v1/notify"
        Timber.i("Testing $custom")
        try {
            val response = matrix.rawService().getUrl(custom, CacheStrategy.NoCache)
            val moshi = MatrixJsonParser.getMoshi()
            moshi.adapter(DiscoveryResponse::class.java).fromJson(response)
                    ?.let { discoveryResponse ->
                        if (discoveryResponse.unifiedpush.gateway == "matrix") {
                            Timber.d("Using custom gateway")
                            unifiedPushStore.storePushGateway(custom)
                            onDoneRunnable?.run()
                            return
                        }
                    }
        } catch (e: Throwable) {
            Timber.d(e, "Cannot try custom gateway")
        }
        unifiedPushStore.storePushGateway(gateway)
        onDoneRunnable?.run()
    }

    fun getExternalDistributors(): List<String> {
        return up.getDistributors(context)
                .filterNot { it == context.packageName }
    }

    fun getCurrentDistributorName(): String {
        if (isEmbeddedDistributor()) {
            return stringProvider.getString(R.string.unifiedpush_distributor_fcm_fallback)
        }
        if (isBackgroundSync()) {
            return stringProvider.getString(R.string.unifiedpush_distributor_background_sync)
        }
        val distributor = up.getDistributor(context)
        return try {
            val ai = context.packageManager.getApplicationInfo(distributor, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            distributor
        }
    }

    fun isEmbeddedDistributor(): Boolean {
        return up.getDistributor(context) == context.packageName && FcmHelper.isPushSupported()
    }

    fun isBackgroundSync(): Boolean {
        return up.getDistributor(context) == context.packageName && !FcmHelper.isPushSupported()
    }

    fun getPrivacyFriendlyUpEndpoint(): String? {
        val endpoint = unifiedPushStore.getEndpointOrToken()
        if (endpoint.isNullOrEmpty()) return endpoint
        if (isEmbeddedDistributor()) {
            return endpoint
        }
        return try {
            val parsed = URL(endpoint)
            "${parsed.protocol}://${parsed.host}/***"
        } catch (e: Exception) {
            Timber.e(e, "Error parsing unifiedpush endpoint")
            null
        }
    }
}
