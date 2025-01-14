/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentLoadingBinding

@AndroidEntryPoint
class LoadingFragment : VectorBaseFragment<FragmentLoadingBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoadingBinding {
        return FragmentLoadingBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val background = views.animatedLogoImageView.background
        if (background is AnimationDrawable) {
            background.start()
        }
    }
}
