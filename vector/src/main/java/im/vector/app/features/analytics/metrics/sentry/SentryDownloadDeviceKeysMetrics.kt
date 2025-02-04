/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.analytics.metrics.sentry

import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import org.matrix.android.sdk.api.metrics.DownloadDeviceKeysMetricsPlugin
import javax.inject.Inject

class SentryDownloadDeviceKeysMetrics @Inject constructor() : DownloadDeviceKeysMetricsPlugin {
    private var transaction: ITransaction? = null

    override fun startTransaction() {
        if (Sentry.isEnabled()) {
            transaction = Sentry.startTransaction("download_device_keys", "task")
            logTransaction("Sentry transaction started")
        }
    }

    override fun finishTransaction() {
        transaction?.finish()
        logTransaction("Sentry transaction finished")
    }

    override fun onError(throwable: Throwable) {
        transaction?.apply {
            this.throwable = throwable
            this.status = SpanStatus.INTERNAL_ERROR
        }
        logTransaction("Sentry transaction encountered error ${throwable.message}")
    }
}
