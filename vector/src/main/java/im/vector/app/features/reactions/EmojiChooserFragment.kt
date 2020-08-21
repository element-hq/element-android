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
package im.vector.app.features.reactions

import android.os.Bundle
import android.view.View
import androidx.lifecycle.observe
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.emoji_chooser_fragment.*
import javax.inject.Inject

class EmojiChooserFragment @Inject constructor(
        private val emojiRecyclerAdapter: EmojiRecyclerAdapter
) : VectorBaseFragment(),
        EmojiRecyclerAdapter.InteractionListener,
        ReactionClickListener {

    override fun getLayoutResId() = R.layout.emoji_chooser_fragment

    private lateinit var viewModel: EmojiChooserViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = activityViewModelProvider.get(EmojiChooserViewModel::class.java)

        emojiRecyclerAdapter.reactionClickListener = this
        emojiRecyclerAdapter.interactionListener = this

        emojiRecyclerView.adapter = emojiRecyclerAdapter

        viewModel.moveToSection.observe(viewLifecycleOwner) { section ->
            emojiRecyclerAdapter.scrollToSection(section)
        }
    }

    override fun firstVisibleSectionChange(section: Int) {
        viewModel.setCurrentSection(section)
    }

    override fun onReactionSelected(reaction: String) {
        viewModel.onReactionSelected(reaction)
    }

    override fun onDestroyView() {
        emojiRecyclerView.cleanup()
        emojiRecyclerAdapter.reactionClickListener = null
        emojiRecyclerAdapter.interactionListener = null
        super.onDestroyView()
    }
}
