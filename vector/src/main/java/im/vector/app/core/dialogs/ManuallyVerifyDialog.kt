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
import im.vector.app.databinding.DialogDeviceVerifyBinding
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.getFingerprintHumanReadable
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo

object ManuallyVerifyDialog {

    fun show(activity: Activity, cryptoDeviceInfo: CryptoDeviceInfo, onVerified: (() -> Unit)) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_device_verify, null)
        val views = DialogDeviceVerifyBinding.bind(dialogLayout)
        val builder = MaterialAlertDialogBuilder(activity)
                .setTitle(CommonStrings.cross_signing_verify_by_text)
                .setView(dialogLayout)
                .setPositiveButton(CommonStrings.encryption_information_verify) { _, _ ->
                    onVerified()
                }
                .setNegativeButton(CommonStrings.action_cancel, null)

        views.encryptedDeviceInfoDeviceName.text = cryptoDeviceInfo.displayName()
        views.encryptedDeviceInfoDeviceId.text = cryptoDeviceInfo.deviceId
        views.encryptedDeviceInfoDeviceKey.text = cryptoDeviceInfo.getFingerprintHumanReadable()

        builder.show()
    }
}
