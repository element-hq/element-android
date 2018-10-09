/*
 * Copyright 2014 OpenMarket Ltd
 * Copyright 2017 Vector Creations Ltd
 * Copyright 2018 New Vector Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;

import im.vector.matrix.android.internal.legacy.crypto.MXCryptoError;
import im.vector.matrix.android.internal.legacy.crypto.MXEventDecryptionResult;
import im.vector.matrix.android.internal.legacy.db.MXMediasCache;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedFileInfo;
import im.vector.matrix.android.internal.legacy.rest.model.message.FileMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.ImageMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.LocationMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.message.StickerMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.VideoMessage;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Generic event class with all possible fields for events.
 */
public class Event implements Externalizable {
    private static final String LOG_TAG = Event.class.getSimpleName();

    private static final long serialVersionUID = -1431845331022808337L;

    public enum SentState {
        // the event has not been sent
        UNSENT,
        // the event is encrypting
        ENCRYPTING,
        // the event is currently sending
        SENDING,
        // the event is going to be resent asap
        WAITING_RETRY,
        // the event has been sent
        SENT,
        // The event failed to be sent
        UNDELIVERED,
        // the event failed to be sent because some unknown devices have been found while encrypting it
        FAILED_UNKNOWN_DEVICES
    }

    // when there is no more message to be paginated in a room
    // the server returns a null token.
    // defines by a non null one to be able to store it.
    public static final String PAGINATE_BACK_TOKEN_END = "PAGINATE_BACK_TOKEN_END";

    public static final String EVENT_TYPE_PRESENCE = "m.presence";
    public static final String EVENT_TYPE_MESSAGE = "m.room.message";
    public static final String EVENT_TYPE_STICKER = "m.sticker";
    public static final String EVENT_TYPE_MESSAGE_ENCRYPTED = "m.room.encrypted";
    public static final String EVENT_TYPE_MESSAGE_ENCRYPTION = "m.room.encryption";
    public static final String EVENT_TYPE_FEEDBACK = "m.room.message.feedback";
    public static final String EVENT_TYPE_TYPING = "m.typing";
    public static final String EVENT_TYPE_REDACTION = "m.room.redaction";
    public static final String EVENT_TYPE_RECEIPT = "m.receipt";
    public static final String EVENT_TYPE_TAGS = "m.tag";
    public static final String EVENT_TYPE_ROOM_KEY = "m.room_key";
    public static final String EVENT_TYPE_READ_MARKER = "m.fully_read";
    public static final String EVENT_TYPE_ROOM_PLUMBING = "m.room.plumbing";
    public static final String EVENT_TYPE_ROOM_BOT_OPTIONS = "m.room.bot.options";
    public static final String EVENT_TYPE_ROOM_KEY_REQUEST = "m.room_key_request";
    public static final String EVENT_TYPE_FORWARDED_ROOM_KEY = "m.forwarded_room_key";
    public static final String EVENT_TYPE_URL_PREVIEW = "org.matrix.room.preview_urls";

    // State events
    public static final String EVENT_TYPE_STATE_ROOM_NAME = "m.room.name";
    public static final String EVENT_TYPE_STATE_ROOM_TOPIC = "m.room.topic";
    public static final String EVENT_TYPE_STATE_ROOM_AVATAR = "m.room.avatar";
    public static final String EVENT_TYPE_STATE_ROOM_MEMBER = "m.room.member";
    public static final String EVENT_TYPE_STATE_ROOM_THIRD_PARTY_INVITE = "m.room.third_party_invite";
    public static final String EVENT_TYPE_STATE_ROOM_CREATE = "m.room.create";
    public static final String EVENT_TYPE_STATE_ROOM_JOIN_RULES = "m.room.join_rules";
    public static final String EVENT_TYPE_STATE_ROOM_GUEST_ACCESS = "m.room.guest_access";
    public static final String EVENT_TYPE_STATE_ROOM_POWER_LEVELS = "m.room.power_levels";
    public static final String EVENT_TYPE_STATE_ROOM_ALIASES = "m.room.aliases";
    public static final String EVENT_TYPE_STATE_ROOM_TOMBSTONE = "m.room.tombstone";
    public static final String EVENT_TYPE_STATE_CANONICAL_ALIAS = "m.room.canonical_alias";
    public static final String EVENT_TYPE_STATE_HISTORY_VISIBILITY = "m.room.history_visibility";
    public static final String EVENT_TYPE_STATE_RELATED_GROUPS = "m.room.related_groups";
    public static final String EVENT_TYPE_STATE_PINNED_EVENT = "m.room.pinned_events";

