/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.image
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.content.EncryptedEventContent
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
                    val errorDescription =
                            if (cryptoError == MXCryptoError.ErrorType.UNKNOWN_INBOUND_SESSION_ID) {
                                stringProvider.getString(CommonStrings.notice_crypto_error_unknown_inbound_session_id)
                            } else {
                                // TODO i18n
                                cryptoError?.name
                            }

                    val message = stringProvider.getString(CommonStrings.encrypted_message).takeIf { cryptoError == null }
                            ?: stringProvider.getString(CommonStrings.notice_crypto_unable_to_decrypt, errorDescription)
                    span(message) {
                        textStyle = "italic"
                        textColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                    }
                } else {
                    val colorFromAttribute = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                    if (cryptoError == null) {
                        span(stringProvider.getString(CommonStrings.encrypted_message)) {
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
                                    span(stringProvider.getString(CommonStrings.notice_crypto_unable_to_decrypt_final)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
                                }
                            }
                            else -> {
                                span {
                                    drawableProvider.getDrawable(R.drawable.ic_clock, colorFromAttribute)?.let {
                                        image(it, "baseline")
                                        +" "
                                    }
                                    span(stringProvider.getString(CommonStrings.notice_crypto_unable_to_decrypt_friendly)) {
                                        textStyle = "italic"
                                        textColor = colorFromAttribute
                                    }
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
