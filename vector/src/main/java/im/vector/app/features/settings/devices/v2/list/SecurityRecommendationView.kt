/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.R
import im.vector.app.databinding.ViewSecurityRecommendationBinding

class SecurityRecommendationView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ViewSecurityRecommendationBinding

    init {
        inflate(context, R.layout.view_security_recommendation, this)
        views = ViewSecurityRecommendationBinding.bind(this)

        context.obtainStyledAttributes(
                attrs,
                R.styleable.SecurityRecommendationView,
                0,
                0
        ).use {
            setTitle(it)
            setDescription(it)
            setImage(it)
        }
    }

    private fun setTitle(typedArray: TypedArray) {
        val title = typedArray.getString(R.styleable.SecurityRecommendationView_recommendationTitle)
        views.recommendationTitleTextView.text = title
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(R.styleable.SecurityRecommendationView_recommendationDescription)
        setDescription(description)
    }

    private fun setImage(typedArray: TypedArray) {
        val imageResource = typedArray.getResourceId(R.styleable.SecurityRecommendationView_recommendationImageResource, 0)
        val backgroundTint = typedArray.getColor(R.styleable.SecurityRecommendationView_recommendationImageBackgroundTint, 0)
        views.recommendationShieldImageView.setImageResource(imageResource)
        views.recommendationShieldImageView.backgroundTintList = ColorStateList.valueOf(backgroundTint)
    }

    private fun setDescription(description: String?) {
        views.recommendationDescriptionTextView.text = description
    }

    private fun setCount(sessionsCount: Int) {
        views.recommendationViewAllButton.text = context.getString(R.string.device_manager_other_sessions_view_all, sessionsCount)
    }

    fun render(viewState: SecurityRecommendationViewState) {
        setDescription(viewState.description)
        setCount(viewState.sessionsCount)
    }
}
