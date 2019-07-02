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
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.VectorApplication
import im.vector.riotx.core.pushers.PushersManager
import im.vector.riotx.features.configuration.VectorConfiguration
import im.vector.riotx.features.crypto.keysrequest.KeyRequestHandler
import im.vector.riotx.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.HomeNavigator
import im.vector.riotx.features.home.HomeRoomListObservableStore
import im.vector.riotx.features.home.group.SelectedGroupStore
import im.vector.riotx.features.navigation.Navigator
import im.vector.riotx.features.notifications.NotifiableEventResolver
import im.vector.riotx.features.notifications.NotificationBroadcastReceiver
import im.vector.riotx.features.notifications.NotificationDrawerManager
import im.vector.riotx.features.notifications.PushRuleTriggerListener
import im.vector.riotx.features.rageshake.BugReporter
import im.vector.riotx.features.rageshake.VectorUncaughtExceptionHandler
import javax.inject.Singleton

@Component(modules = [VectorModule::class])
@Singleton
interface VectorComponent {

    fun inject(vectorApplication: NotificationBroadcastReceiver)

    fun inject(vectorApplication: VectorApplication)

    fun matrix(): Matrix

    fun currentSession(): Session

    fun notificationDrawerManager(): NotificationDrawerManager

    fun appContext(): Context

    fun resources(): Resources

    fun vectorConfiguration(): VectorConfiguration

    fun avatarRenderer(): AvatarRenderer

    fun activeSessionHolder(): ActiveSessionHolder

    fun emojiCompatFontProvider(): EmojiCompatFontProvider

    fun navigator(): Navigator

    fun homeNavigator(): HomeNavigator

    fun homeRoomListObservableStore(): HomeRoomListObservableStore

    fun selectedGroupStore(): SelectedGroupStore

    fun incomingVerificationRequestHandler(): IncomingVerificationRequestHandler

    fun incomingKeyRequestHandler(): KeyRequestHandler

    fun authenticator(): Authenticator

    fun bugReporter(): BugReporter

    fun vectorUncaughtExceptionHandler(): VectorUncaughtExceptionHandler

    fun pushRuleTriggerListener(): PushRuleTriggerListener

    fun pusherManager(): PushersManager

    fun notifiableEventResolver(): NotifiableEventResolver

    @Component.Factory
    interface Factory {
        fun create(@BindsInstance context: Context): VectorComponent
    }

}