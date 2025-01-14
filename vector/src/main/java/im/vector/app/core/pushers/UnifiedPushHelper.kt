/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.pushers

import android.content.Context
import androidx.annotation.MainThread
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.getApplicationLabel
import im.vector.app.features.mdm.MdmData
import im.vector.app.features.mdm.MdmService
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.cache.CacheStrategy
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.unifiedpush.android.connector.UnifiedPush
import timber.log.Timber
import java.net.URL
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException

class UnifiedPushHelper @Inject constructor(
        private val context: Context,
        private val unifiedPushStore: UnifiedPushStore,
        private val stringProvider: StringProvider,
        private val matrix: Matrix,
        private val fcmHelper: FcmHelper,
        private val mdmService: MdmService,
) {

    @MainThread
    fun showSelectDistributorDialog(
            context: Context,
            onDistributorSelected: (String) -> Unit,
    ) {
        val internalDistributorName = stringProvider.getString(
                if (fcmHelper.isFirebaseAvailable()) {
                    CommonStrings.unifiedpush_distributor_fcm_fallback
                } else {
                    CommonStrings.unifiedpush_distributor_background_sync
                }
        )

        val distributors = UnifiedPush.getDistributors(context)
        val distributorsName = distributors.map {
            if (it == context.packageName) {
                internalDistributorName
            } else {
                context.getApplicationLabel(it)
            }
        }

        MaterialAlertDialogBuilder(context)
                .setTitle(stringProvider.getString(CommonStrings.unifiedpush_getdistributors_dialog_title))
                .setItems(distributorsName.toTypedArray()) { _, which ->
                    val distributor = distributors[which]
                    onDistributorSelected(distributor)
                }
                .setOnCancelListener {
                    // we do not want to change the distributor on behalf of the user
                    if (UnifiedPush.getDistributor(context).isEmpty()) {
                        // By default, use internal solution (fcm/background sync)
                        onDistributorSelected(context.packageName)
                    }
                }
                .setCancelable(true)
                .show()
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
            unifiedPushStore.storePushGateway(
                    gateway = mdmService.getData(
                            mdmData = MdmData.DefaultPushGatewayUrl,
                            defaultValue = stringProvider.getString(im.vector.app.config.R.string.pusher_http_url),
                    )
            )
            onDoneRunnable?.run()
            return
        }
        // else, unifiedpush, and pushkey is an endpoint
        val gateway = stringProvider.getString(im.vector.app.config.R.string.default_push_gateway_http_url)
        val parsed = URL(endpoint)
        val port = if (parsed.port != -1) {
            ":${parsed.port}"
        } else {
            ""
        }
        val custom = "${parsed.protocol}://${parsed.host}${port}/_matrix/push/v1/notify"
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
            Timber.e(e, "Cannot try custom gateway")
            if (e is Failure.NetworkConnection && e.ioException is SSLHandshakeException) {
                Timber.w(e, "SSLHandshakeException, ignore this error")
                unifiedPushStore.storePushGateway(custom)
                onDoneRunnable?.run()
                return
            }
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
            isEmbeddedDistributor() -> stringProvider.getString(CommonStrings.unifiedpush_distributor_fcm_fallback)
            isBackgroundSync() -> stringProvider.getString(CommonStrings.unifiedpush_distributor_background_sync)
            else -> context.getApplicationLabel(UnifiedPush.getDistributor(context))
        }
    }

    fun isEmbeddedDistributor(): Boolean {
        return isInternalDistributor() && fcmHelper.isFirebaseAvailable()
    }

    fun isBackgroundSync(): Boolean {
        return isInternalDistributor() && !fcmHelper.isFirebaseAvailable()
    }

    private fun isInternalDistributor(): Boolean {
        return UnifiedPush.getDistributor(context).isEmpty() ||
                UnifiedPush.getDistributor(context) == context.packageName
    }

    fun getPrivacyFriendlyUpEndpoint(): String? {
        val endpoint = getEndpointOrToken()
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

    fun getEndpointOrToken(): String? {
        return if (isEmbeddedDistributor()) fcmHelper.getFcmToken()
        else unifiedPushStore.getEndpoint()
    }

    fun getPushGateway(): String? {
        return if (isEmbeddedDistributor()) {
            mdmService.getData(
                    mdmData = MdmData.DefaultPushGatewayUrl,
                    defaultValue = stringProvider.getString(im.vector.app.config.R.string.pusher_http_url),
            )
        } else {
            unifiedPushStore.getPushGateway()
        }
    }
}
