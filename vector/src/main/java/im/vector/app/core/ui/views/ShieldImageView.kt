/*
 * Copyright 2021-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.features.home.room.detail.timeline.item.E2EDecoration
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class ShieldImageView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    init {
        if (isInEditMode) {
            render(RoomEncryptionTrustLevel.Trusted)
        }
    }

    fun render(roomEncryptionTrustLevel: RoomEncryptionTrustLevel?, borderLess: Boolean = false) {
        isVisible = roomEncryptionTrustLevel != null

        when (roomEncryptionTrustLevel) {
            RoomEncryptionTrustLevel.Default -> {
                contentDescription = context.getString(R.string.a11y_trust_level_default)
                setImageResource(
                        if (borderLess) R.drawable.ic_shield_black_no_border
                        else R.drawable.ic_shield_black
                )
            }
            RoomEncryptionTrustLevel.Warning -> {
                contentDescription = context.getString(R.string.a11y_trust_level_warning)
                setImageResource(
                        if (borderLess) R.drawable.ic_shield_warning_no_border
                        else R.drawable.ic_shield_warning
                )
            }
            RoomEncryptionTrustLevel.Trusted -> {
                contentDescription = context.getString(R.string.a11y_trust_level_trusted)
                setImageResource(
                        if (borderLess) R.drawable.ic_shield_trusted_no_border
                        else R.drawable.ic_shield_trusted
                )
            }
            RoomEncryptionTrustLevel.E2EWithUnsupportedAlgorithm -> {
                contentDescription = context.getString(R.string.a11y_trust_level_trusted)
                setImageResource(R.drawable.ic_warning_badge)
            }
            null -> Unit
        }
    }

    fun renderE2EDecoration(decoration: E2EDecoration?) {
        isVisible = true
        when (decoration) {
            E2EDecoration.WARN_IN_CLEAR -> {
                contentDescription = context.getString(R.string.unencrypted)
                setImageResource(R.drawable.ic_shield_warning)
            }
            E2EDecoration.WARN_SENT_BY_UNVERIFIED -> {
                contentDescription = context.getString(R.string.encrypted_unverified)
                setImageResource(R.drawable.ic_shield_warning)
            }
            E2EDecoration.WARN_SENT_BY_UNKNOWN -> {
                contentDescription = context.getString(R.string.encrypted_unverified)
                setImageResource(R.drawable.ic_shield_warning)
            }
            E2EDecoration.WARN_SENT_BY_DELETED_SESSION -> {
                contentDescription = context.getString(R.string.encrypted_unverified)
                setImageResource(R.drawable.ic_shield_warning)
            }
            E2EDecoration.WARN_UNSAFE_KEY -> {
                contentDescription = context.getString(R.string.key_authenticity_not_guaranteed)
                setImageResource(
                        R.drawable.ic_shield_gray
                )
            }
            E2EDecoration.NONE,
            null -> {
                contentDescription = null
                isVisible = false
            }
        }
    }
}

@DrawableRes
fun RoomEncryptionTrustLevel.toDrawableRes(): Int {
    return when (this) {
        RoomEncryptionTrustLevel.Default -> R.drawable.ic_shield_black
        RoomEncryptionTrustLevel.Warning -> R.drawable.ic_shield_warning
        RoomEncryptionTrustLevel.Trusted -> R.drawable.ic_shield_trusted
        RoomEncryptionTrustLevel.E2EWithUnsupportedAlgorithm -> R.drawable.ic_warning_badge
    }
}
