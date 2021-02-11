/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.core.di

import android.content.Context
import android.content.res.Resources
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import im.vector.app.ActiveSessionDataSource
import im.vector.app.EmojiCompatFontProvider
import im.vector.app.EmojiCompatWrapper
import im.vector.app.core.dialogs.UnrecognizedCertificateDialog
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.pushers.PushersManager
import im.vector.app.core.utils.AssetReader
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.configuration.VectorConfiguration
import im.vector.app.features.crypto.keysrequest.KeyRequestHandler
import im.vector.app.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.app.features.grouplist.SelectedGroupDataSource
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.HomeRoomListDataSource
import im.vector.app.features.home.room.detail.RoomDetailPendingActionStore
import im.vector.app.features.home.room.detail.timeline.helper.MatrixItemColorProvider
import im.vector.app.features.home.room.detail.timeline.helper.RoomSummariesHolder
import im.vector.app.features.html.EventHtmlRenderer
import im.vector.app.features.html.VectorHtmlCompressor
import im.vector.app.features.login.ReAuthHelper
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotifiableEventResolver
import im.vector.app.features.notifications.NotificationBroadcastReceiver
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.notifications.NotificationUtils
import im.vector.app.features.notifications.PushRuleTriggerListener
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.PinLocker
import im.vector.app.features.popup.PopupAlertManager
import im.vector.app.features.rageshake.BugReporter
import im.vector.app.features.rageshake.VectorFileLogger
import im.vector.app.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.app.features.reactions.data.EmojiDataSource
import im.vector.app.features.session.SessionListener
import im.vector.app.features.settings.VectorPreferences
import im.vector.app.features.ui.UiStateRepository
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.AuthenticationService
import org.matrix.android.sdk.api.auth.HomeServerHistoryService
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session

@InstallIn(SingletonComponent::class)
@EntryPoint
interface SingletonEntryPoint{

  fun sessionListener(): SessionListener

  fun currentSession(): Session

  fun avatarRenderer(): AvatarRenderer

  fun activeSessionHolder(): ActiveSessionHolder

  fun unrecognizedCertificateDialog(): UnrecognizedCertificateDialog

  fun navigator(): Navigator

  fun errorFormatter(): ErrorFormatter

  fun bugReporter(): BugReporter

  fun vectorPreferences(): VectorPreferences

  fun vectorFileLogger(): VectorFileLogger

  fun uiStateRepository(): UiStateRepository

  fun pinLocker(): PinLocker


}
