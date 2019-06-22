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

package im.vector.riotredesign.core.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.EmojiCompatFontProvider
import im.vector.riotredesign.core.error.ErrorFormatter
import im.vector.riotredesign.core.pushers.PushersManager
import im.vector.riotredesign.core.resources.AppNameProvider
import im.vector.riotredesign.core.resources.LocaleProvider
import im.vector.riotredesign.core.resources.StringArrayProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.configuration.VectorConfiguration
import im.vector.riotredesign.features.crypto.keysrequest.KeyRequestHandler
import im.vector.riotredesign.features.crypto.verification.IncomingVerificationRequestHandler
import im.vector.riotredesign.features.home.HomeRoomListObservableStore
import im.vector.riotredesign.features.home.group.SelectedGroupStore
import im.vector.riotredesign.features.home.room.list.AlphabeticalRoomComparator
import im.vector.riotredesign.features.home.room.list.ChronologicalRoomComparator
import im.vector.riotredesign.features.navigation.DefaultNavigator
import im.vector.riotredesign.features.navigation.Navigator
import im.vector.riotredesign.features.notifications.NotifiableEventResolver
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import im.vector.riotredesign.features.notifications.OutdatedEventDetector
import im.vector.riotredesign.features.notifications.PushRuleTriggerListener
import org.koin.dsl.module.module

class AppModule(private val context: Context) {

    val definition = module {

        single {
            VectorConfiguration(context)
        }

        single {
            LocaleProvider(context.resources)
        }

        single {
            StringProvider(context.resources)
        }

        single {
            StringArrayProvider(context.resources)
        }

        single {
            context.getSharedPreferences("im.vector.riot", MODE_PRIVATE)
        }

        single {
            SelectedGroupStore()
        }

        single {
            HomeRoomListObservableStore()
        }

        single {
            ChronologicalRoomComparator()
        }

        single {
            AlphabeticalRoomComparator()
        }

        single {
            ErrorFormatter(get())
        }

        single {
            PushRuleTriggerListener(get(), get())
        }

        single {
            OutdatedEventDetector()
        }

        single {
            NotificationDrawerManager(context, get())
        }

        single {
            NotifiableEventResolver(get(), get())
        }

        factory {
            Matrix.getInstance().currentSession!!
        }

        single {
            KeyRequestHandler(context, get())
        }

        single {
            IncomingVerificationRequestHandler(context, get())
        }

        factory {
            DefaultNavigator() as Navigator
        }

        single {
            EmojiCompatFontProvider()
        }

        single {
            AppNameProvider(context)
        }

        single {
            PushersManager(get(), get(), get(), get())
        }
    }
}