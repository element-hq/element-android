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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import im.vector.app.core.utils.LiveEvent
import im.vector.app.features.reactions.data.EmojiData
import im.vector.app.features.reactions.data.EmojiDataSource
import kotlinx.coroutines.launch
import javax.inject.Inject

class EmojiChooserViewModel @Inject constructor(private val emojiDataSource: EmojiDataSource) : ViewModel() {

    val emojiData: MutableLiveData<EmojiData> = MutableLiveData()
    val navigateEvent: MutableLiveData<LiveEvent<String>> = MutableLiveData()
    var selectedReaction: String? = null
    var eventId: String? = null

    val currentSection: MutableLiveData<Int> = MutableLiveData()
    val moveToSection: MutableLiveData<Int> = MutableLiveData()

    init {
        loadEmojiData()
    }

    private fun loadEmojiData() {
        viewModelScope.launch {
            val rawData = emojiDataSource.rawData.await()
            emojiData.postValue(rawData)
        }
    }

    fun onReactionSelected(reaction: String) {
        selectedReaction = reaction
        navigateEvent.value = LiveEvent(NAVIGATE_FINISH)
    }

    // Called by the Fragment, when the List is scrolled
    fun setCurrentSection(section: Int) {
        currentSection.value = section
    }

    // Called by the Activity, when a tab item is clicked
    fun scrollToSection(section: Int) {
        moveToSection.value = section
    }

    companion object {
        const val NAVIGATE_FINISH = "NAVIGATE_FINISH"
    }
}
