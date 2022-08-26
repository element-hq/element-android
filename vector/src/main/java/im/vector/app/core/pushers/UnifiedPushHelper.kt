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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.getApplicationLabel
import im.vector.app.features.VectorFeatures
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import kotlinx.coroutines.launch
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
        private val vectorFeatures: VectorFeatures,
        private val fcmHelper: FcmHelper,
) {
    fun register(
            activity: FragmentActivity,
            onDoneRunnable: Runnable? = null,
    ) {
        registerInternal(
                activity,
                onDoneRunnable = onDoneRunnable
        )
    }

    fun reRegister(
            activity: FragmentActivity,
            pushersManager: PushersManager,
            onDoneRunnable: Runnable? = null
    ) {
        registerInternal(
                activity,
                force = true,
                pushersManager = pushersManager,
                onDoneRunnable = onDoneRunnable
        )
    }

    private fun registerInternal(
            activity: FragmentActivity,
            force: Boolean = false,
            pushersManager: PushersManager? = null,
            onDoneRunnable: Runnable? = null
    ) {
        activity.lifecycleScope.launch {
            if (!vectorFeatures.allowExternalUnifiedPushDistributors()) {
                UnifiedPush.saveDistributor(context, context.packageName)
                UnifiedPush.registerApp(context)
                onDoneRunnable?.run()
                return@launch
            }
            if (force) {
                // Un-register first
                unregister(pushersManager)
            }
            if (UnifiedPush.getDistributor(context).isNotEmpty()) {
                UnifiedPush.registerApp(context)
                onDoneRunnable?.run()
                return@launch
            }

            val distributors = UnifiedPush.getDistributors(context)

            if (distributors.size == 1 && !force) {
                UnifiedPush.saveDistributor(context, distributors.first())
                UnifiedPush.registerApp(context)
                onDoneRunnable?.run()
            } else {
                openDistributorDialogInternal(
                        activity = activity,
                        pushersManager = pushersManager,
                        onDoneRunnable = onDoneRunnable,
                        distributors = distributors,
                        unregisterFirst = force,
                        cancellable = !force
                )
            }
        }
    }

    fun openDistributorDialog(
            activity: FragmentActivity,
            pushersManager: PushersManager,
            onDoneRunnable: Runnable,
    ) {
        val distributors = UnifiedPush.getDistributors(activity)
        openDistributorDialogInternal(
                activity,
                pushersManager,
                onDoneRunnable, distributors,
                unregisterFirst = true,
                cancellable = true,
        )
    }

    private fun openDistributorDialogInternal(
            activity: FragmentActivity,
            pushersManager: PushersManager?,
            onDoneRunnable: Runnable?,
            distributors: List<String>,
            unregisterFirst: Boolean,
            cancellable: Boolean,
    ) {
        val internalDistributorName = stringProvider.getString(
                if (fcmHelper.isFirebaseAvailable()) {
                    R.string.unifiedpush_distributor_fcm_fallback
                } else {
                    R.string.unifiedpush_distributor_background_sync
                }
        )

        val distributorsName = distributors.map {
            if (it == context.packageName) {
                internalDistributorName
            } else {
                context.getApplicationLabel(it)
            }
        }

        MaterialAlertDialogBuilder(activity)
                .setTitle(stringProvider.getString(R.string.unifiedpush_getdistributors_dialog_title))
                .setItems(distributorsName.toTypedArray()) { _, which ->
                    val distributor = distributors[which]
                    if (distributor == UnifiedPush.getDistributor(context)) {
                        Timber.d("Same distributor selected again, no action")
                        return@setItems
                    }

                    activity.lifecycleScope.launch {
                        if (unregisterFirst) {
                            // Un-register first
                            unregister(pushersManager)
                        }
                        UnifiedPush.saveDistributor(context, distributor)
                        Timber.i("Saving distributor: $distributor")
                        UnifiedPush.registerApp(context)
                        onDoneRunnable?.run()
                    }
                }
                .setOnCancelListener {
                    // By default, use internal solution (fcm/background sync)
                    UnifiedPush.saveDistributor(context, context.packageName)
                    UnifiedPush.registerApp(context)
                    onDoneRunnable?.run()
                }
                .setCancelable(cancellable)
                .show()
    }

    suspend fun unregister(pushersManager: PushersManager? = null) {
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences.setFdroidSyncBackgroundMode(mode)
        try {
            pushersManager?.unregisterPusher(unifiedPushStore.getEndpointOrToken().orEmpty())
        } catch (e: Exception) {
            Timber.d(e, "Probably unregistering a non existing pusher")
        }
        unifiedPushStore.storeUpEndpoint(null)
        unifiedPushStore.storePushGateway(null)
        UnifiedPush.unregisterApp(context)
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
        if (UnifiedPush.getDistributor(context) == context.packageName) {
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
        return UnifiedPush.getDistributors(context)
                .filterNot { it == context.packageName }
    }

    fun getCurrentDistributorName(): String {
        return when {
            isEmbeddedDistributor() -> stringProvider.getString(R.string.unifiedpush_distributor_fcm_fallback)
            isBackgroundSync() -> stringProvider.getString(R.string.unifiedpush_distributor_background_sync)
            else -> context.getApplicationLabel(UnifiedPush.getDistributor(context))
        }
    }

    fun isEmbeddedDistributor(): Boolean {
        return UnifiedPush.getDistributor(context) == context.packageName && fcmHelper.isFirebaseAvailable()
    }

    fun isBackgroundSync(): Boolean {
        return UnifiedPush.getDistributor(context) == context.packageName && !fcmHelper.isFirebaseAvailable()
    }

    fun getPrivacyFriendlyUpEndpoint(): String? {
        val endpoint = unifiedPushStore.getEndpointOrToken()
        if (endpoint.isNullOrEmpty()) return null
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
