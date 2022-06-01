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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.core.di.DefaultSharedPreferences
import im.vector.app.features.settings.BackgroundSyncMode
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.push.fcm.FcmHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber
import java.net.URL

object UnifiedPushHelper {
    private const val PREFS_ENDPOINT_OR_TOKEN = "UP_ENDPOINT_OR_TOKEN"
    private const val PREFS_PUSH_GATEWAY = "PUSH_GATEWAY"
    private val up = UnifiedPush

    /**
     * Retrieves the UnifiedPush Endpoint.
     *
     * @return the UnifiedPush Endpoint or null if not received
     */
    fun getEndpointOrToken(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_ENDPOINT_OR_TOKEN, null)
    }

    /**
     * Store UnifiedPush Endpoint to the SharedPrefs.
     * TODO Store in realm
     *
     * @param context android context
     * @param endpoint the endpoint to store
     */
    fun storeUpEndpoint(context: Context,
                        endpoint: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_ENDPOINT_OR_TOKEN, endpoint)
        }
    }

    /**
     * Retrieves the Push Gateway.
     *
     * @return the Push Gateway or null if not defined
     */
    fun getPushGateway(context: Context): String? {
        return DefaultSharedPreferences.getInstance(context).getString(PREFS_PUSH_GATEWAY, null)
    }

    /**
     * Store Push Gateway to the SharedPrefs.
     * TODO Store in realm
     *
     * @param context android context
     * @param gateway the push gateway to store
     */
    private fun storePushGateway(context: Context,
                         gateway: String?) {
        DefaultSharedPreferences.getInstance(context).edit {
            putString(PREFS_PUSH_GATEWAY, gateway)
        }
    }

    fun register(context: Context, onDoneRunnable: Runnable? = null) {
        gRegister(context,
                onDoneRunnable = onDoneRunnable)
    }

    fun reRegister(context: Context,
                   pushersManager: PushersManager,
                   vectorPreferences: VectorPreferences,
                   onDoneRunnable: Runnable? = null) {
        gRegister(context,
                force = true,
                pushersManager = pushersManager,
                vectorPreferences = vectorPreferences,
                onDoneRunnable = onDoneRunnable
        )
    }

    private fun gRegister(context: Context,
                 force: Boolean = false,
                 pushersManager: PushersManager? = null,
                 vectorPreferences: VectorPreferences? = null,
                 onDoneRunnable: Runnable? = null) {
        if (!BuildConfig.ALLOW_EXTERNAL_UNIFIEDPUSH_DISTRIB) {
            up.saveDistributor(context, context.packageName)
            up.registerApp(context)
            onDoneRunnable?.run()
            return
        }
        if (force) {
            // Un-register first
            unregister(context, pushersManager, vectorPreferences)
        }
        if (up.getDistributor(context).isNotEmpty()) {
            up.registerApp(context)
            onDoneRunnable?.run()
            return
        }

        // By default, use internal solution (fcm/background sync)
        up.saveDistributor(context, context.packageName)
        val distributors = up.getDistributors(context).toMutableList()

        val internalDistributorName = if (FcmHelper.isPushSupported()) {
            context.getString(R.string.unifiedpush_distributor_fcm_fallback)
        } else {
            context.getString(R.string.unifiedpush_distributor_background_sync)
        }

        if (distributors.size == 1 &&
                !force) {
            up.saveDistributor(context, distributors.first())
            up.registerApp(context)
            onDoneRunnable?.run()
        } else {
            val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(context)
            builder.setTitle(context.getString(R.string.unifiedpush_getdistributors_dialog_title))

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
                    } as String
                }
            }.toTypedArray()
            builder.setItems(distributorsNameArray) { _, which ->
                val distributor = distributorsArray[which]
                up.saveDistributor(context, distributor)
                Timber.i("Saving distributor: $distributor")
                up.registerApp(context)
                onDoneRunnable?.run()
            }
            builder.setOnDismissListener {
                onDoneRunnable?.run()
            }
            builder.setOnCancelListener {
                onDoneRunnable?.run()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    fun unregister(
            context: Context,
            pushersManager: PushersManager? = null,
            vectorPreferences: VectorPreferences? = null
    ) {
        val mode = BackgroundSyncMode.FDROID_BACKGROUND_SYNC_MODE_FOR_REALTIME
        vectorPreferences?.setFdroidSyncBackgroundMode(mode)
        runBlocking {
            try {
                pushersManager?.unregisterPusher(getEndpointOrToken(context) ?: "")
            } catch (e: Exception) {
                Timber.d("Probably unregistering a non existant pusher")
            }
        }
        storeUpEndpoint(context, null)
        storePushGateway(context, null)
        up.unregisterApp(context)
    }

    @JsonClass(generateAdapter = true)
    internal data class DiscoveryResponse(
            val unifiedpush: DiscoveryUnifiedPush = DiscoveryUnifiedPush()
    )

    @JsonClass(generateAdapter = true)
    internal data class DiscoveryUnifiedPush(
            val gateway: String = ""
    )

    fun storeCustomOrDefaultGateway(
            context: Context,
            endpoint: String,
            onDoneRunnable: Runnable? = null) {
        // if we use the embedded distributor,
        // register app_id type upfcm on sygnal
        // the pushkey if FCM key
        if (up.getDistributor(context) == context.packageName) {
            context.getString(R.string.pusher_http_url).let {
                storePushGateway(context, it)
                onDoneRunnable?.run()
                return
            }
        }
        // else, unifiedpush, and pushkey is an endpoint
        val gateway = context.getString(R.string.default_push_gateway_http_url)
        val parsed = URL(endpoint)
        val custom = "${parsed.protocol}://${parsed.host}/_matrix/push/v1/notify"
        Timber.i("Testing $custom")
        val thread = CoroutineScope(SupervisorJob()).launch {
            try {
                val moshi: Moshi = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                val client = OkHttpClient()
                val request = Request.Builder()
                        .url(custom)
                        .build()
                    val sResponse = client.newCall(request).execute()
                            .body?.string() ?: ""
                    moshi.adapter(DiscoveryResponse::class.java)
                            .fromJson(sResponse)?.let { response ->
                                if (response.unifiedpush.gateway == "matrix") {
                                    Timber.d("Using custom gateway")
                                    storePushGateway(context, custom)
                                    onDoneRunnable?.run()
                                    return@launch
                                }
                            }
            } catch (e: Exception) {
                Timber.d("Cannot try custom gateway: $e")
            }
            storePushGateway(context, gateway)
            onDoneRunnable?.run()
            return@launch
        }
        thread.start()
    }

    fun getExternalDistributors(context: Context): List<String> {
        val distributors =  up.getDistributors(context).toMutableList()
        distributors.remove(context.packageName)
        return distributors
    }

    fun getCurrentDistributorName(context: Context): String {
        if (isEmbeddedDistributor(context)) {
            return context.getString(R.string.unifiedpush_distributor_fcm_fallback)
        }
        if (isBackgroundSync(context)) {
            return context.getString(R.string.unifiedpush_distributor_background_sync)
        }
        val distributor = up.getDistributor(context)
        return try {
            val ai = context.packageManager.getApplicationInfo(distributor, 0)
            context.packageManager.getApplicationLabel(ai)
        } catch (e: PackageManager.NameNotFoundException) {
            distributor
        } as String
    }

    fun isEmbeddedDistributor(context: Context): Boolean {
        return (up.getDistributor(context) == context.packageName &&
                FcmHelper.isPushSupported())
    }

    fun isBackgroundSync(context: Context): Boolean {
        return (up.getDistributor(context) == context.packageName &&
                !FcmHelper.isPushSupported())
    }

    fun getPrivacyFriendlyUpEndpoint(context: Context): String? {
        val endpoint = getEndpointOrToken(context)
        if (endpoint.isNullOrEmpty()) return endpoint
        if (isEmbeddedDistributor(context)) {
            return endpoint
        }
        return try {
            val parsed = URL(endpoint)
            "${parsed.protocol}://${parsed.host}/***"
        } catch (e: Exception) {
            Timber.e("Error parsing unifiedpush endpoint: $e")
            null
        }
    }
}
