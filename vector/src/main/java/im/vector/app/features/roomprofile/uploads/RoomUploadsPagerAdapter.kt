/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
