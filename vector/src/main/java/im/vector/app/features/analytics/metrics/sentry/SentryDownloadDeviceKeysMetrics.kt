/*
 * Copyright (c) 2022 New Vector Ltd
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
