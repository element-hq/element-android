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
package im.vector.app.features.settings.push

import android.os.Bundle
import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericFooterItem
import kotlinx.android.synthetic.main.fragment_generic_recycler.*

// Referenced in vector_settings_notifications.xml
class PushRulesFragment : VectorBaseFragment() {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    private val viewModel: PushRulesViewModel by fragmentViewModel(PushRulesViewModel::class)

    private val epoxyController by lazy { PushRulesController(StringProvider(requireContext().resources)) }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_push_rules)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.configureWith(epoxyController, showDivider = true)
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }

    class PushRulesController(private val stringProvider: StringProvider) : TypedEpoxyController<PushRulesViewState>() {

        override fun buildModels(data: PushRulesViewState?) {
            data?.let {
                it.rules.forEach {
                    pushRuleItem {
                        id(it.ruleId)
                        pushRule(it)
                    }
                }
            } ?: run {
                genericFooterItem {
                    id("footer")
                    text(stringProvider.getString(R.string.settings_push_rules_no_rules))
                }
            }
        }
    }
}
