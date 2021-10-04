/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.core.dialogs

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.databinding.DialogDeviceVerifyBinding
import org.matrix.android.sdk.api.extensions.getFingerprintHumanReadable
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo

object ManuallyVerifyDialog {

    fun show(activity: Activity, cryptoDeviceInfo: CryptoDeviceInfo, onVerified: (() -> Unit)) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_device_verify, null)
        val views = DialogDeviceVerifyBinding.bind(dialogLayout)
        val builder = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.cross_signing_verify_by_text)
                .setView(dialogLayout)
                .setPositiveButton(R.string.encryption_information_verify) { _, _ ->
                    onVerified()
                }
                .setNegativeButton(R.string.cancel, null)

        views.encryptedDeviceInfoDeviceName.text = cryptoDeviceInfo.displayName()
        views.encryptedDeviceInfoDeviceId.text = cryptoDeviceInfo.deviceId
        views.encryptedDeviceInfoDeviceKey.text = cryptoDeviceInfo.getFingerprintHumanReadable()

        builder.show()
    }
}
