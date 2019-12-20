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

package im.vector.riotx.features.roomprofile.members

import android.os.Bundle
import android.view.View
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.features.roomprofile.RoomProfileArgs
import javax.inject.Inject


class RoomMemberListFragment @Inject constructor(
        val viewModelFactory: RoomMemberListViewModel.Factory
) : VectorBaseFragment() {

    private val viewModel: RoomMemberListViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getLayoutResId() = R.layout.fragment_room_member_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize your view, subscribe to viewModel...
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear your view, unsubscribe...
    }

    override fun invalidate() = withState(viewModel) { _ ->

    }

}
