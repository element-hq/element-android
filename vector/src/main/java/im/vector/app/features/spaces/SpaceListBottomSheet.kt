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

package im.vector.app.features.spaces

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import im.vector.app.R
import im.vector.app.core.extensions.replaceChildFragment
import im.vector.app.databinding.FragmentSpacesBottomSheetBinding

class SpaceListBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSpacesBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSpacesBottomSheetBinding.inflate(inflater, container, false)
        if (savedInstanceState == null) {
            replaceChildFragment(R.id.space_list, SpaceListFragment::class.java)
        }
        return binding.root
    }

    companion object {
        const val TAG = "SpacesBottomSheet"
    }
}
