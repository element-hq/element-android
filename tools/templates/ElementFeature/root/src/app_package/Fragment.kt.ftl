package ${escapeKotlinIdentifiers(packageName)}

import android.os.Bundle
<#if createFragmentArgs>
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.airbnb.mvrx.args
</#if>
import android.view.View
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseFragment
import javax.inject.Inject

<#if createFragmentArgs>
@Parcelize
data class ${fragmentArgsClass}() : Parcelable
</#if>

//TODO: add this fragment into FragmentModule
class ${fragmentClass} @Inject constructor(
        private val viewModelFactory: ${viewModelClass}.Factory
) : VectorBaseFragment(), ${viewModelClass}.Factory by viewModelFactory {

	<#if createFragmentArgs>
		private val fragmentArgs: ${fragmentArgsClass} by args()
	</#if>
    private val viewModel: ${viewModelClass} by fragmentViewModel()

    override fun getLayoutResId() = R.layout.${fragmentLayout}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
		// Initialize your view, subscribe to viewModel... 
    }

    override fun onDestroyView() {
		// Clear your view, unsubscribe...
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        //TODO
    }

}
