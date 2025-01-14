/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityBigImageViewerBinding
import javax.inject.Inject

/**
 * Simple Activity to display an avatar in fullscreen.
 */
@AndroidEntryPoint
class BigImageViewerActivity : VectorBaseActivity<ActivityBigImageViewerBinding>() {
    @Inject lateinit var sessionHolder: ActiveSessionHolder

    override fun getBinding() = ActivityBigImageViewerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar(views.bigImageViewerToolbar)
                .setTitle(intent.getStringExtra(EXTRA_TITLE))
                .allowBack()

        val uri = sessionHolder.getSafeActiveSession()
                ?.contentUrlResolver()
                ?.resolveFullSize(intent.getStringExtra(EXTRA_IMAGE_URL))
                ?.toUri()

        if (uri == null) {
            finish()
        } else {
            views.bigImageViewerImageView.showImage(uri)
        }
    }

    companion object {
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_IMAGE_URL = "EXTRA_IMAGE_URL"

        fun newIntent(context: Context, title: String?, imageUrl: String): Intent {
            return Intent(context, BigImageViewerActivity::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_IMAGE_URL, imageUrl)
            }
        }
    }
}
