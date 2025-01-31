/*
 * Copyright 2019-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.databinding.ViewReadReceiptsBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.item.ReadReceiptData
import im.vector.app.features.home.room.detail.timeline.item.toMatrixItem

private const val MAX_RECEIPT_DISPLAYED = 3

class ReadReceiptsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val views: ViewReadReceiptsBinding

    init {
        setupView()
        views = ViewReadReceiptsBinding.bind(this)
    }

    private val receiptAvatars: List<ImageView> by lazy {
        listOf(views.receiptAvatar1, views.receiptAvatar2, views.receiptAvatar3)
    }

    private fun setupView() {
        inflate(context, R.layout.view_read_receipts, this)
        contentDescription = context.getString(R.string.a11y_view_read_receipts)
    }

    fun render(readReceipts: List<ReadReceiptData>, avatarRenderer: AvatarRenderer) {
        receiptAvatars.forEach { it.isVisible = false }

        readReceipts.take(MAX_RECEIPT_DISPLAYED).forEachIndexed { index, receiptData ->
            receiptAvatars[index].isVisible = true
            avatarRenderer.render(receiptData.toMatrixItem(), receiptAvatars[index])
        }

        val displayNames = readReceipts
                .mapNotNull { it.displayName }
                .filter { it.isNotBlank() }
                .take(MAX_RECEIPT_DISPLAYED)

        if (readReceipts.size > MAX_RECEIPT_DISPLAYED) {
            views.receiptMore.visibility = View.VISIBLE
            views.receiptMore.text = context.getString(
                    R.string.x_plus, readReceipts.size - MAX_RECEIPT_DISPLAYED
            )
        } else {
            views.receiptMore.visibility = View.GONE
        }
        contentDescription = when (readReceipts.size) {
            1 ->
                if (displayNames.size == 1) {
                    context.getString(R.string.one_user_read, displayNames[0])
                } else {
                    context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                }
            2 ->
                if (displayNames.size == 2) {
                    context.getString(R.string.two_users_read, displayNames[0], displayNames[1])
                } else {
                    context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                }
            3 ->
                if (displayNames.size == 3) {
                    context.getString(R.string.three_users_read, displayNames[0], displayNames[1], displayNames[2])
                } else {
                    context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                }
            else ->
                if (displayNames.size >= 2) {
                    val qty = readReceipts.size - 2
                    context.resources.getQuantityString(
                            R.plurals.two_and_some_others_read,
                            qty,
                            displayNames[0],
                            displayNames[1],
                            qty
                    )
                } else {
                    context.resources.getQuantityString(R.plurals.fallback_users_read, readReceipts.size)
                }
        }
    }

    fun unbind(avatarRenderer: AvatarRenderer?) {
        receiptAvatars.forEach {
            avatarRenderer?.clear(it)
        }
        isVisible = false
    }
}
