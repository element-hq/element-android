/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification

import org.matrix.android.sdk.internal.crypto.tasks.SendToDeviceTask
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class VerificationTransportToDeviceFactory @Inject constructor(
        private val sendToDeviceTask: SendToDeviceTask,
        @DeviceId val myDeviceId: String?,
        private val taskExecutor: TaskExecutor,
        private val clock: Clock,
) {

    fun createTransport(tx: DefaultVerificationTransaction?): VerificationTransportToDevice {
        return VerificationTransportToDevice(tx, sendToDeviceTask, myDeviceId, taskExecutor, clock)
    }
}
