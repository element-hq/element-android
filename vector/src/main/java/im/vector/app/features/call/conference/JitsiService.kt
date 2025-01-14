/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.call.conference

import im.vector.app.core.network.await
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.toBase32String
import im.vector.app.features.call.conference.jwt.JitsiJWTFactory
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.settings.VectorLocaleProvider
import im.vector.app.features.themes.ThemeProvider
import im.vector.lib.core.utils.timer.Clock
import okhttp3.Request
import org.jitsi.meet.sdk.JitsiMeetUserInfo
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.widgets.model.Widget
import org.matrix.android.sdk.api.session.widgets.model.WidgetType
import org.matrix.android.sdk.api.util.MatrixJsonParser
import org.matrix.android.sdk.api.util.appendParamToUrl
import org.matrix.android.sdk.api.util.toMatrixItem
import java.net.URL
import java.util.UUID
import javax.inject.Inject

class JitsiService @Inject constructor(
        private val session: Session,
        private val rawService: RawService,
        private val stringProvider: StringProvider,
        private val themeProvider: ThemeProvider,
        private val jitsiJWTFactory: JitsiJWTFactory,
        private val clock: Clock,
        private val vectorLocale: VectorLocaleProvider,
) {

    companion object {
        const val JITSI_OPEN_ID_TOKEN_JWT_AUTH = "openidtoken-jwt"
    }

    private val jitsiWidgetDataFactory by lazy {
        JitsiWidgetDataFactory(stringProvider.getString(im.vector.app.config.R.string.preferred_jitsi_domain)) { widget ->
            session.widgetService().getWidgetComputedUrl(widget, themeProvider.isLightTheme())
        }
    }

    suspend fun createJitsiWidget(roomId: String, withVideo: Boolean): Widget {
        // Build data for a jitsi widget
        val widgetId: String = WidgetType.Jitsi.preferred + "_" + session.myUserId + "_" + clock.epochMillis()
        val preferredJitsiDomain = tryOrNull {
            rawService.getElementWellknown(session.sessionParams)
                    ?.jitsiServer
                    ?.preferredDomain
        }
        val jitsiDomain = preferredJitsiDomain ?: stringProvider.getString(im.vector.app.config.R.string.preferred_jitsi_domain)
        val jitsiAuth = getJitsiAuth(jitsiDomain)
        val confId = createConferenceId(roomId, jitsiAuth)

        // We use the default element wrapper for this widget
        // https://github.com/element-hq/element-web/blob/develop/docs/jitsi-dev.md
        // https://github.com/matrix-org/matrix-react-sdk/blob/develop/src/utils/WidgetUtils.ts#L469
        val url = buildString {
            append("https://app.element.io/jitsi.html")
            appendParamToUrl("confId", confId)
            append("#conferenceDomain=\$domain")
            append("&conferenceId=\$conferenceId")
            append("&isAudioOnly=\$isAudioOnly")
            append("&displayName=\$matrix_display_name")
            append("&avatarUrl=\$matrix_avatar_url")
            append("&userId=\$matrix_user_id")
            append("&roomId=\$matrix_room_id")
            append("&theme=\$theme")
            if (jitsiAuth != null) {
                append("&auth=$jitsiAuth")
            }
        }
        val widgetEventContent = mapOf(
                "url" to url,
                "type" to WidgetType.Jitsi.legacy,
                "data" to JitsiWidgetData(jitsiDomain, confId, !withVideo, jitsiAuth),
                "creatorUserId" to session.myUserId,
                "id" to widgetId,
                "name" to "jitsi"
        )
        return session.widgetService().createRoomWidget(roomId, widgetId, widgetEventContent)
    }

    suspend fun joinConference(roomId: String, jitsiWidget: Widget, enableVideo: Boolean): JitsiCallViewEvents.JoinConference {
        val me = session.roomService().getRoomMember(session.myUserId, roomId)?.toMatrixItem()
        val userDisplayName = me?.getBestName()
        val userAvatar = me?.avatarUrl?.let { session.contentUrlResolver().resolveFullSize(it) }
        val userInfo = JitsiMeetUserInfo().apply {
            this.displayName = userDisplayName
            this.avatar = userAvatar?.let { URL(it) }
        }
        val roomName = session.getRoomSummary(roomId)?.displayName
        val widgetData = jitsiWidgetDataFactory.create(jitsiWidget)
        val token = if (widgetData.isOpenIdJWTAuthenticationRequired()) {
            getOpenIdJWTToken(roomId, widgetData.domain, userDisplayName ?: session.myUserId, userAvatar ?: "")
        } else {
            null
        }
        return JitsiCallViewEvents.JoinConference(
                enableVideo = enableVideo,
                jitsiUrl = widgetData.domain.ensureProtocol(),
                subject = roomName ?: "",
                confId = widgetData.confId,
                userInfo = userInfo,
                token = token
        )
    }

    fun extractJitsiWidgetData(widget: Widget): JitsiWidgetData? {
        return tryOrNull {
            jitsiWidgetDataFactory.create(widget)
        }
    }

    private fun JitsiWidgetData.isOpenIdJWTAuthenticationRequired(): Boolean {
        return auth == JITSI_OPEN_ID_TOKEN_JWT_AUTH
    }

    private suspend fun getOpenIdJWTToken(roomId: String, domain: String, userDisplayName: String, userAvatar: String): String {
        val openIdToken = session.openIdService().getOpenIdToken()
        return jitsiJWTFactory.create(
                openIdToken = openIdToken,
                jitsiServerDomain = domain,
                roomId = roomId,
                userAvatarUrl = userAvatar,
                userDisplayName = userDisplayName
        )
    }

    private fun createConferenceId(roomId: String, jitsiAuth: String?): String {
        return if (jitsiAuth == JITSI_OPEN_ID_TOKEN_JWT_AUTH) {
            // Create conference ID from room ID
            // For compatibility with Jitsi, use base32 without padding.
            // More details here:
            // https://github.com/matrix-org/prosody-mod-auth-matrix-user-verification
            roomId.toBase32String(padding = false)
        } else {
            // Create a random enough jitsi conference id
            // Note: the jitsi server automatically creates conference when the conference
            // id does not exist yet
            var widgetSessionId = UUID.randomUUID().toString()
            if (widgetSessionId.length > 8) {
                widgetSessionId = widgetSessionId.substring(0, 7)
            }
            roomId.substring(1, roomId.indexOf(":") - 1) + widgetSessionId.lowercase(vectorLocale.applicationLocale)
        }
    }

    private suspend fun getJitsiAuth(jitsiDomain: String): String? {
        val request = Request.Builder().url("$jitsiDomain/.well-known/element/jitsi".ensureProtocol()).build()
        return tryOrNull {
            val response = session.getOkHttpClient().newCall(request).await()
            val json = response.body?.string() ?: return null
            MatrixJsonParser.getMoshi().adapter(JitsiWellKnown::class.java).fromJson(json)?.auth
        }
    }
}
