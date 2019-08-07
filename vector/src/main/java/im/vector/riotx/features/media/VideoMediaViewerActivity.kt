/*
 * Copyright 2019 New Vector Ltd
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
 */

package im.vector.riotx.features.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import kotlinx.android.synthetic.main.activity_video_media_viewer.*
import javax.inject.Inject


class VideoMediaViewerActivity : VectorBaseActivity() {

    @Inject lateinit var imageContentRenderer: ImageContentRenderer
    @Inject lateinit var videoContentRenderer: VideoContentRenderer
    @Inject lateinit var mediaDownloadHelper: MediaDownloadHelper
    lateinit var mediaData: VideoContentRenderer.Data

    override fun getMenuRes() = R.menu.video_media_viewer

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_media_viewer)
        mediaData = intent.getParcelableExtra(EXTRA_MEDIA_DATA)
        if (mediaData.url.isNullOrEmpty()) {
            finish()
            return
        }
        configureToolbar(videoMediaViewerToolbar, mediaData)
        imageContentRenderer.render(mediaData.thumbnailMediaData, ImageContentRenderer.Mode.FULL_SIZE, videoMediaViewerThumbnailView)
        videoContentRenderer.render(mediaData, videoMediaViewerThumbnailView, videoMediaViewerLoading, videoMediaViewerVideoView, videoMediaViewerErrorView)
    }

    private fun configureToolbar(toolbar: Toolbar, mediaData: VideoContentRenderer.Data) {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = mediaData.filename
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val downloadItem = menu.findItem(R.id.download_video)
        downloadItem.isVisible = !mediaData.isLocalFile()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download_video -> mediaDownloadHelper.checkPermissionAndDownload(
                    mediaData.eventId,
                    mediaData.filename,
                    mediaData.url,
                    mediaData.elementToDecrypt
            )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mediaDownloadHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {

        private const val EXTRA_MEDIA_DATA = "EXTRA_MEDIA_DATA"

        fun newIntent(context: Context, mediaData: VideoContentRenderer.Data): Intent {
            return Intent(context, VideoMediaViewerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA_DATA, mediaData)
            }
        }
    }


}