/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
