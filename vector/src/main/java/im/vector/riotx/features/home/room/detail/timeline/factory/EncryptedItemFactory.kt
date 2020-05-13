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

package im.vector.riotx.features.home.room.detail.timeline.factory

import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.model.event.WithHeldCode
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.DrawableProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.riotx.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.riotx.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.riotx.features.home.room.detail.timeline.item.MessageTextItem_
import im.vector.riotx.features.home.room.detail.timeline.tools.createLinkMovementMethod
import me.gujun.android.span.image
import me.gujun.android.span.span
import javax.inject.Inject

// This class handles timeline events who haven't been successfully decrypted
class EncryptedItemFactory @Inject constructor(private val messageInformationDataFactory: MessageInformationDataFactory,
                                               private val colorProvider: ColorProvider,
                                               private val stringProvider: StringProvider,
                                               private val avatarSizeProvider: AvatarSizeProvider,
                                               private val drawableProvider: DrawableProvider,
                                               private val attributesFactory: MessageItemAttributesFactory) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               highlight: Boolean,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*>? {
        event.root.eventId ?: return null

        return when {
            EventType.ENCRYPTED == event.root.getClearType() -> {
                val cryptoError = event.root.mCryptoError
//                val errorDescription =
//                        if (cryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
//                            stringProvider.getString(R.string.notice_crypto_error_unkwown_inbound_session_id)
//                        } else {
//                            // TODO i18n
//                            cryptoError?.name
//                        }

                val colorFromAttribute = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                val spannableStr = if(cryptoError == null) {
                        span(stringProvider.getString(R.string.encrypted_message)) {
                            textStyle = "italic"
                            textColor = colorFromAttribute
                        }
                    } else {
                         when(cryptoError) {
                            MXCryptoError.ErrorType.KEYS_WITHHELD -> {
//                                val why = when (event.root.mCryptoErrorReason) {
//                                    WithHeldCode.BLACKLISTED.value -> stringProvider.getString(R.string.crypto_error_withheld_blacklisted)
//                                    WithHeldCode.UNVERIFIED.value  -> stringProvider.getString(R.string.crypto_error_withheld_unverified)
//                                    else                          -> stringProvider.getString(R.string.crypto_error_withheld_generic)
//                                }
                                //stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, why)
                                span {
                                    apply {
                                        drawableProvider.getDrawable(R.drawable.ic_forbidden, colorFromAttribute)?.let {
                                            image(it, "baseline")
                                        }
                                    }
                                    span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_final)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
                                }
                            }
                            else ->  {
                                span {
                                    apply {
                                        drawableProvider.getDrawable(R.drawable.ic_clock, colorFromAttribute)?.let {
                                            image(it, "baseline")
                                        }
                                    }
                                    span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_friendly)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
                                }

                            }
                        }

                }
//                val spannableStr = span(message) {
//                    textStyle = "italic"
//                    textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
//                }

                // TODO This is not correct format for error, change it

                val informationData = messageInformationDataFactory.create(event, nextEvent)
                val attributes = attributesFactory.create(null, informationData, callback)
                return MessageTextItem_()
                        .leftGuideline(avatarSizeProvider.leftGuideline)
                        .highlighted(highlight)
                        .attributes(attributes)
                        .message(spannableStr)
                        .movementMethod(createLinkMovementMethod(callback))
            }
            else                                             -> null
        }
    }
}
