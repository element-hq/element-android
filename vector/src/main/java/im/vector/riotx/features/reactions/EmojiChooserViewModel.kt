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

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import im.vector.riotx.core.utils.LiveEvent
import javax.inject.Inject

class EmojiChooserViewModel @Inject constructor() : ViewModel() {

    var adapter: EmojiRecyclerAdapter? = null
    val emojiSourceLiveData: MutableLiveData<EmojiDataSource> = MutableLiveData()

    val navigateEvent: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    var selectedReaction: String? = null
    var eventId: String? = null

    val currentSection: MutableLiveData<Int> = MutableLiveData()

    var reactionClickListener = object : ReactionClickListener {
        override fun onReactionSelected(reaction: String) {
            selectedReaction = reaction
            navigateEvent.value = LiveEvent(NAVIGATE_FINISH)
        }
    }

    fun initWithContext(context: Context) {
        // TODO load async
        val emojiDataSource = EmojiDataSource(context)
        emojiSourceLiveData.value = emojiDataSource
        adapter = EmojiRecyclerAdapter(emojiDataSource, reactionClickListener)
        adapter?.interactionListener = object : EmojiRecyclerAdapter.InteractionListener {
            override fun firstVisibleSectionChange(section: Int) {
                currentSection.value = section
            }
        }
    }

    fun scrollToSection(sectionIndex: Int) {
        adapter?.scrollToSection(sectionIndex)
    }

    companion object {
        const val NAVIGATE_FINISH = "NAVIGATE_FINISH"
    }
}
