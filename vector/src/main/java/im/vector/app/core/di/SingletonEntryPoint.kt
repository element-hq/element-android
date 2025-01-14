/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.session.SessionListener
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.UiStateRepository
import im.vector.lib.core.utils.timer.Clock
import kotlinx.coroutines.CoroutineScope

@InstallIn(SingletonComponent::class)
@EntryPoint
interface SingletonEntryPoint {

    fun sessionListener(): SessionListener

    fun avatarRenderer(): AvatarRenderer

    fun activeSessionHolder(): ActiveSessionHolder

    fun unrecognizedCertificateDialog(): UnrecognizedCertificateDialog

    fun navigator(): Navigator

    fun clock(): Clock

    fun errorFormatter(): ErrorFormatter

    fun bugReporter(): BugReporter

    fun vectorPreferences(): VectorPreferences

    fun uiStateRepository(): UiStateRepository

    fun pinLocker(): PinLocker

    fun analyticsTracker(): AnalyticsTracker

    fun webRtcCallManager(): WebRtcCallManager

    fun appCoroutineScope(): CoroutineScope
}