    // call events
    public static final String EVENT_TYPE_CALL_INVITE = "m.call.invite";
    public static final String EVENT_TYPE_CALL_CANDIDATES = "m.call.candidates";
    public static final String EVENT_TYPE_CALL_ANSWER = "m.call.answer";
    public static final String EVENT_TYPE_CALL_HANGUP = "m.call.hangup";

    public static final long DUMMY_EVENT_AGE = Long.MAX_VALUE - 1;

    /**
     * Type of the event
     * Warning, consider using {@link #getType()} to get the type of the unencrypted event
     */
    public String type;

    public transient JsonElement content = null;
    private String contentAsString = null;

    public transient JsonElement prev_content = null;
    private String prev_content_as_string = null;

    public String eventId;
    public String roomId;
    // former Sync V1 sender name
    public String userId;
    // Sync V2 sender name
    public String sender;
    public long originServerTs;
    public Long age;

    // Specific to state events
    @SerializedName("state_key")
    public String stateKey;

    // Contains optional extra information about the event.
    public UnsignedData unsigned;

    // Specific to redaction
    public String redacts;

    // A subset of the state of the room at the time of the invite, if membership is invite
    public List<Event> invite_room_state;

    // store the exception triggered when unsent
    public Exception unsentException = null;
    public MatrixError unsentMatrixError = null;

    // sent state
    public SentState mSentState;

    // save the token to back paginate
    // the room history could have been reduced to save memory.
    // so store the token from each event.
    public String mToken;

    // The file cache uses the token as a pagination marker.
    // When the user paginates, the file cache paginate until to find X events or an event with a token.
    // This token must be used to perform a server catchup.
    public boolean mIsInternalPaginationToken;

    // store the linked matrix id
    private String mMatrixId;

    // the time raw offset (time zone management)
    private long mTimeZoneRawOffset = 0;

    private long getTimeZoneOffset() {
        return TimeZone.getDefault().getRawOffset();
    }

    /**
     * Default constructor
     */
    public Event() {
        type = null;
        content = null;
        prev_content = null;
        mIsInternalPaginationToken = false;

        userId = roomId = eventId = null;
        originServerTs = 0;
        age = null;

        mTimeZoneRawOffset = getTimeZoneOffset();

        stateKey = null;
        redacts = null;

        unsentMatrixError = null;
        unsentException = null;

        mMatrixId = null;

        mSentState = SentState.SENT;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return (null == sender) ? userId : sender;
    }

    /**
     * Update the sender
     *
     * @param aSender the new sender
     */
    public void setSender(String aSender) {
        sender = userId = aSender;
    }

    /**
     * Update the matrix Id.
     *
     * @param aMatrixId the new matrix id.
     */
    public void setMatrixId(String aMatrixId) {
        mMatrixId = aMatrixId;
    }

    /**
     * @return the matrix id.
     */
    public String getMatrixId() {
        return mMatrixId;
    }

    static final long MAX_ORIGIN_SERVER_TS = 1L << 50L;

    /**
     * @return true if originServerTs is valid.
     */
    public boolean isValidOriginServerTs() {
        return originServerTs < MAX_ORIGIN_SERVER_TS;
    }

    /**
     * @return the originServerTs.
     */
    public long getOriginServerTs() {
        return originServerTs;
    }

    /**
     * Update the event content.
     *
     * @param newContent the new content.
     */
    public void updateContent(JsonElement newContent) {
        content = newContent;
        contentAsString = null;
    }

    /**
     * @return true if content has some entries
     */
    public boolean hasContentFields() {
        boolean res = false;
        JsonObject json = getContentAsJsonObject();

        if (null != json) {
            Set<Map.Entry<String, JsonElement>> entries = getContentAsJsonObject().entrySet();

            res = (null != entries) && (0 != entries.size());
        }
        return res;
    }

    /**
     * @return true if this event was redacted
     */
    public boolean isRedacted() {
        return (null != unsigned) && (null != unsigned.redacted_because);
    }

    static DateFormat mDateFormat = null;
    static long mFormatterRawOffset = 1234;

    /**
     * @return a formatted timestamp.
     */
    public String formattedOriginServerTs() {
        // avoid displaying weird origin ts
        if (!isValidOriginServerTs()) {
            return " ";
        } else {
            // the formatter must be updated if the timezone has been updated
            // else the formatted string are wrong (does not use the current timezone)
            if ((null == mDateFormat) || (mFormatterRawOffset != getTimeZoneOffset())) {
                mDateFormat = new SimpleDateFormat("MMM d HH:mm", Locale.getDefault());
                mFormatterRawOffset = getTimeZoneOffset();
            }

            return mDateFormat.format(new Date(getOriginServerTs()));
        }
    }

