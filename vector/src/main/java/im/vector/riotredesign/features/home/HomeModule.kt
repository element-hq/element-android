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

import androidx.fragment.app.Fragment
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.resources.ColorProvider
import im.vector.riotredesign.features.autocomplete.command.AutocompleteCommandController
import im.vector.riotredesign.features.autocomplete.command.AutocompleteCommandPresenter
import im.vector.riotredesign.features.autocomplete.user.AutocompleteUserController
import im.vector.riotredesign.features.autocomplete.user.AutocompleteUserPresenter
import im.vector.riotredesign.features.home.group.GroupSummaryController
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.factory.CallItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.DefaultItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.MessageItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.RoomHistoryVisibilityItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.RoomMemberItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.RoomNameItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.RoomTopicItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.factory.TimelineItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.list.RoomSummaryController
import im.vector.riotredesign.features.html.EventHtmlRenderer
import org.koin.dsl.module.module

class HomeModule {

    companion object {
        const val HOME_SCOPE = "HOME_SCOPE"
        const val ROOM_DETAIL_SCOPE = "ROOM_DETAIL_SCOPE"
        const val ROOM_LIST_SCOPE = "ROOM_LIST_SCOPE"
        const val GROUP_LIST_SCOPE = "GROUP_LIST_SCOPE"
    }

    val definition = module {

        // Activity scope

        scope(HOME_SCOPE) {
            HomeNavigator()
        }

        scope(HOME_SCOPE) {
            HomeRoomListObservableStore()
        }

        scope(HOME_SCOPE) {
            HomePermalinkHandler(get())
        }

        // Fragment scopes

        factory { (fragment: Fragment) ->
            val eventHtmlRenderer = EventHtmlRenderer(GlideApp.with(fragment), fragment.requireContext(), get())
            val timelineDateFormatter = TimelineDateFormatter(get())
            val timelineMediaSizeProvider = TimelineMediaSizeProvider()
            val colorProvider = ColorProvider(fragment.requireContext())
            val messageItemFactory = MessageItemFactory(colorProvider, timelineMediaSizeProvider, timelineDateFormatter, eventHtmlRenderer)

            val timelineItemFactory = TimelineItemFactory(messageItemFactory = messageItemFactory,
                                                          roomNameItemFactory = RoomNameItemFactory(get()),
                                                          roomTopicItemFactory = RoomTopicItemFactory(get()),
                                                          roomMemberItemFactory = RoomMemberItemFactory(get()),
                                                          roomHistoryVisibilityItemFactory = RoomHistoryVisibilityItemFactory(get()),
                                                          callItemFactory = CallItemFactory(get()),
                                                          defaultItemFactory = DefaultItemFactory()
            )
            TimelineEventController(timelineDateFormatter, timelineItemFactory, timelineMediaSizeProvider)
        }

        factory {
            RoomSummaryController(get())
        }

        factory {
            GroupSummaryController()
        }

        scope(ROOM_DETAIL_SCOPE) { (fragment: Fragment) ->
            val commandController = AutocompleteCommandController(get())
            AutocompleteCommandPresenter(fragment.requireContext(), commandController)
        }

        scope(ROOM_DETAIL_SCOPE) { (fragment: Fragment) ->
            val userController = AutocompleteUserController()
            AutocompleteUserPresenter(fragment.requireContext(), userController)
        }

    }
}