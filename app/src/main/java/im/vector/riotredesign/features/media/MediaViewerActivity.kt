/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.github.piasy.biv.indicator.progresspie.ProgressPieIndicator
import com.github.piasy.biv.view.GlideImageViewFactory
import im.vector.riotredesign.core.platform.RiotActivity
import kotlinx.android.synthetic.main.activity_media_viewer.*


class MediaViewerActivity : RiotActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(im.vector.riotredesign.R.layout.activity_media_viewer)
        val mediaData = intent.getParcelableExtra<MediaContentRenderer.Data>(EXTRA_MEDIA_DATA)
        if (mediaData.url.isNullOrEmpty()) {
            finish()
        } else {
            configureToolbar(mediaViewerToolbar, mediaData)
            mediaViewerImageView.setImageViewFactory(GlideImageViewFactory())
            mediaViewerImageView.setProgressIndicator(ProgressPieIndicator())
            MediaContentRenderer.render(mediaData, mediaViewerImageView)
        }
    }

    private fun configureToolbar(toolbar: Toolbar, mediaData: MediaContentRenderer.Data) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = mediaData.filename
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return true
    }


    companion object {

        private const val EXTRA_MEDIA_DATA = "EXTRA_MEDIA_DATA"

        fun newIntent(context: Context, mediaData: MediaContentRenderer.Data): Intent {
            return Intent(context, MediaViewerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_DATA, mediaData)
            }
        }
    }


}