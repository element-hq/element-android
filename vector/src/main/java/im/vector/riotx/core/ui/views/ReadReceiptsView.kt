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

package im.vector.riotx.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import butterknife.ButterKnife
import im.vector.riotx.R
import im.vector.riotx.core.utils.DebouncedClickListener
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.item.ReadReceiptData
import kotlinx.android.synthetic.main.view_read_receipts.view.*

private const val MAX_RECEIPT_DISPLAYED = 5

class ReadReceiptsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val receiptAvatars: List<ImageView> by lazy {
        listOf(receiptAvatar1, receiptAvatar2, receiptAvatar3, receiptAvatar4, receiptAvatar5)
    }

    init {
        setupView()
    }

    private fun setupView() {
        inflate(context, R.layout.view_read_receipts, this)
        ButterKnife.bind(this)
    }

    fun render(readReceipts: List<ReadReceiptData>, avatarRenderer: AvatarRenderer, clickListener: OnClickListener) {
        setOnClickListener(clickListener)
        if (readReceipts.isNotEmpty()) {
            isVisible = true
            for (index in 0 until MAX_RECEIPT_DISPLAYED) {
                val receiptData = readReceipts.getOrNull(index)
                if (receiptData == null) {
                    receiptAvatars[index].visibility = View.INVISIBLE
                } else {
                    receiptAvatars[index].visibility = View.VISIBLE
                    avatarRenderer.render(receiptData.avatarUrl, receiptData.userId, receiptData.displayName, receiptAvatars[index])
                }
            }
            if (readReceipts.size > MAX_RECEIPT_DISPLAYED) {
                receiptMore.visibility = View.VISIBLE
                receiptMore.text = context.getString(
                        R.string.x_plus, readReceipts.size - MAX_RECEIPT_DISPLAYED
                )
            } else {
                receiptMore.visibility = View.GONE
            }
        } else {
            isVisible = false
        }
    }

}
