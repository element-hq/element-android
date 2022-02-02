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
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.databinding.ViewUrlPreviewBinding
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.view.TimelineMessageLayoutRenderer
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.media.PreviewUrlData

/**
 * A View to display a PreviewUrl and some other state
 */
class PreviewUrlView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr), View.OnClickListener, TimelineMessageLayoutRenderer {

    private lateinit var views: ViewUrlPreviewBinding

    var delegate: TimelineEventController.PreviewUrlCallback? = null

    init {
        setupView()
        radius = resources.getDimensionPixelSize(R.dimen.preview_url_view_corner_radius).toFloat()
        cardElevation = 0f
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

    override fun renderMessageLayout(messageLayout: TimelineMessageLayout) {
        when (messageLayout) {
            is TimelineMessageLayout.Default -> {
                val backgroundColor = ThemeUtils.getColor(context, R.attr.vctr_system)
                setCardBackgroundColor(backgroundColor)
                val guidelineBegin = DimensionConverter(resources).dpToPx(8)
                views.urlPreviewStartGuideline.setGuidelineBegin(guidelineBegin)
            }
            is TimelineMessageLayout.Bubble  -> {
                setCardBackgroundColor(Color.TRANSPARENT)
                rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
                views.urlPreviewStartGuideline.setGuidelineBegin(0)
            }
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
        isVisible = true

        views.urlPreviewTitle.setTextOrHide(previewUrlData.title)
        views.urlPreviewImage.isVisible = imageContentRenderer.render(previewUrlData, views.urlPreviewImage)
        views.urlPreviewDescription.setTextOrHide(previewUrlData.description)
        views.urlPreviewDescription.maxLines = when {
            previewUrlData.mxcUrl != null -> 2
            previewUrlData.title != null  -> 3
            else                          -> 5
        }
        views.urlPreviewSite.setTextOrHide(previewUrlData.siteName.takeIf { it != previewUrlData.title })
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
}
