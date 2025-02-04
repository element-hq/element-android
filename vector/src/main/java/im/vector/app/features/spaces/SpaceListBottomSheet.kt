/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.spaces

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.R
import im.vector.app.core.extensions.replaceChildFragment
import im.vector.app.core.platform.VectorBaseBottomSheetDialogFragment
import im.vector.app.databinding.FragmentSpacesBottomSheetBinding
import im.vector.app.features.analytics.plan.MobileScreen

class SpaceListBottomSheet : VectorBaseBottomSheetDialogFragment<FragmentSpacesBottomSheetBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentSpacesBottomSheetBinding {
        return FragmentSpacesBottomSheetBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SpaceBottomSheet
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            replaceChildFragment(R.id.space_list, SpaceListFragment::class.java)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setPeekHeightAsScreenPercentage(0.75f)
        }
    }

    companion object {
        const val TAG = "SpacesBottomSheet"
    }
}
