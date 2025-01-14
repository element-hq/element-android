/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.dialogs

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.DialogSslFingerprintBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.network.ssl.Fingerprint
import timber.log.Timber
import javax.inject.Inject

/**
 * This class displays the unknown certificate dialog.
 */
class UnrecognizedCertificateDialog @Inject constructor(
        private val activeSessionHolder: ActiveSessionHolder,
        private val stringProvider: StringProvider
) {
    private val ignoredFingerprints: MutableMap<String, MutableSet<Fingerprint>> = HashMap()
    private val openDialogIds: MutableSet<String> = HashSet()

    /**
     * Display a certificate dialog box, asking the user about an unknown certificate
     * To use when user is currently logged in.
     *
     * @param activity the Android activity
     * @param unrecognizedFingerprint the fingerprint for the unknown certificate
     * @param callback callback to fire when the user makes a decision
     */
    fun show(
            activity: Activity,
            unrecognizedFingerprint: Fingerprint,
            callback: Callback
    ) {
        val userId = activeSessionHolder.getSafeActiveSession()?.myUserId
        val hsConfig = activeSessionHolder.getSafeActiveSession()?.sessionParams?.homeServerConnectionConfig ?: return

        internalShow(
                activity = activity,
                unrecognizedFingerprint = unrecognizedFingerprint,
                existing = true,
                callback = callback,
                userId = userId,
                homeServerUrl = hsConfig.homeServerUriBase.toString(),
                homeServerConnectionConfigHasFingerprints = hsConfig.allowedFingerprints.isNotEmpty()
        )
    }

    /**
     * To use during login flow.
     */
    fun show(
            activity: Activity,
            unrecognizedFingerprint: Fingerprint,
            homeServerUrl: String,
            callback: Callback
    ) {
        internalShow(
                activity = activity,
                unrecognizedFingerprint = unrecognizedFingerprint,
                existing = false,
                callback = callback,
                userId = null,
                homeServerUrl = homeServerUrl,
                homeServerConnectionConfigHasFingerprints = false
        )
    }

    /**
     * Display a certificate dialog box, asking the user about an unknown certificate.
     *
     * @param activity the Activity
     * @param unrecognizedFingerprint the fingerprint for the unknown certificate
     * @param existing the current session already exist, so it mean that something has changed server side
     * @param callback callback to fire when the user makes a decision
     * @param userId the matrix userId
     * @param homeServerUrl the homeserver url
     * @param homeServerConnectionConfigHasFingerprints true if the homeServerConnectionConfig has fingerprint
     */
    private fun internalShow(
            activity: Activity,
            unrecognizedFingerprint: Fingerprint,
            existing: Boolean,
            callback: Callback,
            userId: String?,
            homeServerUrl: String,
            homeServerConnectionConfigHasFingerprints: Boolean
    ) {
        val dialogId = userId ?: homeServerUrl + unrecognizedFingerprint.displayableHexRepr

        if (openDialogIds.contains(dialogId)) {
            Timber.i("Not opening dialog $dialogId as one is already open.")
            return
        }

        if (userId != null) {
            val f: Set<Fingerprint>? = ignoredFingerprints[userId]
            if (f != null && f.contains(unrecognizedFingerprint)) {
                callback.onIgnore()
                return
            }
        }

        val builder = MaterialAlertDialogBuilder(activity)
        val inflater = activity.layoutInflater
        val layout = inflater.inflate(R.layout.dialog_ssl_fingerprint, null)
        val views = DialogSslFingerprintBinding.bind(layout)
        views.sslFingerprintTitle.text = stringProvider.getString(CommonStrings.ssl_fingerprint_hash, unrecognizedFingerprint.hashType.toString())
        views.sslFingerprint.text = unrecognizedFingerprint.displayableHexRepr
        if (userId != null) {
            views.sslUserId.text = stringProvider.getString(
                    CommonStrings.generic_label_and_value,
                    stringProvider.getString(CommonStrings.username),
                    userId
            )
        } else {
            views.sslUserId.text = stringProvider.getString(
                    CommonStrings.generic_label_and_value,
                    stringProvider.getString(CommonStrings.hs_url),
                    homeServerUrl
            )
        }
        if (existing) {
            if (homeServerConnectionConfigHasFingerprints) {
                views.sslExplanation.text = stringProvider.getString(CommonStrings.ssl_expected_existing_expl)
            } else {
                views.sslExplanation.text = stringProvider.getString(CommonStrings.ssl_unexpected_existing_expl)
            }
        } else {
            views.sslExplanation.text = stringProvider.getString(CommonStrings.ssl_cert_new_account_expl)
        }
        builder.setView(layout)
        builder.setTitle(CommonStrings.ssl_could_not_verify)
        builder.setPositiveButton(CommonStrings.ssl_trust) { _, _ ->
            callback.onAccept()
        }
        if (existing) {
            builder.setNegativeButton(CommonStrings.ssl_remain_offline) { _, _ ->
                if (userId != null) {
                    var f = ignoredFingerprints[userId]
                    if (f == null) {
                        f = HashSet()
                        ignoredFingerprints[userId] = f
                    }
                    f.add(unrecognizedFingerprint)
                }
                callback.onIgnore()
            }
            builder.setNeutralButton(CommonStrings.ssl_logout_account) { _, _ -> callback.onReject() }
        } else {
            builder.setNegativeButton(CommonStrings.action_cancel) { _, _ -> callback.onReject() }
        }

        builder.setOnDismissListener {
            Timber.d("Dismissed!")
            openDialogIds.remove(dialogId)
        }

        builder.show()
        openDialogIds.add(dialogId)
    }

    interface Callback {
        /**
         * The certificate was explicitly accepted.
         */
        fun onAccept()

        /**
         * The warning was ignored by the user.
         */
        fun onIgnore()

        /**
         * The unknown certificate was explicitly rejected.
         */
        fun onReject()
    }
}
