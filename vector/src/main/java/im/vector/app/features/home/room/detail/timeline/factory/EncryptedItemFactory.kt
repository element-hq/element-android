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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.MessageTextItem_
import im.vector.app.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
import org.matrix.android.sdk.api.session.events.model.content.WithHeldCode
import org.matrix.android.sdk.api.session.events.model.toModel
import javax.inject.Inject

// This class handles timeline events who haven't been successfully decrypted
class EncryptedItemFactory @Inject constructor(
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val colorProvider: ColorProvider,
        private val stringProvider: StringProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val drawableProvider: DrawableProvider,
        private val attributesFactory: MessageItemAttributesFactory,
        private val vectorPreferences: VectorPreferences
) {

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        event.root.eventId ?: return null

        return when {
            EventType.ENCRYPTED == event.root.getClearType() -> {
                val cryptoError = event.root.mCryptoError

                val spannableStr = if (vectorPreferences.developerMode()) {
                    // In developer mode we want to show the raw error
                    val errorDescription = if (event.root.mCryptoWithHeldCode != null) {
                        "${cryptoError?.name ?: "NULL"} | ${event.root.mCryptoWithHeldCode?.value}"
                    } else {
                        cryptoError?.name ?: "NULL"
                    }

                    val message = stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, errorDescription).let {
                        if (event.hasActiveRequestForKeys == true) {
                            "$it - key request in progress"
                        } else it
                    }
                    span(message) {
                        textStyle = "italic"
                        textColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                    }
                } else {
                    val colorFromAttribute = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                    if (cryptoError == null) {
                        span(stringProvider.getString(R.string.encrypted_message)) {
                            textStyle = "italic"
                            textColor = colorFromAttribute
                        }
                    } else {
                        if (event.hasActiveRequestForKeys == true) {
                            span {
                                drawableProvider.getDrawable(R.drawable.ic_clock, colorFromAttribute)?.let {
                                    image(it, "baseline")
                                    +" "
                                }
                                span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_requesting_keys)) {
                                    textStyle = "italic"
                                    textColor = colorFromAttribute
                                }
                            }
                        } else if (event.root.mCryptoWithHeldCode != null) {
                            val messageId = when (event.root.mCryptoWithHeldCode) {
                                WithHeldCode.BLACKLISTED -> R.string.crypto_error_withheld_blacklisted
                                WithHeldCode.UNVERIFIED -> R.string.crypto_error_withheld_unverified
                                else -> R.string.crypto_error_withheld_generic
                            }
                            span {
                                drawableProvider.getDrawable(R.drawable.ic_forbidden, colorFromAttribute)?.let {
                                    image(it, "baseline")
                                    +" "
                                }
                                span(stringProvider.getString(messageId)) {
                                    textStyle = "italic"
                                    textColor = colorFromAttribute
                                }
                            }
                        } else {
                            span {
                                span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_no_keys)) {
                                    textStyle = "italic"
                                    textColor = colorFromAttribute
                                }
                            }
                        }
                    }
                }

                val informationData = messageInformationDataFactory.create(params)
                val threadDetails = if (params.isFromThreadTimeline()) null else event.root.threadDetails
                val attributes = attributesFactory.create(
                        messageContent = event.root.content.toModel<EncryptedEventContent>(),
                        informationData = informationData,
                        callback = params.callback,
                        threadDetails = threadDetails,
                        reactionsSummaryEvents = params.reactionsSummaryEvents
                )
                return MessageTextItem_()
                        .layout(informationData.messageLayout.layoutRes)
                        .leftGuideline(avatarSizeProvider.leftGuideline)
                        .highlighted(params.isHighlighted)
                        .attributes(attributes)
                        .message(spannableStr.toEpoxyCharSequence())
                        .movementMethod(createLinkMovementMethod(params.callback))
            }
            else -> null
        }
    }
}
