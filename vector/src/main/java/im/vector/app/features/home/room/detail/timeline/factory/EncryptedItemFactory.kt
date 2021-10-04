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
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import javax.inject.Inject

// This class handles timeline events who haven't been successfully decrypted
class EncryptedItemFactory @Inject constructor(private val messageInformationDataFactory: MessageInformationDataFactory,
                                               private val colorProvider: ColorProvider,
                                               private val stringProvider: StringProvider,
                                               private val avatarSizeProvider: AvatarSizeProvider,
                                               private val drawableProvider: DrawableProvider,
                                               private val attributesFactory: MessageItemAttributesFactory,
                                               private val vectorPreferences: VectorPreferences) {

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        event.root.eventId ?: return null

        return when {
            EventType.ENCRYPTED == event.root.getClearType() -> {
                val cryptoError = event.root.mCryptoError

                val spannableStr = if (vectorPreferences.developerMode()) {
                    val errorDescription =
                            if (cryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                                stringProvider.getString(R.string.notice_crypto_error_unkwown_inbound_session_id)
                            } else {
                                // TODO i18n
                                cryptoError?.name
                            }

                    val message = stringProvider.getString(R.string.encrypted_message).takeIf { cryptoError == null }
                            ?: stringProvider.getString(R.string.notice_crypto_unable_to_decrypt, errorDescription)
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
                        when (cryptoError) {
                            MXCryptoError.ErrorType.KEYS_WITHHELD -> {
                                span {
                                    drawableProvider.getDrawable(R.drawable.ic_forbidden, colorFromAttribute)?.let {
                                        image(it, "baseline")
                                        +" "
                                    }
                                    span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_final)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
                                }
                            }
                            else                                  -> {
                                span {
                                    drawableProvider.getDrawable(R.drawable.ic_clock, colorFromAttribute)?.let {
                                        image(it, "baseline")
                                        +" "
                                    }
                                    span(stringProvider.getString(R.string.notice_crypto_unable_to_decrypt_friendly)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
                                }
                            }
                        }
                    }
                }

                val informationData = messageInformationDataFactory.create(params)
                val attributes = attributesFactory.create(event.root.content.toModel<EncryptedEventContent>(), informationData, params.callback)
                return MessageTextItem_()
                        .leftGuideline(avatarSizeProvider.leftGuideline)
                        .highlighted(params.isHighlighted)
                        .attributes(attributes)
                        .message(spannableStr)
                        .movementMethod(createLinkMovementMethod(params.callback))
            }
            else                                             -> null
        }
    }
}
