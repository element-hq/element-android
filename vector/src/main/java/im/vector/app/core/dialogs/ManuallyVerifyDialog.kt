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
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import im.vector.app.R
import org.matrix.android.sdk.api.extensions.getFingerprintHumanReadable
import org.matrix.android.sdk.internal.crypto.model.CryptoDeviceInfo

object ManuallyVerifyDialog {

    fun show(activity: Activity, cryptoDeviceInfo: CryptoDeviceInfo, onVerified: (() -> Unit)) {
        val dialogLayout = activity.layoutInflater.inflate(R.layout.dialog_device_verify, null)
        val builder = AlertDialog.Builder(activity)
                .setTitle(R.string.cross_signing_verify_by_text)
                .setView(dialogLayout)
                .setPositiveButton(R.string.encryption_information_verify) { _, _ ->
                    onVerified()
                }
                .setNegativeButton(R.string.cancel, null)

        dialogLayout.findViewById<TextView>(R.id.encrypted_device_info_device_name)?.let {
            it.text = cryptoDeviceInfo.displayName()
        }

        dialogLayout.findViewById<TextView>(R.id.encrypted_device_info_device_id)?.let {
            it.text = cryptoDeviceInfo.deviceId
        }

        dialogLayout.findViewById<TextView>(R.id.encrypted_device_info_device_key)?.let {
            it.text = cryptoDeviceInfo.getFingerprintHumanReadable()
        }

        builder.show()
    }
}
