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

package im.vector.riotredesign.features.home

import im.vector.riotredesign.features.home.group.SelectedGroupStore
import im.vector.riotredesign.features.home.room.VisibleRoomStore
import im.vector.riotredesign.features.home.room.detail.timeline.*
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.list.RoomSummaryComparator
import im.vector.riotredesign.features.home.room.list.RoomSummaryController
import org.koin.dsl.module.module

class HomeModule {

    val definition = module(override = true) {

        single {
            TimelineDateFormatter(get())
        }

        single {
            MessageItemFactory(get(), get(), get())
        }

        single {
            RoomNameItemFactory(get())
        }

        single {
            RoomTopicItemFactory(get())
        }

        single {
            RoomMemberItemFactory(get())
        }

        single {
            CallItemFactory(get())
        }

        single {
            RoomHistoryVisibilityItemFactory(get())
        }

        single {
            DefaultItemFactory()
        }

        single {
            TimelineItemFactory(get(), get(), get(), get(), get(), get(), get())
        }

        single {
            HomeNavigator()
        }

        factory {
            RoomSummaryController(get())
        }

        factory { (roomId: String) ->
            TimelineEventController(roomId, get(), get(), get())
        }

        single {
            TimelineMediaSizeProvider()
        }

        single {
            SelectedGroupStore()
        }

        single {
            VisibleRoomStore()
        }

        single {
            HomePermalinkHandler(get())
        }

        single {
            RoomSummaryComparator()
        }


    }
}