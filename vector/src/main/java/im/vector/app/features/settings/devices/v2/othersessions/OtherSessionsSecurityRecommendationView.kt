/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
