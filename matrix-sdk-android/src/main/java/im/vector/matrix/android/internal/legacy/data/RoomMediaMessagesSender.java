/*
 * Copyright 2016 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;

import im.vector.matrix.android.R;
import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.crypto.MXEncryptedAttachments;
import im.vector.matrix.android.internal.legacy.db.MXMediasCache;
import im.vector.matrix.android.internal.legacy.listeners.MXMediaUploadListener;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.message.AudioMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.FileMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.ImageMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.MediaMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.message.RelatesTo;
import im.vector.matrix.android.internal.legacy.rest.model.message.VideoMessage;
import im.vector.matrix.android.internal.legacy.util.ImageUtils;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.PermalinkUtils;
import im.vector.matrix.android.internal.legacy.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Room helper to send media messages in the right order.
 */
class RoomMediaMessagesSender {
    private static final String LOG_TAG = RoomMediaMessagesSender.class.getSimpleName();

    // pending events list
    private final List<RoomMediaMessage> mPendingRoomMediaMessages = new ArrayList<>();

    // linked room
    private final Room mRoom;

    // data handler
    private final MXDataHandler mDataHandler;

    // linked context
    private final Context mContext;

    // the sending item
    private RoomMediaMessage mSendingRoomMediaMessage;

    // UI thread
    private static android.os.Handler mUiHandler = null;

    // events creation threads
    private static android.os.Handler mEventHandler = null;

    // encoding creation threads
    private static android.os.Handler mEncodingHandler = null;

    /**
     * Constructor
     *
     * @param context     the context
     * @param dataHandler the dataHanlder
     * @param room        the room
     */
    RoomMediaMessagesSender(Context context, MXDataHandler dataHandler, Room room) {
        mRoom = room;
        mContext = context.getApplicationContext();
        mDataHandler = dataHandler;

        if (null == mUiHandler) {
            mUiHandler = new android.os.Handler(Looper.getMainLooper());

            HandlerThread eventHandlerThread = new HandlerThread("RoomDataItemsSender_event", Thread.MIN_PRIORITY);
            eventHandlerThread.start();
            mEventHandler = new android.os.Handler(eventHandlerThread.getLooper());

            HandlerThread encodingHandlerThread = new HandlerThread("RoomDataItemsSender_encoding", Thread.MIN_PRIORITY);
            encodingHandlerThread.start();
            mEncodingHandler = new android.os.Handler(encodingHandlerThread.getLooper());
        }
    }

