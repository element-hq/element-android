/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.signout

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class BuildConfirmSignoutDialogUseCase @Inject constructor() {

    fun execute(context: Context, onConfirm: () -> Unit) =
            MaterialAlertDialogBuilder(context)
                    .setTitle(CommonStrings.action_sign_out)
                    .setMessage(CommonStrings.action_sign_out_confirmation_simple)
                    .setPositiveButton(CommonStrings.action_sign_out) { _, _ ->
                        onConfirm()
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .create()
}
