/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.attachments.preview

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.themes.ActivityOtherThemes
import im.vector.lib.core.utils.compat.getParcelableArrayListExtraCompat
import im.vector.lib.core.utils.compat.getParcelableCompat
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

@AndroidEntryPoint
class AttachmentsPreviewActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    companion object {
        private const val EXTRA_FRAGMENT_ARGS = "EXTRA_FRAGMENT_ARGS"
        private const val ATTACHMENTS_PREVIEW_RESULT = "ATTACHMENTS_PREVIEW_RESULT"
        private const val KEEP_ORIGINAL_IMAGES_SIZE = "KEEP_ORIGINAL_IMAGES_SIZE"

        fun newIntent(context: Context, args: AttachmentsPreviewArgs): Intent {
            return Intent(context, AttachmentsPreviewActivity::class.java).apply {
                putExtra(EXTRA_FRAGMENT_ARGS, args)
            }
        }

        fun getOutput(intent: Intent): List<ContentAttachmentData> {
            return intent.getParcelableArrayListExtraCompat<ContentAttachmentData>(ATTACHMENTS_PREVIEW_RESULT).orEmpty()
        }

        fun getKeepOriginalSize(intent: Intent): Boolean {
            return intent.getBooleanExtra(KEEP_ORIGINAL_IMAGES_SIZE, false)
        }
    }

    override fun getOtherThemes() = ActivityOtherThemes.AttachmentsPreview

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        if (isFirstCreation()) {
            val fragmentArgs: AttachmentsPreviewArgs = intent?.extras?.getParcelableCompat(EXTRA_FRAGMENT_ARGS) ?: return
            addFragment(views.simpleFragmentContainer, AttachmentsPreviewFragment::class.java, fragmentArgs)
        }
    }

    fun setResultAndFinish(data: List<ContentAttachmentData>, keepOriginalImageSize: Boolean) {
        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(ATTACHMENTS_PREVIEW_RESULT, ArrayList(data))
            putExtra(KEEP_ORIGINAL_IMAGES_SIZE, keepOriginalImageSize)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
