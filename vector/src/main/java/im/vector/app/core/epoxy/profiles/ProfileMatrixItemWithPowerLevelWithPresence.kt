/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import org.matrix.android.sdk.api.session.presence.model.UserPresence

@EpoxyModelClass
abstract class ProfileMatrixItemWithPowerLevelWithPresence : ProfileMatrixItemWithPowerLevel() {

    @EpoxyAttribute var showPresence: Boolean = true
    @EpoxyAttribute var userPresence: UserPresence? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.presenceImageView.render(showPresence, userPresence)
    }
}