    /**
     * Update the originServerTs.
     *
     * @param anOriginServer the new originServerTs.
     */
    public void setOriginServerTs(long anOriginServer) {
        originServerTs = anOriginServer;
    }

    /**
     * @return the event type
     */
    public String getType() {
        if (null != mClearEvent) {
            return mClearEvent.type;
        } else {
            return type;
        }
    }

    /**
     * Update the event type
     *
     * @param aType the new type
     */
    public void setType(String aType) {
        // TODO manage encryption
        type = aType;
    }

    /**
     * @return the wire event type
     */
    public String getWireType() {
        return type;
    }

    /**
     * @return the event content
     */
    public JsonElement getContent() {
        if (null != mClearEvent) {
            return mClearEvent.getWireContent();
        } else {
            return getWireContent();
        }
    }

    /**
     * @return the wired event content
     */
    public JsonElement getWireContent() {
        finalizeDeserialization();
        return content;
    }

    /**
     * @return a Json representation of the event
     */
    public JsonObject toJsonObject() {
        finalizeDeserialization();
        return JsonUtils.toJson(this);
    }

    /**
     * @return the content casted as JsonObject.
     */
    @Nullable
    public JsonObject getContentAsJsonObject() {
        JsonElement cont = getContent();

        if (null != cont && cont.isJsonObject()) {
            return cont.getAsJsonObject();
        }
        return null;
    }

    /**
     * @return the prev_content casted as JsonObject.
     */
    public JsonObject getPrevContentAsJsonObject() {
        finalizeDeserialization();

        if ((null != unsigned) && (null != unsigned.prev_content)) {
            // avoid getting two value for the same thing
            if (null == prev_content) {
                prev_content = unsigned.prev_content;
            }
            unsigned.prev_content = null;
        }

        if ((null != prev_content) && prev_content.isJsonObject()) {
            return prev_content.getAsJsonObject();
        }
        return null;
    }

    /**
     * @return the content formatted as EventContent.
     */
    public EventContent getEventContent() {
        if (null != getContent()) {
            return JsonUtils.toEventContent(getContent());
        }
        return null;
    }

    /**
     * @return the content formatted as EventContent.
     */
    public EventContent getWireEventContent() {
        if (null != getWireContent()) {
            return JsonUtils.toEventContent(getWireContent());
        }
        return null;
    }

    /**
     * @return the content formatted as EventContent.
     */
    public EventContent getPrevContent() {
        if (null != getPrevContentAsJsonObject()) {
            return JsonUtils.toEventContent(getPrevContentAsJsonObject());
        }
        return null;
    }

    /**
     * @return the event age.
     */
    public long getAge() {
        if (null != age) {
            return age;
        } else if ((null != unsigned) && (null != unsigned.age)) {
            age = unsigned.age;
            return age;
        }

        return Long.MAX_VALUE;
    }

    /**
     * @return the redacted event id.
     */
    @Nullable
    public String getRedactedEventId() {
        if (null != redacts) {
            return redacts;
        } else if (isRedacted()) {
            redacts = unsigned.redacted_because.redacts;
            return redacts;
        }

        return null;
    }

    /**
     * Create an event from a message.
     *
     * @param message  the event content
     * @param anUserId the event user Id
     * @param aRoomId  the vent room Id
     */
    public Event(Message message, String anUserId, String aRoomId) {
        type = Event.EVENT_TYPE_MESSAGE;
        content = JsonUtils.toJson(message);
        originServerTs = System.currentTimeMillis();
        sender = userId = anUserId;
        roomId = aRoomId;
        mSentState = Event.SentState.UNSENT;
        createDummyEventId();
    }

    /**
     * Create an event from a content and a type.
     *
     * @param aType    the event type
     * @param aContent the event content
     * @param anUserId the event user Id
     * @param aRoomId  the vent room Id
     */
    public Event(String aType, JsonObject aContent, String anUserId, String aRoomId) {
        type = aType;
        content = aContent;
        originServerTs = System.currentTimeMillis();
        sender = userId = anUserId;
        roomId = aRoomId;
        mSentState = Event.SentState.UNSENT;
        createDummyEventId();
    }

    /**
     * Some events are not sent by the server.
     * They are temporary stored until to get the server response.
     */
    public void createDummyEventId() {
        eventId = roomId + "-" + originServerTs;
        age = DUMMY_EVENT_AGE;
    }

    /**
     * @return true if the event is a dummy id i.e this event has been created with createDummyEventId.
     */
    public boolean isDummyEvent() {
        return (roomId + "-" + originServerTs).equals(eventId);
    }

    /**
     * Update the pagination token.
     *
     * @param token the new token.
     */
    public void setInternalPaginationToken(String token) {
        mToken = token;
        mIsInternalPaginationToken = true;
    }

