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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.databinding.ViewUrlPreviewBinding
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

    private lateinit var views: ViewUrlPreviewBinding

    var footerWidth: Int = 0
    var footerHeight: Int = 0

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

    private fun onImageClick() {
        when (val finalState = state) {
            is PreviewUrlUiState.Data -> {
                delegate?.onPreviewUrlImageClicked(
                        sharedView = views.urlPreviewImage,
                        mxcUrl = finalState.previewUrlData.mxcUrl,
                        title = finalState.previewUrlData.title
                )
            }
            else                      -> Unit
        }
    }

    private fun onCloseClick() {
        when (val finalState = state) {
            is PreviewUrlUiState.Data -> delegate?.onPreviewUrlCloseClicked(finalState.eventId, finalState.url)
            else                      -> Unit
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Get max available width - we're faking "wrap_content" here to use all available space,
        // since match_parent doesn't work here as our parent does wrap_content as well
        /*
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthLimit = if (widthMode == MeasureSpec.AT_MOST) {
            widthSize.toFloat()
        } else {
            Float.MAX_VALUE
        }
         */
        //setMeasuredDimension(round(widthLimit).toInt(), measuredHeight)

        // We extract the size from an AT_MOST spec, which is the width limit, but change the mode to EXACTLY
        val newWidthSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)

        // We measure our children based on the now fixed width
        super.onMeasure(newWidthSpec, heightMeasureSpec)

    }

    // PRIVATE METHODS ****************************************************************************************************************************************

    private fun setupView() {
        inflate(context, R.layout.view_url_preview, this)
        views = ViewUrlPreviewBinding.bind(this)

        setOnClickListener(this)
        views.urlPreviewImage.setOnClickListener { onImageClick() }
        views.urlPreviewClose.setOnClickListener { onCloseClick() }
    }

    private fun renderHidden() {
        isVisible = false
    }

    private fun renderLoading() {
        // Just hide for the moment
        isVisible = false
    }

    private fun renderData(previewUrlData: PreviewUrlData, imageContentRenderer: ImageContentRenderer) {
        // Set footer sizes before setText() calls so they are available onMeasure
        val siteText = previewUrlData.siteName.takeIf { it != previewUrlData.title }
        updateFooterSpaceInternal(siteText)

        isVisible = true
        views.urlPreviewTitle.setTextOrHide(previewUrlData.title)
        views.urlPreviewImage.isVisible = previewUrlData.mxcUrl?.let { imageContentRenderer.render(it, views.urlPreviewImage, hideOnFail = true) }.orFalse()
        views.urlPreviewDescription.setTextOrHide(previewUrlData.description)
        views.urlPreviewSite.setTextOrHide(siteText)
    }

    /**
     * Hide all views that are not visible in all state
     */
    private fun hideAll() {
        views.urlPreviewTitle.isVisible = false
        views.urlPreviewImage.isVisible = false
        views.urlPreviewDescription.isVisible = false
        views.urlPreviewSite.isVisible = false
    }

    public fun updateFooterSpace() {
        val siteText = views.urlPreviewSite.text as String?
        updateFooterSpaceInternal(siteText)
        requestLayout()
    }

    private fun updateFooterSpaceInternal(siteText: String?) {
        val siteViewHidden = siteText == null || siteText.isBlank() // identical to setTextOrHide
        if (siteViewHidden) {
            views.urlPreviewDescription.footerWidth = footerWidth
            views.urlPreviewDescription.footerHeight = footerHeight
        } else {
            views.urlPreviewSite.footerWidth = footerWidth
            views.urlPreviewSite.footerHeight = footerHeight
            views.urlPreviewDescription.footerWidth = 0
            views.urlPreviewDescription.footerHeight = 0
        }
    }
}
