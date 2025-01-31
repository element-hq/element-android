/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.crypto.model.rest

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.matrix.android.sdk.api.session.crypto.model.SendToDeviceObject
import org.matrix.android.sdk.internal.crypto.verification.VerificationInfoDone

/**
 * Requests a key verification with another user's devices.
 */
@JsonClass(generateAdapter = true)
internal data class KeyVerificationDone(
        @Json(name = "transaction_id") override val transactionId: String? = null
) : SendToDeviceObject, VerificationInfoDone {

    override fun toSendToDeviceObject() = this
}