    /**
     * @return true if the token has been set by setInternalPaginationToken.
     */
    public boolean isInternalPaginationToken() {
        return mIsInternalPaginationToken;
    }

    /**
     * @return true if the event has a token.
     */
    public boolean hasToken() {
        return (null != mToken) && !mIsInternalPaginationToken;
    }

    /**
     * @return true if the event if a call event.
     */
    public boolean isCallEvent() {
        return EVENT_TYPE_CALL_INVITE.equals(getType())
                || EVENT_TYPE_CALL_CANDIDATES.equals(getType())
                || EVENT_TYPE_CALL_ANSWER.equals(getType())
                || EVENT_TYPE_CALL_HANGUP.equals(getType());
    }

    /**
     * Make a deep copy of this room state object.
     *
     * @return the copy
     */
    public Event deepCopy() {
        finalizeDeserialization();

        Event copy = new Event();
        copy.type = type;
        copy.content = content;
        copy.contentAsString = contentAsString;

        copy.eventId = eventId;
        copy.roomId = roomId;
        copy.userId = userId;
        copy.sender = sender;
        copy.originServerTs = originServerTs;
        copy.mTimeZoneRawOffset = mTimeZoneRawOffset;
        copy.age = age;

        copy.stateKey = stateKey;
        copy.prev_content = prev_content;
        copy.prev_content_as_string = prev_content_as_string;

        copy.unsigned = unsigned;
        copy.invite_room_state = invite_room_state;
        copy.redacts = redacts;

        copy.mSentState = mSentState;

        copy.unsentException = unsentException;
        copy.unsentMatrixError = unsentMatrixError;

        copy.mMatrixId = mMatrixId;
        copy.mToken = mToken;
        copy.mIsInternalPaginationToken = mIsInternalPaginationToken;

        return copy;
    }

    /**
     * Check if the current event can resent.
     *
     * @return true if it can be resent.
     */
    public boolean canBeResent() {
        return (mSentState == SentState.WAITING_RETRY) || (mSentState == SentState.UNDELIVERED) || (mSentState == SentState.FAILED_UNKNOWN_DEVICES);
    }

    /**
     * Check if the current event is encrypting.
     *
     * @return true if the message encryption is in progress.
     */
    public boolean isEncrypting() {
        return (mSentState == SentState.ENCRYPTING);
    }

    /**
     * Check if the current event is unsent.
     *
     * @return true if it is unsent.
     */
    public boolean isUnsent() {
        return (mSentState == SentState.UNSENT);
    }

    /**
     * Check if the current event is sending.
     *
     * @return true if it is sending.
     */
    public boolean isSending() {
        return (mSentState == SentState.SENDING) || (mSentState == SentState.WAITING_RETRY);
    }

    /**
     * Tell if the message sending failed
     *
     * @return true if the event has not been sent because of a failure
     */
    public boolean isUndelivered() {
        return (mSentState == SentState.UNDELIVERED);
    }

    /**
     * Tells if the message sending failed because some unknown devices have been detected.
     *
     * @return true if some unknown devices have been detected.
     */
    public boolean isUnknownDevice() {
        return (mSentState == SentState.FAILED_UNKNOWN_DEVICES);
    }

    /**
     * Check if the current event is sent.
     *
     * @return true if it is sent.
     */
    public boolean isSent() {
        return (mSentState == SentState.SENT);
    }

    /**
     * @return the media URLs defined in the event.
     */
    public List<String> getMediaUrls() {
        List<String> urls = new ArrayList<>();

        if (Event.EVENT_TYPE_MESSAGE.equals(getType())) {
            String msgType = JsonUtils.getMessageMsgType(getContent());

            if (Message.MSGTYPE_IMAGE.equals(msgType)) {
                ImageMessage imageMessage = JsonUtils.toImageMessage(getContent());

                if (null != imageMessage.getUrl()) {
                    urls.add(imageMessage.getUrl());
                }
                if (null != imageMessage.getThumbnailUrl()) {
                    urls.add(imageMessage.getThumbnailUrl());
                }
            } else if (Message.MSGTYPE_FILE.equals(msgType) || Message.MSGTYPE_AUDIO.equals(msgType)) {
                FileMessage fileMessage = JsonUtils.toFileMessage(getContent());

                if (null != fileMessage.getUrl()) {
                    urls.add(fileMessage.getUrl());
                }
            } else if (Message.MSGTYPE_VIDEO.equals(msgType)) {
                VideoMessage videoMessage = JsonUtils.toVideoMessage(getContent());

                if (null != videoMessage.getUrl()) {
                    urls.add(videoMessage.getUrl());
                }
                if (null != videoMessage.getThumbnailUrl()) {
                    urls.add(videoMessage.getThumbnailUrl());
                }
            } else if (Message.MSGTYPE_LOCATION.equals(msgType)) {
                LocationMessage locationMessage = JsonUtils.toLocationMessage(getContent());

                if (null != locationMessage.thumbnail_url) {
                    urls.add(locationMessage.thumbnail_url);
                }
            }
        } else if (Event.EVENT_TYPE_STICKER.equals(getType())) {
            StickerMessage stickerMessage = JsonUtils.toStickerMessage(getContent());

            if (null != stickerMessage.getUrl()) {
                urls.add(stickerMessage.getUrl());
            }

            if (null != stickerMessage.getThumbnailUrl()) {
                urls.add(stickerMessage.getThumbnailUrl());
            }
        }

        return urls;
    }

