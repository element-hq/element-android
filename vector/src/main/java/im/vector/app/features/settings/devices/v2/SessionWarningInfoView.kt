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

package im.vector.app.features.settings.devices.v2

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import im.vector.app.R
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.databinding.ViewSessionWarningInfoBinding

class SessionWarningInfoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewSessionWarningInfoBinding.inflate(
            LayoutInflater.from(context),
            this
    )

    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        context.obtainStyledAttributes(
                attrs,
                R.styleable.SessionWarningInfoView,
                0,
                0
        ).use {
            setDescription(it)
        }
    }

    private fun setDescription(typedArray: TypedArray) {
        val description = typedArray.getString(R.styleable.SessionWarningInfoView_sessionsWarningInfoDescription)
        val hasLearnMore = typedArray.getBoolean(R.styleable.SessionWarningInfoView_sessionsWarningInfoHasLearnMore, false)
        if (hasLearnMore) {
            val learnMore = context.getString(R.string.action_learn_more)
            val fullDescription = buildString {
                append(description)
                append(" ")
                append(learnMore)
            }

            binding.sessionWarningInfoDescription.setTextWithColoredPart(
                    fullText = fullDescription,
                    coloredPart = learnMore,
                    underline = false
            ) {
                onLearnMoreClickListener?.invoke()
            }
        } else {
            binding.sessionWarningInfoDescription.text = description
        }
    }
}
