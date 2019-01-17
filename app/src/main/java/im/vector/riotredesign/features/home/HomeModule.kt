package im.vector.riotredesign.features.home

import im.vector.riotredesign.features.home.group.SelectedGroupHolder
import im.vector.riotredesign.features.home.room.VisibleRoomHolder
import im.vector.riotredesign.features.home.room.detail.timeline.DefaultItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.MessageItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.RoomMemberItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.RoomNameItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.RoomTopicItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineItemFactory
import org.koin.dsl.module.module

class HomeModule {

    val definition = module(override = true) {

        single {
            TimelineDateFormatter(get())
        }

        single {
            MessageItemFactory(get())
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
            DefaultItemFactory()
        }

        single {
            TimelineItemFactory(get(), get(), get(), get(), get())
        }

        single {
            HomeNavigator()
        }

        factory { (roomId: String) ->
            TimelineEventController(roomId, get(), get())
        }

        single {
            SelectedGroupHolder()
        }

        single {
            VisibleRoomHolder()
        }

        single {
            HomePermalinkHandler(get())
        }


    }
}