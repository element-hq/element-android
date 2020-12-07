/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.detail.timeline.url

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import butterknife.BindView
import butterknife.ButterKnife
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.media.ImageContentRenderer
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.media.PreviewUrlData

/**
 * A View to display a PreviewUrl and some other state
 */
class PreviewUrlView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), View.OnClickListener {

    @BindView(R.id.url_preview_title)
    lateinit var titleView: TextView

    @BindView(R.id.url_preview_image)
    lateinit var imageView: ImageView

    @BindView(R.id.url_preview_description)
    lateinit var descriptionView: TextView

    @BindView(R.id.url_preview_site)
    lateinit var siteView: TextView

    @BindView(R.id.url_preview_close)
    lateinit var closeView: View

    var delegate: TimelineEventController.PreviewUrlCallback? = null

    init {
        setupView()
    }

    private var state: PreviewUrlUiState = PreviewUrlUiState.Unknown

    /**
     * This methods is responsible for rendering the view according to the newState
     *
     * @param newState the newState representing the view
     */
    fun render(newState: PreviewUrlUiState,
               imageContentRenderer: ImageContentRenderer,
               force: Boolean = false) {
        if (newState == state && !force) {
            return
        }

        state = newState

        hideAll()
        when (newState) {
            PreviewUrlUiState.Unknown,
            PreviewUrlUiState.NoUrl    -> renderHidden()
            PreviewUrlUiState.Loading  -> renderLoading()
            is PreviewUrlUiState.Error -> renderHidden()
            is PreviewUrlUiState.Data  -> renderData(newState.previewUrlData, imageContentRenderer)
        }
    }

    override fun onClick(v: View?) {
        when (val finalState = state) {
            is PreviewUrlUiState.Data -> delegate?.onPreviewUrlClicked(finalState.url)
            else                      -> Unit
        }
    }

    private fun onCloseClick() {
        when (val finalState = state) {
            is PreviewUrlUiState.Data -> delegate?.onPreviewUrlCloseClicked(finalState.eventId, finalState.url)
            else                      -> Unit
        }
    }

    // PRIVATE METHODS ****************************************************************************************************************************************

    private fun setupView() {
        inflate(context, R.layout.url_preview, this)
        ButterKnife.bind(this)

        setOnClickListener(this)
        closeView.setOnClickListener { onCloseClick() }
    }

    private fun renderHidden() {
        isVisible = false
    }

    private fun renderLoading() {
        // Just hide for the moment
        isVisible = false
    }

    private fun renderData(previewUrlData: PreviewUrlData, imageContentRenderer: ImageContentRenderer) {
        isVisible = true
        titleView.setTextOrHide(previewUrlData.title)
        imageView.isVisible = previewUrlData.mxcUrl?.let { imageContentRenderer.render(it, imageView) }.orFalse()
        descriptionView.setTextOrHide(previewUrlData.description)
        siteView.setTextOrHide(previewUrlData.siteName.takeIf { it != previewUrlData.title })
    }

    /**
     * Hide all views that are not visible in all state
     */
    private fun hideAll() {
        titleView.isVisible = false
        imageView.isVisible = false
        descriptionView.isVisible = false
        siteView.isVisible = false
    }
}
