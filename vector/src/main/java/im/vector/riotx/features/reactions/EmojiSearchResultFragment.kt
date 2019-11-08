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
package im.vector.riotx.features.reactions

import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.LiveEvent
import javax.inject.Inject

class EmojiSearchResultFragment @Inject constructor(
        private val epoxyController: EmojiSearchResultController
) : VectorBaseFragment() {

    override fun getLayoutResId(): Int = R.layout.fragment_generic_recycler_epoxy

    val viewModel: EmojiSearchResultViewModel by activityViewModel()

    var sharedViewModel: EmojiChooserViewModel? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedViewModel = activityViewModelProvider.get(EmojiChooserViewModel::class.java)

        epoxyController.listener = object : ReactionClickListener {
            override fun onReactionSelected(reaction: String) {
                sharedViewModel?.selectedReaction = reaction
                sharedViewModel?.navigateEvent?.value = LiveEvent(EmojiChooserViewModel.NAVIGATE_FINISH)
            }
        }

        val lmgr = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val epoxyRecyclerView = view as? EpoxyRecyclerView ?: return
        epoxyRecyclerView.layoutManager = lmgr
        val dividerItemDecoration = DividerItemDecoration(epoxyRecyclerView.context, lmgr.orientation)
        epoxyRecyclerView.addItemDecoration(dividerItemDecoration)
        epoxyRecyclerView.setController(epoxyController)
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
    }
}
