/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.core.platform.VectorSharedAction
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

sealed class EventSharedAction(
        @StringRes val titleRes: Int,
        @DrawableRes val iconResId: Int,
        val destructive: Boolean = false
) : VectorSharedAction {
    object Separator :
            EventSharedAction(0, 0)

    data class OpenUserProfile(val userId: String) :
            EventSharedAction(0, 0)

    data class AddReaction(val eventId: String) :
            EventSharedAction(CommonStrings.message_add_reaction, R.drawable.ic_add_reaction)

    data class Copy(val content: String) :
            EventSharedAction(CommonStrings.action_copy, R.drawable.ic_copy)

    data class Edit(val eventId: String, val eventType: String) :
            EventSharedAction(CommonStrings.edit, R.drawable.ic_edit)

    data class Quote(val eventId: String) :
            EventSharedAction(CommonStrings.action_quote, R.drawable.ic_quote)

    data class Reply(val eventId: String) :
            EventSharedAction(CommonStrings.reply, R.drawable.ic_reply)

    data class ReplyInThread(val eventId: String, val startsThread: Boolean) :
            EventSharedAction(CommonStrings.reply_in_thread, R.drawable.ic_reply_in_thread)

    object ViewInRoom :
            EventSharedAction(CommonStrings.view_in_room, R.drawable.ic_threads_view_in_room_24)

    data class Share(val eventId: String, val messageContent: MessageContent) :
            EventSharedAction(CommonStrings.action_share, R.drawable.ic_share)

    data class Save(val eventId: String, val messageContent: MessageWithAttachmentContent) :
            EventSharedAction(CommonStrings.action_save, R.drawable.ic_material_save)

    data class Resend(val eventId: String) :
            EventSharedAction(CommonStrings.global_retry, R.drawable.ic_refresh_cw)

    data class Remove(val eventId: String) :
            EventSharedAction(CommonStrings.action_remove, R.drawable.ic_trash, true)

    data class Redact(val eventId: String, val askForReason: Boolean, val dialogTitleRes: Int, val dialogDescriptionRes: Int) :
            EventSharedAction(CommonStrings.message_action_item_redact, R.drawable.ic_delete, true)

    data class Cancel(val event: TimelineEvent, val force: Boolean) :
            EventSharedAction(CommonStrings.action_cancel, R.drawable.ic_close_round)

    data class ViewSource(val content: String) :
            EventSharedAction(CommonStrings.view_source, R.drawable.ic_view_source)

    data class ViewDecryptedSource(val content: String) :
            EventSharedAction(CommonStrings.view_decrypted_source, R.drawable.ic_view_source)

    data class CopyPermalink(val eventId: String) :
            EventSharedAction(CommonStrings.permalink, R.drawable.ic_permalink)

    data class ReportContent(val eventId: String, val senderId: String?) :
            EventSharedAction(CommonStrings.report_content, R.drawable.ic_flag)

    data class ReportContentSpam(val eventId: String, val senderId: String?) :
            EventSharedAction(CommonStrings.report_content_spam, R.drawable.ic_report_spam)

    data class ReportContentInappropriate(val eventId: String, val senderId: String?) :
            EventSharedAction(CommonStrings.report_content_inappropriate, R.drawable.ic_report_inappropriate)

    data class ReportContentCustom(val eventId: String, val senderId: String?) :
            EventSharedAction(CommonStrings.report_content_custom, R.drawable.ic_report_custom)

    data class IgnoreUser(val senderId: String?) :
            EventSharedAction(CommonStrings.message_ignore_user, R.drawable.ic_alert_triangle, true)

    data class ReportUser(val eventId: String, val senderId: String?) :
            EventSharedAction(CommonStrings.message_report_user, R.drawable.ic_flag, true)

    data class QuickReact(val eventId: String, val clickedOn: String, val add: Boolean) :
            EventSharedAction(0, 0)

    data class ViewReactions(val messageInformationData: MessageInformationData) :
            EventSharedAction(CommonStrings.message_view_reaction, R.drawable.ic_view_reactions)

    data class ViewEditHistory(val messageInformationData: MessageInformationData) :
            EventSharedAction(CommonStrings.message_view_edit_history, R.drawable.ic_view_edit_history)

    // An url in the event preview has been clicked
    data class OnUrlClicked(val url: String, val title: String) :
            EventSharedAction(0, 0)

    // An url in the event preview has been long clicked
    data class OnUrlLongClicked(val url: String) :
            EventSharedAction(0, 0)

    data class ReRequestKey(val eventId: String) :
            EventSharedAction(CommonStrings.e2e_re_request_encryption_key, R.drawable.key_small)

    object UseKeyBackup :
            EventSharedAction(CommonStrings.e2e_use_keybackup, R.drawable.shield)

    data class EndPoll(val eventId: String) :
            EventSharedAction(CommonStrings.poll_end_action, R.drawable.ic_check_on)
}
