package im.vector.riotredesign.features.home

import im.vector.riotredesign.features.home.room.detail.timeline.MessageItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.TextItemFactory
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineDateFormatter
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

class HomeModule(private val homeActivity: HomeActivity) : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        factory {
            homeActivity as HomeNavigator
        }

        single {
            TimelineDateFormatter(get())
        }

        single {
            MessageItemFactory(get())
        }

        single {
            TextItemFactory()
        }

        single {
            TimelineEventController(get(), get(), get())
        }

    }.invoke()
}