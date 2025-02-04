/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.location.live.map

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.TextUtils
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.core.utils.timer.CountUpTimer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem
import org.threeten.bp.Duration

@EpoxyModelClass
abstract class LiveLocationUserItem : VectorEpoxyModel<LiveLocationUserItem.Holder>(R.layout.item_live_location_users_bottom_sheet) {

    interface Callback {
        fun onUserSelected(userId: String)
        fun onStopSharingClicked()
    }

    @EpoxyAttribute
    var callback: Callback? = null

    @EpoxyAttribute
    lateinit var matrixItem: MatrixItem

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    lateinit var stringProvider: StringProvider

    @EpoxyAttribute
    lateinit var clock: Clock

    @EpoxyAttribute
    var remainingTime: String? = null

    @EpoxyAttribute
    var locationUpdateTimeMillis: Long? = null

    @EpoxyAttribute
    var showStopSharingButton: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.itemUserAvatarImageView)
        holder.itemUserDisplayNameTextView.text = matrixItem.displayName
        holder.itemRemainingTimeTextView.text = remainingTime

        holder.itemStopSharingButton.isVisible = showStopSharingButton
        if (showStopSharingButton) {
            holder.itemStopSharingButton.onClick {
                callback?.onStopSharingClicked()
            }
        }

        holder.timer.apply {
            tickListener = CountUpTimer.TickListener {
                holder.itemLastUpdatedAtTextView.text = getFormattedLastUpdatedAt(locationUpdateTimeMillis)
            }
            start()
        }

        holder.view.setOnClickListener { callback?.onUserSelected(matrixItem.id) }
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        stopTimer(holder)
    }

    private fun stopTimer(holder: Holder) {
        holder.timer.stop()
    }

    private fun getFormattedLastUpdatedAt(locationUpdateTimeMillis: Long?): String {
        if (locationUpdateTimeMillis == null) return ""
        val elapsedTime = clock.epochMillis() - locationUpdateTimeMillis
        val duration = Duration.ofMillis(elapsedTime.coerceAtLeast(0L))
        val formattedDuration = TextUtils.formatDurationWithUnits(stringProvider, duration, appendSeconds = false)
        return stringProvider.getString(CommonStrings.live_location_bottom_sheet_last_updated_at, formattedDuration)
    }

    class Holder : VectorEpoxyHolder() {
        val timer: CountUpTimer = CountUpTimer(intervalInMs = 1000)
        val itemUserAvatarImageView by bind<ImageView>(R.id.itemUserAvatarImageView)
        val itemUserDisplayNameTextView by bind<TextView>(R.id.itemUserDisplayNameTextView)
        val itemRemainingTimeTextView by bind<TextView>(R.id.itemRemainingTimeTextView)
        val itemLastUpdatedAtTextView by bind<TextView>(R.id.itemLastUpdatedAtTextView)
        val itemStopSharingButton by bind<Button>(R.id.itemStopSharingButton)
    }
}
