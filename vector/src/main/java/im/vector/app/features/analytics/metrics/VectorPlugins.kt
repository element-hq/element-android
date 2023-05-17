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

package im.vector.app.features.analytics.metrics

import im.vector.app.features.analytics.metrics.sentry.SentryCryptoAnalytics
import im.vector.app.features.analytics.metrics.sentry.SentryDownloadDeviceKeysMetrics
import im.vector.app.features.analytics.metrics.sentry.SentrySyncDurationMetrics
import org.matrix.android.sdk.api.metrics.MetricPlugin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class that contains the all plugins which can be used for tracking.
 */
@Singleton
data class VectorPlugins @Inject constructor(
        val sentryDownloadDeviceKeysMetrics: SentryDownloadDeviceKeysMetrics,
        val sentrySyncDurationMetrics: SentrySyncDurationMetrics,
        val cryptoMetricPlugin: SentryCryptoAnalytics
) {
    /**
     * Returns [List] of all [MetricPlugin] hold by this class.
     */
    fun plugins(): List<MetricPlugin> = listOf(sentryDownloadDeviceKeysMetrics, sentrySyncDurationMetrics)
}
