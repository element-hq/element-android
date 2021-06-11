/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.attachments.preview

import android.content.Context
import android.content.Intent
import com.google.android.material.appbar.MaterialToolbar
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.themes.ActivityOtherThemes
import org.matrix.android.sdk.api.session.content.ContentAttachmentData

class AttachmentsPreviewActivity : VectorBaseActivity<ActivitySimpleBinding>(), ToolbarConfigurable {

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
            return intent.getParcelableArrayListExtra<ContentAttachmentData>(ATTACHMENTS_PREVIEW_RESULT).orEmpty()
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
            val fragmentArgs: AttachmentsPreviewArgs = intent?.extras?.getParcelable(EXTRA_FRAGMENT_ARGS) ?: return
            addFragment(R.id.simpleFragmentContainer, AttachmentsPreviewFragment::class.java, fragmentArgs)
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

    override fun configure(toolbar: MaterialToolbar) {
        configureToolbar(toolbar)
    }
}
