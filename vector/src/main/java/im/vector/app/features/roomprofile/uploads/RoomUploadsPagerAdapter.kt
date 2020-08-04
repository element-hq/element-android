/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.roomprofile.uploads

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import im.vector.app.features.roomprofile.uploads.files.RoomUploadsFilesFragment
import im.vector.app.features.roomprofile.uploads.media.RoomUploadsMediaFragment

class RoomUploadsPagerAdapter(
        private val fragment: Fragment
) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            fragment.childFragmentManager.fragmentFactory.instantiate(fragment.requireContext().classLoader, RoomUploadsMediaFragment::class.java.name)
        } else {
            fragment.childFragmentManager.fragmentFactory.instantiate(fragment.requireContext().classLoader, RoomUploadsFilesFragment::class.java.name)
        }
    }
}
