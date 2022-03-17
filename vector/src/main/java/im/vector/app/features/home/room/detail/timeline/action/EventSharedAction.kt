/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.action

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import im.vector.app.R
import im.vector.app.core.platform.VectorSharedAction
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent

sealed class EventSharedAction(@StringRes val titleRes: Int,
                               @DrawableRes val iconResId: Int,
                               val destructive: Boolean = false) : VectorSharedAction {
    object Separator :
            EventSharedAction(0, 0)

    data class OpenUserProfile(val userId: String) :
            EventSharedAction(0, 0)

    data class AddReaction(val eventId: String) :
            EventSharedAction(R.string.message_add_reaction, R.drawable.ic_add_reaction)

    data class Copy(val content: String) :
            EventSharedAction(R.string.action_copy, R.drawable.ic_copy)

    data class Edit(val eventId: String, val eventType: String) :
            EventSharedAction(R.string.edit, R.drawable.ic_edit)

    data class Quote(val eventId: String) :
            EventSharedAction(R.string.action_quote, R.drawable.ic_quote)

    data class Reply(val eventId: String) :
            EventSharedAction(R.string.reply, R.drawable.ic_reply)

    data class ReplyInThread(val eventId: String, val startsThread: Boolean) :
            EventSharedAction(R.string.reply_in_thread, R.drawable.ic_reply_in_thread)

    object ViewInRoom :
            EventSharedAction(R.string.view_in_room, R.drawable.ic_thread_view_in_room_menu_item)

    data class Share(val eventId: String, val messageContent: MessageContent) :
            EventSharedAction(R.string.action_share, R.drawable.ic_share)

    data class Save(val eventId: String, val messageContent: MessageWithAttachmentContent) :
            EventSharedAction(R.string.action_save, R.drawable.ic_material_save)

    data class Resend(val eventId: String) :
            EventSharedAction(R.string.global_retry, R.drawable.ic_refresh_cw)

    data class Remove(val eventId: String) :
            EventSharedAction(R.string.action_remove, R.drawable.ic_trash, true)

    data class Redact(val eventId: String, val askForReason: Boolean, val dialogTitleRes: Int, val dialogDescriptionRes: Int) :
            EventSharedAction(R.string.message_action_item_redact, R.drawable.ic_delete, true)

    data class Cancel(val eventId: String, val force: Boolean) :
            EventSharedAction(R.string.action_cancel, R.drawable.ic_close_round)

    data class ViewSource(val content: String) :
            EventSharedAction(R.string.view_source, R.drawable.ic_view_source)

    data class ViewDecryptedSource(val content: String) :
            EventSharedAction(R.string.view_decrypted_source, R.drawable.ic_view_source)

    data class CopyPermalink(val eventId: String) :
            EventSharedAction(R.string.permalink, R.drawable.ic_permalink)

    data class ReportContent(val eventId: String, val senderId: String?) :
            EventSharedAction(R.string.report_content, R.drawable.ic_flag)

    data class ReportContentSpam(val eventId: String, val senderId: String?) :
            EventSharedAction(R.string.report_content_spam, R.drawable.ic_report_spam)

    data class ReportContentInappropriate(val eventId: String, val senderId: String?) :
            EventSharedAction(R.string.report_content_inappropriate, R.drawable.ic_report_inappropriate)

    data class ReportContentCustom(val eventId: String, val senderId: String?) :
            EventSharedAction(R.string.report_content_custom, R.drawable.ic_report_custom)

    data class IgnoreUser(val senderId: String?) :
            EventSharedAction(R.string.message_ignore_user, R.drawable.ic_alert_triangle, true)

    data class QuickReact(val eventId: String, val clickedOn: String, val add: Boolean) :
            EventSharedAction(0, 0)

    data class ViewReactions(val messageInformationData: MessageInformationData) :
            EventSharedAction(R.string.message_view_reaction, R.drawable.ic_view_reactions)

    data class ViewEditHistory(val messageInformationData: MessageInformationData) :
            EventSharedAction(R.string.message_view_edit_history, R.drawable.ic_view_edit_history)

    // An url in the event preview has been clicked
    data class OnUrlClicked(val url: String, val title: String) :
            EventSharedAction(0, 0)

    // An url in the event preview has been long clicked
    data class OnUrlLongClicked(val url: String) :
            EventSharedAction(0, 0)

    data class ReRequestKey(val eventId: String) :
            EventSharedAction(R.string.e2e_re_request_encryption_key, R.drawable.key_small)

    object UseKeyBackup :
            EventSharedAction(R.string.e2e_use_keybackup, R.drawable.shield)

    data class EndPoll(val eventId: String) :
            EventSharedAction(R.string.poll_end_action, R.drawable.ic_check_on)
}
