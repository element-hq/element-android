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
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.core.resources.LocaleProvider
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.features.home.group.SelectedGroupStore
import im.vector.riotredesign.features.home.room.VisibleRoomStore
import im.vector.riotredesign.features.home.room.list.RoomSelectionRepository
import im.vector.riotredesign.features.home.room.list.RoomSummaryComparator
import im.vector.riotredesign.features.notifications.NotificationDrawerManager
import org.koin.dsl.module.module

class AppModule(private val context: Context) {

    val definition = module {

        single {
            LocaleProvider(context.resources)
        }

        single {
            StringProvider(context.resources)
        }

        single {
            ColorProvider(context)
        }

        single {
            context.getSharedPreferences("im.vector.riot", MODE_PRIVATE)
        }

        single {
            RoomSelectionRepository(get())
        }

        single {
            SelectedGroupStore()
        }

        single {
            VisibleRoomStore()
        }

        single {
            RoomSummaryComparator()
        }

        single {
            NotificationDrawerManager(context)
        }

        factory {
            Matrix.getInstance().currentSession!!
        }


    }
}