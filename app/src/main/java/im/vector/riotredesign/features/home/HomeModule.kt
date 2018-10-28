package im.vector.riotredesign.features.home

import org.koin.dsl.context.ModuleDefinition
import org.koin.dsl.module.Module
import org.koin.dsl.module.module

class HomeModule(private val homeActivity: HomeActivity) : Module {

    override fun invoke(): ModuleDefinition = module(override = true) {

        factory {
            homeActivity as HomeNavigator
        }

    }.invoke()
}