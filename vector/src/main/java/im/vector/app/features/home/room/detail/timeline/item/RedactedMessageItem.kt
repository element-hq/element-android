/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class RedactedMessageItem : AbsMessageItem<RedactedMessageItem.Holder>() {

    override fun getViewStubId() = STUB_ID

    override fun shouldShowReactionAtBottom() = false

    class Holder : AbsMessageItem.Holder(STUB_ID)

    companion object {
        private val STUB_ID = R.id.messageContentRedactedStub
    }
}