    /**
     * @return all the encrypted file infos defined in the event.
     */
    public List<EncryptedFileInfo> getEncryptedFileInfos() {
        List<EncryptedFileInfo> encryptedFileInfos = new ArrayList<>();

        if (!isEncrypted()) {
            // return empty array
            return encryptedFileInfos;
        }

        if (Event.EVENT_TYPE_MESSAGE.equals(getType())) {
            String msgType = JsonUtils.getMessageMsgType(getContent());

            if (Message.MSGTYPE_IMAGE.equals(msgType)) {
                ImageMessage imageMessage = JsonUtils.toImageMessage(getContent());

                if (null != imageMessage.file) {
                    encryptedFileInfos.add(imageMessage.file);
                }
                if (null != imageMessage.info && null != imageMessage.info.thumbnail_file) {
                    encryptedFileInfos.add(imageMessage.info.thumbnail_file);
                }
            } else if (Message.MSGTYPE_FILE.equals(msgType) || Message.MSGTYPE_AUDIO.equals(msgType)) {
                FileMessage fileMessage = JsonUtils.toFileMessage(getContent());

                if (null != fileMessage.file) {
                    encryptedFileInfos.add(fileMessage.file);
                }
            } else if (Message.MSGTYPE_VIDEO.equals(msgType)) {
                VideoMessage videoMessage = JsonUtils.toVideoMessage(getContent());

                if (null != videoMessage.file) {
                    encryptedFileInfos.add(videoMessage.file);
                }
                if (null != videoMessage.info && null != videoMessage.info.thumbnail_file) {
                    encryptedFileInfos.add(videoMessage.info.thumbnail_file);
                }
            }
        } else if (Event.EVENT_TYPE_STICKER.equals(getType())) {
            StickerMessage stickerMessage = JsonUtils.toStickerMessage(getContent());

            if (null != stickerMessage.file) {
                encryptedFileInfos.add(stickerMessage.file);
            }
            if (null != stickerMessage.info && null != stickerMessage.info.thumbnail_file) {
                encryptedFileInfos.add(stickerMessage.info.thumbnail_file);
            }
        }

        return encryptedFileInfos;
    }

