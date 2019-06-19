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
import android.widget.LinearLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_settings_pushgateways.*

class PushGatewaysFragment : VectorBaseFragment() {

    override fun getLayoutResId(): Int = R.layout.fragment_settings_pushgateways

    private val viewModel: PushGatewaysViewModel by fragmentViewModel(PushGatewaysViewModel::class)

    private val epoxyController by lazy { PushGateWayController() }

    override fun onResume() {
        super.onResume()
        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_push_gateways)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val lmgr = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        epoxyRecyclerView.layoutManager = lmgr
        val dividerItemDecoration = DividerItemDecoration(epoxyRecyclerView.context,
                lmgr.orientation)
        epoxyRecyclerView.addItemDecoration(dividerItemDecoration)
        epoxyRecyclerView.adapter = epoxyController.adapter
    }

    override fun invalidate() = withState(viewModel) {
        epoxyController.setData(it)
    }

    class PushGateWayController : TypedEpoxyController<PushGatewayViewState>() {
        override fun buildModels(data: PushGatewayViewState?) {
            val pushers = data?.pushgateways?.invoke() ?: return
            pushers.forEach {
                pushGatewayItem {
                    id("${it.pushKey}_${it.appId}")
                    pusher(it)
                }
            }
        }

    }
}
