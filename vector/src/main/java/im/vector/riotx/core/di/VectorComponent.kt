/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.core.di

import android.content.Context
import android.content.res.Resources
import dagger.BindsInstance
import dagger.Component
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.auth.AuthenticationService
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.ActiveSessionDataSource
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.EmojiCompatWrapper
import im.vector.riotx.VectorApplication
import im.vector.riotx.core.dialogs.UnrecognizedCertificateDialog
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.pushers.PushersManager
import im.vector.riotx.core.utils.AssetReader
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.call.WebRtcPeerConnectionManager
import im.vector.riotx.features.configuration.VectorConfiguration
import im.vector.riotx.features.crypto.keysrequest.KeyRequestHandler
import im.vector.riotx.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.riotx.features.grouplist.SelectedGroupDataSource
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.HomeRoomListDataSource
import im.vector.riotx.features.html.EventHtmlRenderer
import im.vector.riotx.features.html.VectorHtmlCompressor
import im.vector.riotx.features.login.ReAuthHelper
import im.vector.riotx.features.navigation.Navigator
import im.vector.riotx.features.notifications.NotifiableEventResolver
import im.vector.riotx.features.notifications.NotificationBroadcastReceiver
import im.vector.riotx.features.notifications.NotificationDrawerManager
import im.vector.riotx.features.notifications.NotificationUtils
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import im.vector.riotx.features.popup.PopupAlertManager
import im.vector.riotx.features.rageshake.BugReporter
import im.vector.riotx.features.rageshake.VectorFileLogger
import im.vector.riotx.features.rageshake.VectorUncaughtExceptionHandler
import im.vector.riotx.features.reactions.data.EmojiDataSource
import im.vector.riotx.features.session.SessionListener
import im.vector.riotx.features.settings.VectorPreferences
import im.vector.riotx.features.ui.UiStateRepository
import javax.inject.Singleton

@Component(modules = [VectorModule::class])
@Singleton
interface VectorComponent {

    fun inject(notificationBroadcastReceiver: NotificationBroadcastReceiver)

    fun inject(vectorApplication: VectorApplication)

    fun matrix(): Matrix

    fun sessionListener(): SessionListener

    fun currentSession(): Session

    fun notificationUtils(): NotificationUtils

    fun notificationDrawerManager(): NotificationDrawerManager

    fun appContext(): Context

    fun resources(): Resources

    fun assetReader(): AssetReader

    fun dimensionConverter(): DimensionConverter

    fun vectorConfiguration(): VectorConfiguration

    fun avatarRenderer(): AvatarRenderer

    fun activeSessionHolder(): ActiveSessionHolder

    fun unrecognizedCertificateDialog(): UnrecognizedCertificateDialog

    fun emojiCompatFontProvider(): EmojiCompatFontProvider

    fun emojiCompatWrapper(): EmojiCompatWrapper

    fun eventHtmlRenderer(): EventHtmlRenderer

    fun vectorHtmlCompressor(): VectorHtmlCompressor

    fun navigator(): Navigator

    fun errorFormatter(): ErrorFormatter

    fun homeRoomListObservableStore(): HomeRoomListDataSource

    fun selectedGroupStore(): SelectedGroupDataSource

    fun activeSessionObservableStore(): ActiveSessionDataSource

    fun incomingVerificationRequestHandler(): IncomingVerificationRequestHandler

    fun incomingKeyRequestHandler(): KeyRequestHandler

    fun authenticationService(): AuthenticationService

    fun bugReporter(): BugReporter

    fun vectorUncaughtExceptionHandler(): VectorUncaughtExceptionHandler

    fun pushRuleTriggerListener(): PushRuleTriggerListener

    fun pusherManager(): PushersManager

    fun notifiableEventResolver(): NotifiableEventResolver

    fun vectorPreferences(): VectorPreferences

    fun vectorFileLogger(): VectorFileLogger

    fun uiStateRepository(): UiStateRepository

    fun emojiDataSource(): EmojiDataSource

    fun alertManager(): PopupAlertManager

    fun reAuthHelper(): ReAuthHelper

    fun webRtcPeerConnectionManager(): WebRtcPeerConnectionManager

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): VectorComponent
    }
}
