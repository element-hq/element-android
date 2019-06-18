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
package im.vector.riotredesign.features.reactions

import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseFragment


class EmojiChooserFragment : VectorBaseFragment() {

    companion object {
        fun newInstance() = EmojiChooserFragment()
    }

    override fun getLayoutResId() = R.layout.emoji_chooser_fragment

    private lateinit var viewModel: EmojiChooserViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this, viewModelFactory).get(EmojiChooserViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        viewModel.initWithContect(context!!)
        (view as? RecyclerView)?.let {
            it.adapter = viewModel.adapter
            it.adapter?.notifyDataSetChanged()
        }

//        val ds = EmojiDataSource(this.context!!)
    }

}
