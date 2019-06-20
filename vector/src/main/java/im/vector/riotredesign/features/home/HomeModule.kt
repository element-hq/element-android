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
import im.vector.riotredesign.features.home.room.detail.timeline.factory.*
import im.vector.riotredesign.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.riotredesign.features.home.room.detail.timeline.util.MessageInformationDataFactory
import im.vector.riotredesign.features.home.room.list.RoomSummaryController
import im.vector.riotredesign.features.html.EventHtmlRenderer
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module.module

class HomeModule {

    companion object {
        const val HOME_SCOPE = "HOME_SCOPE"
        const val ROOM_DETAIL_SCOPE = "ROOM_DETAIL_SCOPE"
    }

    val definition = module {

        // Activity scope

        scope(HOME_SCOPE) {
            HomeNavigator()
        }

        // Fragment scopes

        factory {
            TimelineDateFormatter(get())
        }

        factory {
            NoticeEventFormatter(get())
        }

        factory { (fragment: Fragment) ->
            val colorProvider = ColorProvider(fragment.requireContext())
            val timelineDateFormatter = get<TimelineDateFormatter>()
            val eventHtmlRenderer = EventHtmlRenderer(GlideApp.with(fragment), fragment.requireContext(), get())
            val noticeEventFormatter = get<NoticeEventFormatter>(parameters = { parametersOf(fragment) })
            val timelineMediaSizeProvider = TimelineMediaSizeProvider()
            val messageInformationDataFactory = MessageInformationDataFactory(timelineDateFormatter, colorProvider)
            val messageItemFactory = MessageItemFactory(colorProvider,
                    timelineMediaSizeProvider,
                    eventHtmlRenderer,
                    get(),
                    messageInformationDataFactory,
                    get())

            val encryptedItemFactory = EncryptedItemFactory(messageInformationDataFactory, colorProvider, get())

            val timelineItemFactory = TimelineItemFactory(
                    messageItemFactory = messageItemFactory,
                    noticeItemFactory = NoticeItemFactory(noticeEventFormatter),
                    defaultItemFactory = DefaultItemFactory(),
                    encryptionItemFactory = EncryptionItemFactory(get()),
                    encryptedItemFactory = encryptedItemFactory
            )
            TimelineEventController(timelineDateFormatter, timelineItemFactory, timelineMediaSizeProvider)
        }

        factory {
            RoomSummaryController(get(), get(), get())
        }

        factory {
            GroupSummaryController()
        }

        scope(ROOM_DETAIL_SCOPE) {
            PermalinkHandler(get(), get())
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