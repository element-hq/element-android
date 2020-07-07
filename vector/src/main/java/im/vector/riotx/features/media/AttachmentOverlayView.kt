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

package im.vector.riotx.features.media

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.riotx.R

class AttachmentOverlayView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    var onShareCallback: (() -> Unit) ? = null
    var onBack: (() -> Unit) ? = null

    private val counterTextView: TextView
    private val infoTextView: TextView
    private val shareImage: ImageView

    init {
        View.inflate(context, R.layout.merge_image_attachment_overlay, this)
        setBackgroundColor(Color.TRANSPARENT)
        counterTextView = findViewById(R.id.overlayCounterText)
        infoTextView = findViewById(R.id.overlayInfoText)
        shareImage = findViewById(R.id.overlayShareButton)

        findViewById<ImageView>(R.id.overlayBackButton).setOnClickListener {
            onBack?.invoke()
        }
    }

    fun updateWith(counter: String, senderInfo : String) {
        counterTextView.text = counter
        infoTextView.text = senderInfo
    }
}
