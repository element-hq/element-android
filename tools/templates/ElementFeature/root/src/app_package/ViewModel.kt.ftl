package ${escapeKotlinIdentifiers(packageName)}

import com.airbnb.mvrx.ActivityViewModelContext
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import im.vector.app.core.platform.VectorViewModel

<#if createViewEvents>
<#else>
import im.vector.app.core.platform.EmptyViewEvents
</#if>

class ${viewModelClass} @AssistedInject constructor(@Assisted initialState: ${viewStateClass})
    <#if createViewEvents>
    : VectorViewModel<${viewStateClass}, ${actionClass}, ${viewEventsClass}>(initialState) {
    <#else>
    : VectorViewModel<${viewStateClass}, ${actionClass}, EmptyViewEvents>(initialState) {
    </#if>

    @AssistedFactory
    interface Factory {
        fun create(initialState: ${viewStateClass}): ${viewModelClass}
    }

    companion object : MavericksViewModelFactory<${viewModelClass}, ${viewStateClass}> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ${viewStateClass}): ${viewModelClass}? {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state) ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    override fun handle(action: ${actionClass}) {
        when (action) {

        }
    }
}
