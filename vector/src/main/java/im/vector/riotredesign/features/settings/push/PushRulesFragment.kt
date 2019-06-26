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
package im.vector.riotredesign.features.settings.push

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.core.platform.VectorBaseFragment
import im.vector.riotredesign.core.resources.StringProvider
import im.vector.riotredesign.core.ui.list.genericFooterItem
import kotlinx.android.synthetic.main.fragment_generic_recycler_epoxy.*

// Referenced in vector_settings_notifications.xml
class PushRulesFragment : VectorBaseFragment() {

    override fun getLayoutResId(): Int = R.layout.fragment_generic_recycler_epoxy

    private val viewModel: PushRulesViewModel by fragmentViewModel(PushRulesViewModel::class)

    private val epoxyController by lazy { PushRulesController(StringProvider(requireContext().resources)) }


    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_push_rules)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val lmgr = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        epoxyRecyclerView.layoutManager = lmgr
        val dividerItemDecoration = DividerItemDecoration(epoxyRecyclerView.context,
                lmgr.orientation)
        epoxyRecyclerView.addItemDecoration(dividerItemDecoration)
        epoxyRecyclerView.setController(epoxyController)
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