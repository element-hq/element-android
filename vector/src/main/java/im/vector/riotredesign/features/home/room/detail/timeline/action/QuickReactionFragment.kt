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
package im.vector.riotredesign.features.home.room.detail.timeline.action

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.ButterKnife
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.MvRx
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotredesign.EmojiCompatFontProvider
import im.vector.riotredesign.R
import kotlinx.android.synthetic.main.adapter_item_action_quick_reaction.*
import org.koin.android.ext.android.inject

/**
 * Quick Reaction Fragment (agree / like reactions)
 */
class QuickReactionFragment : BaseMvRxFragment() {

    private val viewModel: QuickReactionViewModel by fragmentViewModel(QuickReactionViewModel::class)

    var interactionListener: InteractionListener? = null

    val fontProvider by inject<EmojiCompatFontProvider>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.adapter_item_action_quick_reaction, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    lateinit var textViews: List<TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViews = listOf(quickReaction0, quickReaction1, quickReaction2, quickReaction3,
                quickReaction4, quickReaction5, quickReaction6, quickReaction7)

        textViews.forEachIndexed { index, textView ->
            textView.typeface = fontProvider.typeface ?: Typeface.DEFAULT
            textView.setOnClickListener {
                viewModel.didSelect(index)
            }
        }

    }

    override fun invalidate() = withState(viewModel) {

        it.quickStates.forEachIndexed { index, qs ->
            textViews[index].text = qs.reaction
            textViews[index].alpha = if (qs.isSelected) 0.2f else 1f
        }

        if (it.result != null) {
            interactionListener?.didQuickReactWith(it.result.reaction, it.result.isSelected, it.eventId)
        }

    }

    interface InteractionListener {
        fun didQuickReactWith(clikedOn: String, add: Boolean, eventId: String)
    }

    companion object {
        fun newInstance(pa: TimelineEventFragmentArgs): QuickReactionFragment {
            val args = Bundle()
            args.putParcelable(MvRx.KEY_ARG, pa)
            val fragment = QuickReactionFragment()
            fragment.arguments = args
            return fragment
        }
    }
}