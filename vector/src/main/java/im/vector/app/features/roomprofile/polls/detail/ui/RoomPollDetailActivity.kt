/*
 * Copyright 2023, 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.polls.detail.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.lib.core.utils.compat.getParcelableExtraCompat

/**
 * Display the details of a given poll.
 */
@AndroidEntryPoint
class RoomPollDetailActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            addFragment(
                    container = views.simpleFragmentContainer,
                    fragmentClass = RoomPollDetailFragment::class.java,
                    params = intent.getParcelableExtraCompat(Mavericks.KEY_ARG)
            )
        }
    }

    companion object {
        fun newIntent(context: Context, pollId: String, roomId: String, isEnded: Boolean): Intent {
            return Intent(context, RoomPollDetailActivity::class.java).apply {
                val args = RoomPollDetailArgs(
                        pollId = pollId,
                        roomId = roomId,
                        isEnded = isEnded,
                )
                putExtra(Mavericks.KEY_ARG, args)
            }
        }
    }
}