    /**
     * Send a new media message to the room
     *
     * @param roomMediaMessage the message to send
     */
    void send(final RoomMediaMessage roomMediaMessage) {
        mEventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null == roomMediaMessage.getEvent()) {
                    Message message;
                    String mimeType = roomMediaMessage.getMimeType(mContext);

                    // avoid null case
                    if (null == mimeType) {
                        mimeType = "";
                    }

                    if (null == roomMediaMessage.getUri()) {
                        message = buildTextMessage(roomMediaMessage);
                    } else if (mimeType.startsWith("image/")) {
                        message = buildImageMessage(roomMediaMessage);
                    } else if (mimeType.startsWith("video/")) {
                        message = buildVideoMessage(roomMediaMessage);
                    } else {
                        message = buildFileMessage(roomMediaMessage);
                    }

                    if (null == message) {
                        Log.e(LOG_TAG, "## send " + roomMediaMessage + " not supported");


                        mUiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                roomMediaMessage.onEventCreationFailed("not supported " + roomMediaMessage);
                            }
                        });
                        return;
                    }

                    roomMediaMessage.setMessageType(message.msgtype);

                    if (roomMediaMessage.getReplyToEvent() != null) {
                        // Note: it is placed here, but may be moved to the outer event during the encryption of the content
                        message.relatesTo = new RelatesTo();
                        message.relatesTo.dict = new HashMap<>();
                        message.relatesTo.dict.put("event_id", roomMediaMessage.getReplyToEvent().eventId);
                    }

                    Event event = new Event(message, mDataHandler.getUserId(), mRoom.getRoomId());

                    roomMediaMessage.setEvent(event);
                }

                mDataHandler.updateEventState(roomMediaMessage.getEvent(), Event.SentState.UNSENT);
                mRoom.storeOutgoingEvent(roomMediaMessage.getEvent());
                mDataHandler.getStore().commit();

                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        roomMediaMessage.onEventCreated();
                    }
                });

                synchronized (LOG_TAG) {
                    if (!mPendingRoomMediaMessages.contains(roomMediaMessage)) {
                        mPendingRoomMediaMessages.add(roomMediaMessage);
                    }
                }

                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // send the item
                        sendNext();
                    }
                });
            }
        });
    }

    /**
     * Skip the sending media item.
     */
    private void skip() {
        synchronized (LOG_TAG) {
            mSendingRoomMediaMessage = null;
        }

        sendNext();
    }

    /**
     * Send the next pending item
     */
    private void sendNext() {
        RoomMediaMessage roomMediaMessage;

        synchronized (LOG_TAG) {
            // please wait
            if (null != mSendingRoomMediaMessage) {
                return;
            }

            if (!mPendingRoomMediaMessages.isEmpty()) {
                mSendingRoomMediaMessage = mPendingRoomMediaMessages.get(0);
                mPendingRoomMediaMessages.remove(0);
            } else {
                // nothing to do
                return;
            }

            roomMediaMessage = mSendingRoomMediaMessage;
        }

        // upload the medias first
        if (uploadMedias(roomMediaMessage)) {
            return;
        }

        // send the event
        sendEvent(roomMediaMessage.getEvent());
    }

    /**
     * Send the event after uploading the medias
     *
     * @param event the event to send
     */
    private void sendEvent(final Event event) {
        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                // nothing more to upload
                mRoom.sendEvent(event, new ApiCallback<Void>() {
                    private ApiCallback<Void> getCallback() {
                        ApiCallback<Void> callback;

                        synchronized (LOG_TAG) {
                            callback = mSendingRoomMediaMessage.getSendingCallback();
                            mSendingRoomMediaMessage.setEventSendingCallback(null);
                            mSendingRoomMediaMessage = null;
                        }

                        return callback;
                    }

                    @Override
                    public void onSuccess(Void info) {
                        ApiCallback<Void> callback = getCallback();

                        if (null != callback) {
                            try {
                                callback.onSuccess(null);
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "## sendNext() failed " + e.getMessage(), e);
                            }
                        }

                        sendNext();
                    }

                    @Override
                    public void onNetworkError(Exception e) {
                        ApiCallback<Void> callback = getCallback();

                        if (null != callback) {
                            try {
                                callback.onNetworkError(e);
                            } catch (Exception e2) {
                                Log.e(LOG_TAG, "## sendNext() failed " + e2.getMessage(), e2);
                            }
                        }

                        sendNext();
                    }

                    @Override
                    public void onMatrixError(MatrixError e) {
                        ApiCallback<Void> callback = getCallback();

                        if (null != callback) {
                            try {
                                callback.onMatrixError(e);
                            } catch (Exception e2) {
                                Log.e(LOG_TAG, "## sendNext() failed " + e2.getMessage(), e2);
                            }
                        }

                        sendNext();
                    }

                    @Override
                    public void onUnexpectedError(Exception e) {
                        ApiCallback<Void> callback = getCallback();

                        if (null != callback) {
                            try {
                                callback.onUnexpectedError(e);
                            } catch (Exception e2) {
                                Log.e(LOG_TAG, "## sendNext() failed " + e2.getMessage(), e2);
                            }
                        }

                        sendNext();
                    }
                });
            }
        });
    }

    //==============================================================================================================
    // Messages builder methods.
    //==============================================================================================================

    /**
     * Build a text message from a RoomMediaMessage.
     *
     * @param roomMediaMessage the RoomMediaMessage.
     * @return the message
     */
    private Message buildTextMessage(RoomMediaMessage roomMediaMessage) {
        CharSequence sequence = roomMediaMessage.getText();
        String htmlText = roomMediaMessage.getHtmlText();
        String text = null;

        if (null == sequence) {
            if (null != htmlText) {
                text = Html.fromHtml(htmlText).toString();
            }
        } else {
            text = sequence.toString();
        }

        // a text message cannot be null
        if (TextUtils.isEmpty(text) && !TextUtils.equals(roomMediaMessage.getMessageType(), Message.MSGTYPE_EMOTE)) {
            return null;
        }

        Message message = new Message();
        message.msgtype = (null == roomMediaMessage.getMessageType()) ? Message.MSGTYPE_TEXT : roomMediaMessage.getMessageType();
        message.body = text;

        // an emote can have an empty body
        if (null == message.body) {
            message.body = "";
        }

        if (!TextUtils.isEmpty(htmlText)) {
            message.formatted_body = htmlText;
            message.format = Message.FORMAT_MATRIX_HTML;
        }

        // Deals with in reply to event
        Event replyToEvent = roomMediaMessage.getReplyToEvent();
        if (replyToEvent != null) {
            // Cf. https://docs.google.com/document/d/1BPd4lBrooZrWe_3s_lHw_e-Dydvc7bXbm02_sV2k6Sc
            String msgType = JsonUtils.getMessageMsgType(replyToEvent.getContentAsJsonObject());

            // Build body and formatted body, depending of the `msgtype` of the event the user is replying to
            if (msgType != null) {
                // Compute the content of the event user is replying to
                String replyToBody;
                String replyToFormattedBody;
                boolean replyToEventIsAlreadyAReply = false;

                switch (msgType) {
                    case Message.MSGTYPE_TEXT:
                    case Message.MSGTYPE_NOTICE:
                    case Message.MSGTYPE_EMOTE:
                        Message messageToReplyTo = JsonUtils.toMessage(replyToEvent.getContentAsJsonObject());

                        replyToBody = messageToReplyTo.body;

                        if (TextUtils.isEmpty(messageToReplyTo.formatted_body)) {
                            replyToFormattedBody = messageToReplyTo.body;
                        } else {
                            replyToFormattedBody = messageToReplyTo.formatted_body;
                        }

                        replyToEventIsAlreadyAReply = messageToReplyTo.relatesTo != null
                                && messageToReplyTo.relatesTo.dict != null
                                && !TextUtils.isEmpty(messageToReplyTo.relatesTo.dict.get("event_id"));

                        break;
                    case Message.MSGTYPE_IMAGE:
                        replyToBody = mContext.getString(R.string.reply_to_an_image);
                        replyToFormattedBody = replyToBody;
                        break;
                    case Message.MSGTYPE_VIDEO:
                        replyToBody = mContext.getString(R.string.reply_to_a_video);
                        replyToFormattedBody = replyToBody;
                        break;
                    case Message.MSGTYPE_AUDIO:
                        replyToBody = mContext.getString(R.string.reply_to_an_audio_file);
                        replyToFormattedBody = replyToBody;
                        break;
                    case Message.MSGTYPE_FILE:
                        replyToBody = mContext.getString(R.string.reply_to_a_file);
                        replyToFormattedBody = replyToBody;
                        break;
                    default:
                        // Other msg types are not supported yet
                        Log.w(LOG_TAG, "Reply to: unsupported msgtype: " + msgType);
                        replyToBody = null;
                        replyToFormattedBody = null;
                        break;
                }

                if (replyToBody != null) {
                    String replyContent;
                    if (TextUtils.isEmpty(message.formatted_body)) {
                        replyContent = message.body;
                    } else {
                        replyContent = message.formatted_body;
                    }

                    message.body = includeReplyToToBody(replyToEvent,
                            replyToBody,
                            replyToEventIsAlreadyAReply,
                            message.body,
                            msgType.equals(Message.MSGTYPE_EMOTE));
                    message.formatted_body = includeReplyToToFormattedBody(replyToEvent,
                            replyToFormattedBody,
                            replyToEventIsAlreadyAReply,
                            replyContent,
                            msgType.equals(Message.MSGTYPE_EMOTE));

                    // Note: we need to force the format to Message.FORMAT_MATRIX_HTML
                    message.format = Message.FORMAT_MATRIX_HTML;
                } else {
                    Log.e(LOG_TAG, "Unsupported 'msgtype': " + msgType + ". Consider calling Room.canReplyTo(Event)");

                    // Ensure there will not be "m.relates_to" data in the sent event
                    roomMediaMessage.setReplyToEvent(null);
                }
            } else {
                Log.e(LOG_TAG, "Null 'msgtype'. Consider calling Room.canReplyTo(Event)");

                // Ensure there will not be "m.relates_to" data in the sent event
                roomMediaMessage.setReplyToEvent(null);
            }
        }

        return message;
    }

    private String includeReplyToToBody(Event replyToEvent,
                                        String replyToBody,
                                        boolean stripPreviousReplyTo,
                                        String messageBody,
                                        boolean isEmote) {
        int firstLineIndex = 0;

        String[] lines = replyToBody.split("\n");

        if (stripPreviousReplyTo) {
            // Strip replyToBody from previous reply to

            // Strip line starting with "> "
            while (firstLineIndex < lines.length && lines[firstLineIndex].startsWith("> ")) {
                firstLineIndex++;
            }

            // Strip empty line after
            if (firstLineIndex < lines.length && lines[firstLineIndex].isEmpty()) {
                firstLineIndex++;
            }
        }

        StringBuilder ret = new StringBuilder();

        if (firstLineIndex < lines.length) {
            // Add <${mxid}> to the first line
            if (isEmote) {
                lines[firstLineIndex] = "* <" + replyToEvent.sender + "> " + lines[firstLineIndex];
            } else {
                lines[firstLineIndex] = "<" + replyToEvent.sender + "> " + lines[firstLineIndex];
            }

            for (int i = firstLineIndex; i < lines.length; i++) {
                ret.append("> ")
                        .append(lines[i])
                        .append("\n");
            }
        }

        ret.append("\n")
                .append(messageBody);

        return ret.toString();
    }

    private String includeReplyToToFormattedBody(Event replyToEvent,
                                                 String replyToFormattedBody,
                                                 boolean stripPreviousReplyTo,
                                                 String messageFormattedBody,
                                                 boolean isEmote) {
        if (stripPreviousReplyTo) {
            // Strip replyToFormattedBody from previous reply to
            replyToFormattedBody = replyToFormattedBody.replaceAll("^<mx-reply>.*</mx-reply>", "");
        }

        StringBuilder ret = new StringBuilder("<mx-reply><blockquote><a href=\"")
                // ${evLink}
                .append(PermalinkUtils.createPermalink(replyToEvent))
                .append("\">")
                // "In reply to"
                .append(mContext.getString(R.string.message_reply_to_prefix))
                .append("</a> ");

        if (isEmote) {
            ret.append("* ");
        }

        ret.append("<a href=\"")
                // ${userLink}
                .append(PermalinkUtils.createPermalink(replyToEvent.sender))
                .append("\">")
                // ${mxid}
                .append(replyToEvent.sender)
                .append("</a><br>")
                .append(replyToFormattedBody)
                .append("</blockquote></mx-reply>")
                .append(messageFormattedBody);

        return ret.toString();
    }

    /**
     * Returns the thumbnail path of shot image.
     *
     * @param picturePath the image path
     * @return the thumbnail image path.
     */
    private static String getThumbnailPath(String picturePath) {
        if (!TextUtils.isEmpty(picturePath) && picturePath.endsWith(".jpg")) {
            return picturePath.replace(".jpg", "_thumb.jpg");
        }

        return null;
    }

    /**
     * Retrieves the image thumbnail saved by the medias picker.
     *
     * @param sharedDataItem the sharedItem
     * @return the thumbnail if it exits.
     */
    private Bitmap getMediasPickerThumbnail(RoomMediaMessage sharedDataItem) {
        Bitmap thumbnailBitmap = null;

        try {
            String thumbPath = getThumbnailPath(sharedDataItem.getUri().getPath());

            if (null != thumbPath) {
                File thumbFile = new File(thumbPath);

                if (thumbFile.exists()) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    thumbnailBitmap = BitmapFactory.decodeFile(thumbPath, options);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "cannot restore the medias picker thumbnail " + e.getMessage(), e);
        } catch (OutOfMemoryError oom) {
            Log.e(LOG_TAG, "cannot restore the medias picker thumbnail oom", oom);
        }

        return thumbnailBitmap;
    }

    /**
     * Retrieve the media Url.
     *
     * @param roomMediaMessage the room media message
     * @return the media URL
     */
    private String getMediaUrl(RoomMediaMessage roomMediaMessage) {
        String mediaUrl = roomMediaMessage.getUri().toString();

        if (!mediaUrl.startsWith("file:")) {
            // save the content:// file in to the medias cache
            String mimeType = roomMediaMessage.getMimeType(mContext);
            ResourceUtils.Resource resource = ResourceUtils.openResource(mContext, roomMediaMessage.getUri(), mimeType);

            // save the file in the filesystem
            mediaUrl = mDataHandler.getMediasCache().saveMedia(resource.mContentStream, null, mimeType);
            resource.close();
        }

        return mediaUrl;
    }

    /**
     * Build an image message from a RoomMediaMessage.
     *
     * @param roomMediaMessage the roomMediaMessage
     * @return the image message
     */
    private Message buildImageMessage(RoomMediaMessage roomMediaMessage) {
        try {
            String mimeType = roomMediaMessage.getMimeType(mContext);
            final MXMediasCache mediasCache = mDataHandler.getMediasCache();

            String mediaUrl = getMediaUrl(roomMediaMessage);

            // compute the thumbnail
            Bitmap thumbnailBitmap = roomMediaMessage.getFullScreenImageKindThumbnail(mContext);

            if (null == thumbnailBitmap) {
                thumbnailBitmap = getMediasPickerThumbnail(roomMediaMessage);
            }

            if (null == thumbnailBitmap) {
                Pair<Integer, Integer> thumbnailSize = roomMediaMessage.getThumbnailSize();
                thumbnailBitmap = ResourceUtils.createThumbnailBitmap(mContext, roomMediaMessage.getUri(), thumbnailSize.first, thumbnailSize.second);
            }

            if (null == thumbnailBitmap) {
                thumbnailBitmap = roomMediaMessage.getMiniKindImageThumbnail(mContext);
            }

            String thumbnailURL = null;

            if (null != thumbnailBitmap) {
                thumbnailURL = mediasCache.saveBitmap(thumbnailBitmap, null);
            }

            // get the exif rotation angle
            final int rotationAngle = ImageUtils.getRotationAngleForBitmap(mContext, Uri.parse(mediaUrl));

            if (0 != rotationAngle) {
                // always apply the rotation to the image
                ImageUtils.rotateImage(mContext, thumbnailURL, rotationAngle, mediasCache);
            }

            ImageMessage imageMessage = new ImageMessage();
            imageMessage.url = mediaUrl;
            imageMessage.body = roomMediaMessage.getFileName(mContext);

            if (TextUtils.isEmpty(imageMessage.body)) {
                imageMessage.body = "Image";
            }

            Uri imageUri = Uri.parse(mediaUrl);

            if (null == imageMessage.info) {
                Room.fillImageInfo(mContext, imageMessage, imageUri, mimeType);
            }

            if ((null != thumbnailURL) && (null != imageMessage.info) && (null == imageMessage.info.thumbnailInfo)) {
                Uri thumbUri = Uri.parse(thumbnailURL);
                Room.fillThumbnailInfo(mContext, imageMessage, thumbUri, "image/jpeg");
                imageMessage.info.thumbnailUrl = thumbnailURL;
            }

            return imageMessage;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## buildImageMessage() failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Compute the video thumbnail
     *
     * @param videoUrl the video url
     * @return the video thumbnail
     */
    public String getVideoThumbnailUrl(final String videoUrl) {
        String thumbUrl = null;
        try {
            Uri uri = Uri.parse(videoUrl);
            Bitmap thumb = ThumbnailUtils.createVideoThumbnail(uri.getPath(), MediaStore.Images.Thumbnails.MINI_KIND);
            thumbUrl = mDataHandler.getMediasCache().saveBitmap(thumb, null);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getVideoThumbnailUrl() failed with " + e.getMessage(), e);
        }

        return thumbUrl;
    }

    /**
     * Build an video message from a RoomMediaMessage.
     *
     * @param roomMediaMessage the roomMediaMessage
     * @return the video message
     */
    private Message buildVideoMessage(RoomMediaMessage roomMediaMessage) {
        try {
            String mediaUrl = getMediaUrl(roomMediaMessage);
            String thumbnailUrl = getVideoThumbnailUrl(mediaUrl);

            if (null == thumbnailUrl) {
                return buildFileMessage(roomMediaMessage);
            }

            VideoMessage videoMessage = new VideoMessage();
            videoMessage.url = mediaUrl;
            videoMessage.body = roomMediaMessage.getFileName(mContext);

            Uri videoUri = Uri.parse(mediaUrl);
            Uri thumbnailUri = (null != thumbnailUrl) ? Uri.parse(thumbnailUrl) : null;
            Room.fillVideoInfo(mContext, videoMessage, videoUri, roomMediaMessage.getMimeType(mContext), thumbnailUri, "image/jpeg");

            if (null == videoMessage.body) {
                videoMessage.body = videoUri.getLastPathSegment();
            }

            return videoMessage;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## buildVideoMessage() failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Build an file message from a RoomMediaMessage.
     *
     * @param roomMediaMessage the roomMediaMessage
     * @return the video message
     */
    private Message buildFileMessage(RoomMediaMessage roomMediaMessage) {
        try {
            String mimeType = roomMediaMessage.getMimeType(mContext);

            String mediaUrl = getMediaUrl(roomMediaMessage);
            FileMessage fileMessage;

            if (mimeType.startsWith("audio/")) {
                fileMessage = new AudioMessage();
            } else {
                fileMessage = new FileMessage();
            }

            fileMessage.url = mediaUrl;
            fileMessage.body = roomMediaMessage.getFileName(mContext);
            Uri uri = Uri.parse(mediaUrl);
            Room.fillFileInfo(mContext, fileMessage, uri, mimeType);

            if (null == fileMessage.body) {
                fileMessage.body = uri.getLastPathSegment();
            }

            return fileMessage;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## buildFileMessage() failed " + e.getMessage(), e);
        }

        return null;
    }

    //==============================================================================================================
    // Upload medias management
    //==============================================================================================================

    /**
     * Upload the medias.
     *
     * @param roomMediaMessage the roomMediaMessage
     * @return true if a media is uploaded
     */
    private boolean uploadMedias(final RoomMediaMessage roomMediaMessage) {
        final Event event = roomMediaMessage.getEvent();
        final Message message = JsonUtils.toMessage(event.getContent());

        if (!(message instanceof MediaMessage)) {
            return false;
        }

        final MediaMessage mediaMessage = (MediaMessage) message;
        final String url;
        final String fMimeType;

        if (mediaMessage.isThumbnailLocalContent()) {
            url = mediaMessage.getThumbnailUrl();
            fMimeType = "image/jpeg";
        } else if (mediaMessage.isLocalContent()) {
            url = mediaMessage.getUrl();
            fMimeType = mediaMessage.getMimeType();
        } else {
            return false;
        }

        mEncodingHandler.post(new Runnable() {
            @Override
            public void run() {
                final MXMediasCache mediasCache = mDataHandler.getMediasCache();

                Uri uri = Uri.parse(url);
                String mimeType = fMimeType;
                final MXEncryptedAttachments.EncryptionResult encryptionResult;
                final Uri encryptedUri;
                InputStream stream;

                String filename = null;

                try {
                    stream = new FileInputStream(new File(uri.getPath()));
                    if (mRoom.isEncrypted() && mDataHandler.isCryptoEnabled() && (null != stream)) {
                        encryptionResult = MXEncryptedAttachments.encryptAttachment(stream, mimeType);
                        stream.close();

                        if (null != encryptionResult) {
                            mimeType = "application/octet-stream";
                            encryptedUri = Uri.parse(mediasCache.saveMedia(encryptionResult.mEncryptedStream, null, fMimeType));
                            File file = new File(encryptedUri.getPath());
                            stream = new FileInputStream(file);
                        } else {
                            skip();

                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mDataHandler.updateEventState(roomMediaMessage.getEvent(), Event.SentState.UNDELIVERED);
                                    mRoom.storeOutgoingEvent(roomMediaMessage.getEvent());
                                    mDataHandler.getStore().commit();

                                    roomMediaMessage.onEncryptionFailed();
                                }
                            });

                            return;
                        }
                    } else {
                        // Only pass filename string to server in non-encrypted rooms to prevent leaking filename
                        filename = mediaMessage.isThumbnailLocalContent() ? ("thumb" + message.body) : message.body;
                        encryptionResult = null;
                        encryptedUri = null;
                    }
                } catch (Exception e) {
                    skip();
                    return;
                }

                mDataHandler.updateEventState(roomMediaMessage.getEvent(), Event.SentState.SENDING);

                mediasCache.uploadContent(stream, filename, mimeType, url,
                        new MXMediaUploadListener() {
                            @Override
                            public void onUploadStart(final String uploadId) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (null != roomMediaMessage.getMediaUploadListener()) {
                                            roomMediaMessage.getMediaUploadListener().onUploadStart(uploadId);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onUploadCancel(final String uploadId) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDataHandler.updateEventState(roomMediaMessage.getEvent(), Event.SentState.UNDELIVERED);

                                        if (null != roomMediaMessage.getMediaUploadListener()) {
                                            roomMediaMessage.getMediaUploadListener().onUploadCancel(uploadId);
                                            roomMediaMessage.setMediaUploadListener(null);
                                            roomMediaMessage.setEventSendingCallback(null);
                                        }

                                        skip();
                                    }
                                });
                            }

                            @Override
                            public void onUploadError(final String uploadId, final int serverResponseCode, final String serverErrorMessage) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDataHandler.updateEventState(roomMediaMessage.getEvent(), Event.SentState.UNDELIVERED);

                                        if (null != roomMediaMessage.getMediaUploadListener()) {
                                            roomMediaMessage.getMediaUploadListener().onUploadError(uploadId, serverResponseCode, serverErrorMessage);
                                            roomMediaMessage.setMediaUploadListener(null);
                                            roomMediaMessage.setEventSendingCallback(null);
                                        }

                                        skip();
                                    }
                                });
                            }

                            @Override
                            public void onUploadComplete(final String uploadId, final String contentUri) {
                                mUiHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean isThumbnailUpload = mediaMessage.isThumbnailLocalContent();

                                        if (isThumbnailUpload) {
                                            mediaMessage.setThumbnailUrl(encryptionResult, contentUri);

                                            if (null != encryptionResult) {
                                                mediasCache.saveFileMediaForUrl(contentUri, encryptedUri.toString(), -1, -1, "image/jpeg");
                                                try {
                                                    new File(Uri.parse(url).getPath()).delete();
                                                } catch (Exception e) {
                                                    Log.e(LOG_TAG, "## cannot delete the uncompress media", e);
                                                }
                                            } else {
                                                Pair<Integer, Integer> thumbnailSize = roomMediaMessage.getThumbnailSize();
                                                mediasCache.saveFileMediaForUrl(contentUri, url, thumbnailSize.first, thumbnailSize.second, "image/jpeg");
                                            }

                                            // update the event content with the new message info
                                            event.updateContent(JsonUtils.toJson(message));

                                            // force to save the room events list
                                            // https://github.com/vector-im/riot-android/issues/1390
                                            mDataHandler.getStore().flushRoomEvents(mRoom.getRoomId());

                                            // upload the media
                                            uploadMedias(roomMediaMessage);
                                        } else {
                                            if (null != encryptedUri) {
                                                // replace the thumbnail and the media contents by the computed one
                                                mediasCache.saveFileMediaForUrl(contentUri, encryptedUri.toString(), mediaMessage.getMimeType());
                                                try {
                                                    new File(Uri.parse(url).getPath()).delete();
                                                } catch (Exception e) {
                                                    Log.e(LOG_TAG, "## cannot delete the uncompress media", e);
                                                }
                                            } else {
                                                // replace the thumbnail and the media contents by the computed one
                                                mediasCache.saveFileMediaForUrl(contentUri, url, mediaMessage.getMimeType());
                                            }
                                            mediaMessage.setUrl(encryptionResult, contentUri);

                                            // update the event content with the new message info
                                            event.updateContent(JsonUtils.toJson(message));

                                            // force to save the room events list
                                            // https://github.com/vector-im/riot-android/issues/1390
                                            mDataHandler.getStore().flushRoomEvents(mRoom.getRoomId());

                                            Log.d(LOG_TAG, "Uploaded to " + contentUri);

                                            // send
                                            sendEvent(event);
                                        }

                                        if (null != roomMediaMessage.getMediaUploadListener()) {
                                            roomMediaMessage.getMediaUploadListener().onUploadComplete(uploadId, contentUri);

                                            if (!isThumbnailUpload) {
                                                roomMediaMessage.setMediaUploadListener(null);
                                            }
                                        }
                                    }
                                });
                            }
                        });
            }
        });

        return true;
    }
}
