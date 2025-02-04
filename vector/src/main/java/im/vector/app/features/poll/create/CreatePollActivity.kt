/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.poll.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.lib.core.utils.compat.getParcelableCompat

@AndroidEntryPoint
class CreatePollActivity : SimpleFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views.toolbar.visibility = View.GONE

        val createPollArgs: CreatePollArgs? = intent?.extras?.getParcelableCompat(EXTRA_CREATE_POLL_ARGS)

        if (isFirstCreation()) {
            addFragment(
                    views.container,
                    CreatePollFragment::class.java,
                    createPollArgs
            )
        }
    }

    companion object {

        private const val EXTRA_CREATE_POLL_ARGS = "EXTRA_CREATE_POLL_ARGS"

        fun getIntent(context: Context, createPollArgs: CreatePollArgs): Intent {
            return Intent(context, CreatePollActivity::class.java).apply {
                putExtra(EXTRA_CREATE_POLL_ARGS, createPollArgs)
            }
        }
    }
}
