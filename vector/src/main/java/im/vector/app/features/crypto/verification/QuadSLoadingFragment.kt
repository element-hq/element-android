/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification

import android.view.LayoutInflater
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentProgressBinding

@AndroidEntryPoint
class QuadSLoadingFragment :
        VectorBaseFragment<FragmentProgressBinding>() {
    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentProgressBinding {
        return FragmentProgressBinding.inflate(inflater, container, false)
    }
}
