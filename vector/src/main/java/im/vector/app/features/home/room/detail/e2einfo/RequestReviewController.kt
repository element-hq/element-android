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

package im.vector.app.features.home.room.detail.e2einfo

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.dividerItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericPositiveButtonItem
import im.vector.app.core.ui.list.genericWithValueItem
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.settings.ignored.userItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class RequestReviewController @Inject constructor(
        private val avatarRenderer: AvatarRenderer
) : EpoxyController() {

    interface Callback {
        fun doTryForward()
    }

    private var state: RequestState? = null
    var callback: Callback? = null

    fun setData(state: RequestState) {
        this.state = state
        requestModelBuild()
    }

    private inner class DividerBuilder {
        private var count = 0

        fun addDivider() {
            dividerItem { id("divider_${count++}") }
        }
    }

    override fun buildModels() {
        val host = this
        val theState = state ?: return
        val request = theState.request ?: return
        val dividerBuilder = DividerBuilder()

        userItem {
            id("user")
            avatarRenderer(host.avatarRenderer)
            matrixItem(theState.requesterItem)
        }

        genericWithValueItem {
            id("device")
            title("Device".toEpoxyCharSequence())
            value(
                    "${request.deviceId}"
            )
        }

        dividerItem {
            id("div_0")
        }

        val knownIndex = theState.knownIndex

        genericItem {
            id("verified")
            title("Local Storage".toEpoxyCharSequence())
            description(
                    if (knownIndex == null) {
                        "This key is not on this device"
                    } else {
                        "The key is on this device (index=$knownIndex)"
                    }.toEpoxyCharSequence()
            )
            endIconResourceId(
                    if (knownIndex == null) {
                        R.drawable.unit_test_ko
                    } else {
                        R.drawable.unit_test_ok
                    }
            )
        }
        dividerBuilder.addDivider()

        val isVerified = theState.requesterDeviceInfo?.trustLevel

        genericItem {
            id("verified")
            title("Verification Status:".toEpoxyCharSequence())
            description(
                    when {
                        isVerified?.isCrossSigningVerified() == true -> {
                            "Device is cross signed"
                        }
                        isVerified?.isLocallyVerified() == true      -> {
                            "Device is locally trusted"
                        }
                        else                                         -> {
                            "Device is not trusted"
                        }
                    }.toEpoxyCharSequence()
            )
            endIconResourceId(
                    if (isVerified?.isVerified() == true) {
                        R.drawable.unit_test_ok
                    } else {
                        R.drawable.unit_test_ko
                    }
            )
        }
        dividerBuilder.addDivider()

        //
        if (!theState.isFromOneOfMyDevices) {
            val isInTheRoom = theState.isUserInRoom.orFalse()
            genericItem {
                id("member")
                title(
                        "Membership".toEpoxyCharSequence()
                )
                description(
                        if (isInTheRoom) {
                            "The user is currently in the room"
                        } else {
                            "The user is not currently in the room"
                        }.toEpoxyCharSequence()
                )
                endIconResourceId(
                        if (isInTheRoom) {
                            R.drawable.unit_test_ok
                        } else {
                            R.drawable.unit_test_ko
                        }
                )
            }
        }
        dividerBuilder.addDivider()

        val wasShared = theState.wasInitiallyShared.orFalse()
        genericItem {
            id("member")
            title(
                    "Initial Share Status".toEpoxyCharSequence()
            )
            description(
                    if (wasShared) {
                        "The key was intended to be shared with this device."
                    } else {
                        "The key was not intended to be shared with this device."
                    }.toEpoxyCharSequence()
            )
            endIconResourceId(
                    if (wasShared) {
                        R.drawable.unit_test_ok
                    } else {
                        R.drawable.unit_test
                    }
            )
        }
        dividerBuilder.addDivider()

        genericPositiveButtonItem {
            id("try_share")
            text("Try to Forward Key")
            buttonClickAction { host.callback?.doTryForward() }
        }
    }
}
