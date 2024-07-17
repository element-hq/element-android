/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.othersessions

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.ViewOtherSessionSecurityRecommendationBinding
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class OtherSessionsSecurityRecommendationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ViewOtherSessionSecurityRecommendationBinding
    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        inflate(context, R.layout.view_other_session_security_recommendation, this)
        views = ViewOtherSessionSecurityRecommendationBinding.bind(this)

        context.obtainStyledAttributes(
                attrs,
                im.vector.lib.ui.styles.R.styleable.OtherSessionsSecurityRecommendationView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
            setImage(it)
        }
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(im.vector.lib.ui.styles.R.styleable.OtherSessionsSecurityRecommendationView_otherSessionsRecommendationTitle)
        setTitle(title)
    }

    private fun setTitle(title: String?) {
        views.recommendationTitleTextView.text = title
    }

    private fun setDescription(typedArray: TypedArray) {
        val description =
                typedArray.getString(im.vector.lib.ui.styles.R.styleable.OtherSessionsSecurityRecommendationView_otherSessionsRecommendationDescription)
        setDescription(description)
    }

    private fun setImage(typedArray: TypedArray) {
        val imageResource = typedArray.getResourceId(
                im.vector.lib.ui.styles.R.styleable.OtherSessionsSecurityRecommendationView_otherSessionsRecommendationImageResource, 0
        )
        val backgroundTint = typedArray.getColor(
                im.vector.lib.ui.styles.R.styleable.OtherSessionsSecurityRecommendationView_otherSessionsRecommendationImageBackgroundTint, 0
        )
        setImageResource(imageResource)
        setImageBackgroundTint(backgroundTint)
    }

    private fun setImageResource(resourceId: Int) {
        views.recommendationShieldImageView.setImageResource(resourceId)
    }

    private fun setImageBackgroundTint(backgroundTintColor: Int) {
        views.recommendationShieldImageView.backgroundTintList = ColorStateList.valueOf(backgroundTintColor)
    }

    private fun setDescription(description: String?) {
        val learnMore = context.getString(CommonStrings.action_learn_more)
        val formattedDescription = buildString {
            append(description)
            append(" ")
            append(learnMore)
        }

        views.recommendationDescriptionTextView.setTextWithColoredPart(
                fullText = formattedDescription,
                coloredPart = learnMore,
                underline = false
        ) {
            onLearnMoreClickListener?.invoke()
        }
    }

    fun render(viewState: OtherSessionsSecurityRecommendationViewState) {
        setTitle(viewState.title)
        setDescription(viewState.description)
        setImageResource(viewState.imageResourceId)
        setImageBackgroundTint(viewState.imageTintColorResourceId)
    }
}
