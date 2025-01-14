/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.glide

import com.bumptech.glide.load.Option
import org.matrix.android.sdk.api.session.crypto.attachments.ElementToDecrypt

const val ElementToDecryptOptionKey = "im.vector.app.core.glide.ElementToDecrypt"

val ELEMENT_TO_DECRYPT = Option.memory(
        ElementToDecryptOptionKey, ElementToDecrypt("", "", "")
)
