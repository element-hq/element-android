/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentSpaceListModalBinding
import im.vector.app.features.spaces.SpaceListViewModel
import im.vector.app.features.spaces.SpaceSummaryController
import javax.inject.Inject

@AndroidEntryPoint
class SpaceListModalFragment : VectorBaseFragment<FragmentSpaceListModalBinding>() {

    @Inject
    lateinit var spaceModalController: SpaceSummaryController

    @Inject
    lateinit var avatarRenderer: AvatarRenderer

    private lateinit var binding: FragmentSpaceListModalBinding

    private val viewModel: SpaceListViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSpaceListModalBinding {
        binding = FragmentSpaceListModalBinding.inflate(inflater)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.roomList.layoutManager = LinearLayoutManager(context)
        binding.roomList.adapter = SpaceListAdapter(mutableListOf(), avatarRenderer)
    }

    override fun invalidate() {
        withState(viewModel) { state ->
            state.rootSpacesOrdered?.let {
                (binding.roomList.adapter as SpaceListAdapter).replaceList(it)
            }
        }
    }
}