    /**
     * Tells if the current event is uploading a media.
     *
     * @param mediasCache the media cache
     * @return true if the event is uploading a media.
     */
    public boolean isUploadingMedias(MXMediasCache mediasCache) {
        List<String> urls = getMediaUrls();

        for (String url : urls) {
            if (mediasCache.getProgressValueForUploadId(url) >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tells if the current event is downloading a media.
     *
     * @param mediasCache the media cache
     * @return true if the event is downloading a media.
     */
    public boolean isDownloadingMedias(MXMediasCache mediasCache) {
        List<String> urls = getMediaUrls();

        for (String url : urls) {
            if (mediasCache.getProgressValueForDownloadId(mediasCache.downloadIdFromUrl(url)) >= 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        // build the string by hand
        String text = "{\n";

        text += "  \"age\" : " + age + ",\n";

        text += "  \"content\": {\n";

        if (null != getWireContent()) {
            if (getWireContent().isJsonArray()) {
                for (JsonElement e : getWireContent().getAsJsonArray()) {
                    text += "    " + e.toString() + ",\n";
                }
            } else if (getWireContent().isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : getWireContent().getAsJsonObject().entrySet()) {
                    text += "    \"" + e.getKey() + "\": " + e.getValue().toString() + ",\n";
                }
            } else {
                text += getWireContent().toString();
            }
        }

        text += "  },\n";

        text += "  \"eventId\": \"" + eventId + "\",\n";
        text += "  \"originServerTs\": " + originServerTs + ",\n";
        text += "  \"roomId\": \"" + roomId + "\",\n";
        text += "  \"type\": \"" + type + "\",\n";
        text += "  \"userId\": \"" + userId + "\",\n";
        text += "  \"sender\": \"" + sender + "\",\n";

        text += "}";

        text += "\n\n Sent state : ";

        if (mSentState == SentState.UNSENT) {
            text += "UNSENT";
        } else if (mSentState == SentState.SENDING) {
            text += "SENDING";
        } else if (mSentState == SentState.WAITING_RETRY) {
            text += "WAITING_RETRY";
        } else if (mSentState == SentState.SENT) {
            text += "SENT";
        } else if (mSentState == SentState.UNDELIVERED) {
            text += "UNDELIVERED";
        } else if (mSentState == SentState.FAILED_UNKNOWN_DEVICES) {
            text += "FAILED UNKNOWN DEVICES";
        }

        if (null != unsentException) {
            text += "\n\n Exception reason: " + unsentException.getMessage() + "\n";
        }

        if (null != unsentMatrixError) {
            text += "\n\n Matrix reason: " + unsentMatrixError.getLocalizedMessage() + "\n";
        }

        return text;
    }

    @Override
    public void readExternal(ObjectInput input) throws IOException, ClassNotFoundException {
        if (input.readBoolean()) {
            type = input.readUTF();
        }

        if (input.readBoolean()) {
            contentAsString = input.readUTF();
        }

        if (input.readBoolean()) {
            prev_content_as_string = input.readUTF();
        }

        if (input.readBoolean()) {
            eventId = input.readUTF();
        }

        if (input.readBoolean()) {
            roomId = input.readUTF();
        }

        if (input.readBoolean()) {
            userId = input.readUTF();
        }

        if (input.readBoolean()) {
            sender = input.readUTF();
        }

        originServerTs = input.readLong();

        if (input.readBoolean()) {
            age = input.readLong();
        }

        if (input.readBoolean()) {
            stateKey = input.readUTF();
        }

        if (input.readBoolean()) {
            unsigned = (UnsignedData) input.readObject();
        }

        if (input.readBoolean()) {
            redacts = input.readUTF();
        }

        if (input.readBoolean()) {
            invite_room_state = (List<Event>) input.readObject();
        }

        if (input.readBoolean()) {
            unsentException = (Exception) input.readObject();
        }

        if (input.readBoolean()) {
            unsentMatrixError = (MatrixError) input.readObject();
        }

        mSentState = (SentState) input.readObject();

        if (input.readBoolean()) {
            mToken = input.readUTF();
        }

        mIsInternalPaginationToken = input.readBoolean();

        if (input.readBoolean()) {
            mMatrixId = input.readUTF();
        }

        mTimeZoneRawOffset = input.readLong();
    }

    @Override
    public void writeExternal(ObjectOutput output) throws IOException {
        prepareSerialization();

        output.writeBoolean(null != type);
        if (null != type) {
            output.writeUTF(type);
        }

        output.writeBoolean(null != contentAsString);
        if (null != contentAsString) {
            output.writeUTF(contentAsString);
        }

        output.writeBoolean(null != prev_content_as_string);
        if (null != prev_content_as_string) {
            output.writeUTF(prev_content_as_string);
        }

        output.writeBoolean(null != eventId);
        if (null != eventId) {
            output.writeUTF(eventId);
        }

        output.writeBoolean(null != roomId);
        if (null != roomId) {
            output.writeUTF(roomId);
        }

        output.writeBoolean(null != userId);
        if (null != userId) {
            output.writeUTF(userId);
        }

        output.writeBoolean(null != sender);
        if (null != sender) {
            output.writeUTF(sender);
        }

        output.writeLong(originServerTs);

        output.writeBoolean(null != age);
        if (null != age) {
            output.writeLong(age);
        }

        output.writeBoolean(null != stateKey);
        if (null != stateKey) {
            output.writeUTF(stateKey);
        }

        output.writeBoolean(null != unsigned);
        if (null != unsigned) {
            output.writeObject(unsigned);
        }

        output.writeBoolean(null != redacts);
        if (null != redacts) {
            output.writeUTF(redacts);
        }

        output.writeBoolean(null != invite_room_state);
        if (null != invite_room_state) {
            output.writeObject(invite_room_state);
        }

        output.writeBoolean(null != unsentException);
        if (null != unsentException) {
            output.writeObject(unsentException);
        }

        output.writeBoolean(null != unsentMatrixError);
        if (null != unsentMatrixError) {
            output.writeObject(unsentMatrixError);
        }

        output.writeObject(mSentState);

        output.writeBoolean(null != mToken);
        if (null != mToken) {
            output.writeUTF(mToken);
        }

        output.writeBoolean(mIsInternalPaginationToken);

        output.writeBoolean(null != mMatrixId);
        if (null != mMatrixId) {
            output.writeUTF(mMatrixId);
        }

        output.writeLong(mTimeZoneRawOffset);
    }

    /**
     * Init some internal fields to serialize the event.
     */
    private void prepareSerialization() {
        if ((null != content) && (null == contentAsString)) {
            contentAsString = content.toString();
        }

        if ((null != getPrevContentAsJsonObject()) && (null == prev_content_as_string)) {
            prev_content_as_string = getPrevContentAsJsonObject().toString();
        }

        if ((null != unsigned) && (null != unsigned.prev_content)) {
            unsigned.prev_content = null;
        }
    }

    /**
     * Deserialize the event.
     */
    private void finalizeDeserialization() {
        if ((null != contentAsString) && (null == content)) {
            try {
                content = new JsonParser().parse(contentAsString).getAsJsonObject();
            } catch (Exception e) {
                Log.e(LOG_TAG, "finalizeDeserialization : contentAsString deserialization " + e.getMessage(), e);
                contentAsString = null;
            }
        }

        if ((null != prev_content_as_string) && (null == prev_content)) {
            try {
                prev_content = new JsonParser().parse(prev_content_as_string).getAsJsonObject();
            } catch (Exception e) {
                Log.e(LOG_TAG, "finalizeDeserialization : prev_content_as_string deserialization " + e.getMessage(), e);
                prev_content_as_string = null;
            }
        }
    }

    /**
     * Filter a JsonObject to keep only the allowed keys.
     *
     * @param aContent    the JsonObject to filter.
     * @param allowedKeys the allowed keys list.
     * @return the filtered JsonObject
     */
    private static JsonObject filterInContentWithKeys(JsonObject aContent, List<String> allowedKeys) {
        // sanity check
        if (null == aContent) {
            return null;
        }

        JsonObject filteredContent = new JsonObject();

        // remove any key
        if ((null == allowedKeys) || (0 == allowedKeys.size())) {
            return new JsonObject();
        }

        Set<Map.Entry<String, JsonElement>> entries = aContent.entrySet();

        if (null != entries) {
            for (Map.Entry<String, JsonElement> entry : entries) {
                if (allowedKeys.indexOf(entry.getKey()) >= 0) {
                    filteredContent.add(entry.getKey(), entry.getValue());
                }
            }
        }

        return filteredContent;
    }

    /**
     * Prune the event which removes all keys we don't know about or think could potentially be dodgy.
     * This is used when we "redact" an event. We want to remove all fields that the user has specified,
     * but we do want to keep necessary information like type, state_key etc.
     *
     * @param redactionEvent the event which triggers this redaction
     */
    public void prune(Event redactionEvent) {
        // Filter in event by keeping only the following keys
        List<String> allowedKeys;

        // Add filtered content, allowed keys in content depends on the event type
        if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_MEMBER, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("membership"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_CREATE, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("creator"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_JOIN_RULES, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("join_rule"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_POWER_LEVELS, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("users",
                    "users_default",
                    "events",
                    "events_default",
                    "state_default",
                    "ban",
                    "kick",
                    "redact",
                    "invite"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_ROOM_ALIASES, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("aliases"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_STATE_CANONICAL_ALIAS, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("alias"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_FEEDBACK, type)) {
            allowedKeys = new ArrayList<>(Arrays.asList("type", "target_event_id"));
        } else if (TextUtils.equals(Event.EVENT_TYPE_MESSAGE_ENCRYPTED, type)) {
            mClearEvent = null;
            allowedKeys = null;
        } else {
            allowedKeys = null;
        }

        content = filterInContentWithKeys(getContentAsJsonObject(), allowedKeys);
        prev_content = filterInContentWithKeys(getPrevContentAsJsonObject(), allowedKeys);

        prev_content_as_string = null;
        contentAsString = null;

        if (null != redactionEvent) {
            if (null == unsigned) {
                unsigned = new UnsignedData();
            }

            unsigned.redacted_because = new RedactedBecause();
            unsigned.redacted_because.type = redactionEvent.type;
            unsigned.redacted_because.origin_server_ts = redactionEvent.originServerTs;
            unsigned.redacted_because.sender = redactionEvent.sender;
            unsigned.redacted_because.event_id = redactionEvent.eventId;
            unsigned.redacted_because.unsigned = redactionEvent.unsigned;
            unsigned.redacted_because.redacts = redactionEvent.redacts;

            unsigned.redacted_because.content = new RedactedContent();

            JsonObject contentAsJson = getContentAsJsonObject();
            if ((null != contentAsJson) && contentAsJson.has("reason")) {
                try {
                    unsigned.redacted_because.content.reason = contentAsJson.get("reason").getAsString();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "unsigned.redacted_because.content.reason failed " + e.getMessage(), e);
                }

            }
        }
    }

    //==============================================================================================================
    // Crypto
    //==============================================================================================================

    /**
     * For encrypted events, the plaintext payload for the event.
     * This is a small MXEvent instance with typically value for `type` and 'content' fields.
     */
    private transient Event mClearEvent;

    /**
     * Curve25519 key which we believe belongs to the sender of the event.
     * See `senderKey` property.
     */
    private transient String mSenderCurve25519Key;

    /**
     * Ed25519 key which the sender of this event (for olm) or the creator of the megolm session (for megolm) claims to own.
     * See `claimedEd25519Key` property.
     */
    private transient String mClaimedEd25519Key;

    /**
     * Curve25519 keys of devices involved in telling us about the senderCurve25519Key and claimedEd25519Key.
     * See `forwardingCurve25519KeyChain` property.
     */
    private transient List<String> mForwardingCurve25519KeyChain = new ArrayList<>();

    /**
     * Decryption error
     */
    private MXCryptoError mCryptoError;

    /**
     * @return true if this event is encrypted.
     */
    public boolean isEncrypted() {
        return TextUtils.equals(getWireType(), EVENT_TYPE_MESSAGE_ENCRYPTED);
    }

    /**
     * Update the clear data on this event.
     * This is used after decrypting an event; it should not be used by applications.
     * It fires kMXEventDidDecryptNotification.
     *
     * @param decryptionResult the decryption result, including the plaintext and some key info.
     */
    public void setClearData(@Nullable MXEventDecryptionResult decryptionResult) {
        mClearEvent = null;

        if (null != decryptionResult) {
            if (null != decryptionResult.mClearEvent) {
                mClearEvent = JsonUtils.toEvent(decryptionResult.mClearEvent);
            }

            if (null != mClearEvent) {
                mClearEvent.mSenderCurve25519Key = decryptionResult.mSenderCurve25519Key;
                mClearEvent.mClaimedEd25519Key = decryptionResult.mClaimedEd25519Key;

                if (null != decryptionResult.mForwardingCurve25519KeyChain) {
                    mClearEvent.mForwardingCurve25519KeyChain = decryptionResult.mForwardingCurve25519KeyChain;
                } else {
                    mClearEvent.mForwardingCurve25519KeyChain = new ArrayList<>();
                }

                try {
                    // Add "m.relates_to" data from e2e event to the unencrypted event
                    if (getWireContent().getAsJsonObject().has("m.relates_to")) {
                        mClearEvent.getContentAsJsonObject()
                                .add("m.relates_to", getWireContent().getAsJsonObject().get("m.relates_to"));
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to restore 'm.relates_to' the clear event", e);
                }
            }

            mCryptoError = null;
        }
    }

    /**
     * @return The curve25519 key that sent this event.
     */
    public String senderKey() {
        if (null != mClearEvent) {
            return mClearEvent.mSenderCurve25519Key;
        } else {
            return mSenderCurve25519Key;
        }
    }

    /**
     * @return The additional keys the sender of this encrypted event claims to possess.
     */
    public Map<String, String> getKeysClaimed() {
        Map<String, String> res = new HashMap<>();

        String claimedEd25519Key = (null != getClearEvent()) ? getClearEvent().mClaimedEd25519Key : mClaimedEd25519Key;

        if (null != claimedEd25519Key) {
            res.put("ed25519", claimedEd25519Key);
        }

        return res;
    }

    /**
     * @return the claimed Ed25519 key
     */
    /*public String getClaimedEd25519Key() {
        if (null != mClearEvent) {
            return mClearEvent.mClaimedEd25519Key;
        } else {
            return mClaimedEd25519Key;
        }
    }*/

    /**
     * @return Get the curve25519 keys of the devices which were involved in telling us about the claimedEd25519Key and sender curve25519 key.
     */
    /*public List<String> getForwardingCurve25519KeyChain() {
        List<String> res = (null != mClearEvent) ? mClearEvent.mForwardingCurve25519KeyChain : mForwardingCurve25519KeyChain;

        if (null == res) {
            res = new ArrayList<>();
        }

        return res;
    }*/

    /**
     * @return the linked crypto error
     */
    public MXCryptoError getCryptoError() {
        return mCryptoError;
    }

    /**
     * Update the linked crypto error
     *
     * @param error the new crypto error.
     */
    public void setCryptoError(MXCryptoError error) {
        mCryptoError = error;
        if (null != error) {
            mClearEvent = null;
        }
    }

    /**
     * @return the clear event
     */
    public Event getClearEvent() {
        return mClearEvent;
    }
}
