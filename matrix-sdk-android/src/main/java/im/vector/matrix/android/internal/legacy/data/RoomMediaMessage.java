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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.webkit.MimeTypeMap;

import im.vector.matrix.android.internal.legacy.listeners.IMXMediaUploadListener;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.util.JsonUtils;
import im.vector.matrix.android.internal.legacy.util.Log;
import im.vector.matrix.android.internal.legacy.util.ResourceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * RoomMediaMessage encapsulates the media information to be sent.
 */
public class RoomMediaMessage implements Parcelable {
    private static final String LOG_TAG = RoomMediaMessage.class.getSimpleName();

    private static final Uri mDummyUri = Uri.parse("http://www.matrixdummy.org");

    /**
     * Interface to monitor event creation.
     */
    public interface EventCreationListener {
        /**
         * The dedicated event has been created and added to the events list.
         *
         * @param roomMediaMessage the room media message.
         */
        void onEventCreated(RoomMediaMessage roomMediaMessage);

        /**
         * The event creation failed.
         *
         * @param roomMediaMessage the room media message.
         * @param errorMessage     the failure reason
         */
        void onEventCreationFailed(RoomMediaMessage roomMediaMessage, String errorMessage);

        /**
         * The media encryption failed.
         *
         * @param roomMediaMessage the room media message.
         */
        void onEncryptionFailed(RoomMediaMessage roomMediaMessage);
    }

    // the item is defined either from an uri
    private Uri mUri;
    private String mMimeType;

    // the message to send
    private Event mEvent;

    // or a clipData Item
    private ClipData.Item mClipDataItem;

    // the filename
    private String mFileName;

    // Message.MSGTYPE_XX value
    private String mMessageType;

    // The replyTo event
    @Nullable
    private Event mReplyToEvent;

    // thumbnail size
    private Pair<Integer, Integer> mThumbnailSize = new Pair<>(100, 100);

    // upload media upload listener
    private transient IMXMediaUploadListener mMediaUploadListener;

    // event sending callback
    private transient ApiCallback<Void> mEventSendingCallback;

    // event creation listener
    private transient EventCreationListener mEventCreationListener;

    /**
     * Constructor from a ClipData.Item.
     * It might be used by a third party medias selection.
     *
     * @param clipDataItem the data item
     * @param mimeType     the mime type
     */
    public RoomMediaMessage(ClipData.Item clipDataItem, String mimeType) {
        mClipDataItem = clipDataItem;
        mMimeType = mimeType;
    }

    /**
     * Constructor for a text message.
     *
     * @param text     the text
     * @param htmlText the HTML text
     * @param format   the formatted text format
     */
    public RoomMediaMessage(CharSequence text, String htmlText, String format) {
        mClipDataItem = new ClipData.Item(text, htmlText);
        mMimeType = (null == htmlText) ? ClipDescription.MIMETYPE_TEXT_PLAIN : format;
    }

    /**
     * Constructor from a media Uri/
     *
     * @param uri the media uri
     */
    public RoomMediaMessage(Uri uri) {
        this(uri, null);
    }

    /**
     * Constructor from a media Uri/
     *
     * @param uri      the media uri
     * @param filename the media file name
     */
    public RoomMediaMessage(Uri uri, String filename) {
        mUri = uri;
        mFileName = filename;
    }

    /**
     * Constructor from an event.
     *
     * @param event the event
     */
    public RoomMediaMessage(Event event) {
        setEvent(event);

        Message message = JsonUtils.toMessage(event.getContent());
        if (null != message) {
            setMessageType(message.msgtype);
        }
    }

    /**
     * Constructor from a parcel
     *
     * @param source the parcel
     */
    private RoomMediaMessage(Parcel source) {
        mUri = unformatNullUri((Uri) source.readParcelable(Uri.class.getClassLoader()));
        mMimeType = unformatNullString(source.readString());

        CharSequence clipDataItemText = unformatNullString(source.readString());
        String clipDataItemHtml = unformatNullString(source.readString());
        Uri clipDataItemUri = unformatNullUri((Uri) source.readParcelable(Uri.class.getClassLoader()));

        if (!TextUtils.isEmpty(clipDataItemText) || !TextUtils.isEmpty(clipDataItemHtml) || (null != clipDataItemUri)) {
            mClipDataItem = new ClipData.Item(clipDataItemText, clipDataItemHtml, null, clipDataItemUri);
        }

        mFileName = unformatNullString(source.readString());
    }

    @Override
    public java.lang.String toString() {
        String description = "";

        description += "mUri " + mUri;
        description += " -- mMimeType " + mMimeType;
        description += " -- mEvent " + mEvent;
        description += " -- mClipDataItem " + mClipDataItem;
        description += " -- mFileName " + mFileName;
        description += " -- mMessageType " + mMessageType;
        description += " -- mThumbnailSize " + mThumbnailSize;

        return description;
    }

    //==============================================================================================================
    // Parcelable
    //==============================================================================================================

    /**
     * Unformat parcelled String
     *
     * @param string the string to unformat
     * @return the unformatted string
     */
    private static String unformatNullString(final String string) {
        if (TextUtils.isEmpty(string)) {
            return null;
        }

        return string;
    }

    /**
     * Convert null uri to a dummy one
     *
     * @param uri the uri to unformat
     * @return the unformatted
     */
    private static Uri unformatNullUri(final Uri uri) {
        if ((null == uri) || mDummyUri.equals(uri)) {
            return null;
        }

        return uri;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Convert null string to ""
     *
     * @param string the string to format
     * @return the formatted string
     */
    private static String formatNullString(final String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        return string;
    }

    private static String formatNullString(final CharSequence charSequence) {
        if (TextUtils.isEmpty(charSequence)) {
            return "";
        }

        return charSequence.toString();
    }

    /**
     * Convert null uri to a dummy one
     *
     * @param uri the uri to format
     * @return the formatted
     */
    private static Uri formatNullUri(final Uri uri) {
        if (null == uri) {
            return mDummyUri;
        }

        return uri;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(formatNullUri(mUri), 0);
        dest.writeString(formatNullString(mMimeType));

        if (null == mClipDataItem) {
            dest.writeString("");
            dest.writeString("");
            dest.writeParcelable(formatNullUri(null), 0);
        } else {
            dest.writeString(formatNullString(mClipDataItem.getText()));
            dest.writeString(formatNullString(mClipDataItem.getHtmlText()));
            dest.writeParcelable(formatNullUri(mClipDataItem.getUri()), 0);
        }

        dest.writeString(formatNullString(mFileName));
    }

    // Creator
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public RoomMediaMessage createFromParcel(Parcel in) {
            return new RoomMediaMessage(in);
        }

        public RoomMediaMessage[] newArray(int size) {
            return new RoomMediaMessage[size];
        }
    };

    //==============================================================================================================
    // Setters / getters
    //==============================================================================================================

    /**
     * Set the message type.
     *
     * @param messageType the message type.
     */
    public void setMessageType(String messageType) {
        mMessageType = messageType;
    }

    /**
     * @return the message type.
     */
    public String getMessageType() {
        return mMessageType;
    }

    /**
     * Set the replyTo event.
     *
     * @param replyToEvent the event to reply to
     */
    public void setReplyToEvent(@Nullable Event replyToEvent) {
        mReplyToEvent = replyToEvent;
    }

    /**
     * @return the replyTo event.
     */
    @Nullable
    public Event getReplyToEvent() {
        return mReplyToEvent;
    }

    /**
     * Update the inner event.
     *
     * @param event the new event.
     */
    public void setEvent(Event event) {
        mEvent = event;
    }

    /**
     * @return the inner event objects
     */
    public Event getEvent() {
        return mEvent;
    }

    /**
     * Update the thumbnail size.
     *
     * @param size the new thumbnail size.
     */
    public void setThumbnailSize(Pair<Integer, Integer> size) {
        mThumbnailSize = size;
    }

    /**
     * @return the thumbnail size.
     */
    public Pair<Integer, Integer> getThumbnailSize() {
        return mThumbnailSize;
    }

    /**
     * Update the media upload listener.
     *
     * @param mediaUploadListener the media upload listener.
     */
    public void setMediaUploadListener(IMXMediaUploadListener mediaUploadListener) {
        mMediaUploadListener = mediaUploadListener;
    }

    /**
     * @return the media upload listener.
     */
    public IMXMediaUploadListener getMediaUploadListener() {
        return mMediaUploadListener;
    }

    /**
     * Update the event sending callback.
     *
     * @param callback the callback
     */
    public void setEventSendingCallback(ApiCallback<Void> callback) {
        mEventSendingCallback = callback;
    }

    /**
     * @return the event sending callback.
     */
    public ApiCallback<Void> getSendingCallback() {
        return mEventSendingCallback;
    }

    /**
     * Update the listener
     *
     * @param eventCreationListener the new listener
     */
    public void setEventCreationListener(EventCreationListener eventCreationListener) {
        mEventCreationListener = eventCreationListener;
    }

    /**
     * @return the listener.
     */
    public EventCreationListener getEventCreationListener() {
        return mEventCreationListener;
    }

    /**
     * Retrieve the raw text contained in this Item.
     *
     * @return the raw text
     */
    public CharSequence getText() {
        if (null != mClipDataItem) {
            return mClipDataItem.getText();
        }
        return null;
    }

    /**
     * Retrieve the raw HTML text contained in this Item.
     *
     * @return the raw HTML text
     */
    public String getHtmlText() {
        if (null != mClipDataItem) {
            return mClipDataItem.getHtmlText();
        }

        return null;
    }

    /**
     * Retrieve the Intent contained in this Item.
     *
     * @return the intent
     */
    public Intent getIntent() {
        if (null != mClipDataItem) {
            return mClipDataItem.getIntent();
        }

        return null;
    }

    /**
     * Retrieve the URI contained in this Item.
     *
     * @return the Uri
     */
    public Uri getUri() {
        if (null != mUri) {
            return mUri;
        } else if (null != mClipDataItem) {
            return mClipDataItem.getUri();
        }

        return null;
    }

    /**
     * Returns the mimetype.
     *
     * @param context the context
     * @return the mimetype
     */
    public String getMimeType(Context context) {
        if ((null == mMimeType) && (null != getUri())) {
            try {
                Uri uri = getUri();
                mMimeType = context.getContentResolver().getType(uri);

                // try to find the mimetype from the filename
                if (null == mMimeType) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString().toLowerCase());
                    if (extension != null) {
                        mMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                    }
                }

                if (null != mMimeType) {
                    // the mimetype is sometimes in uppercase.
                    mMimeType = mMimeType.toLowerCase();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to open resource input stream", e);
            }
        }

        return mMimeType;
    }

    /**
     * Gets the MINI_KIND image thumbnail.
     *
     * @param context the context
     * @return the MINI_KIND thumbnail it it exists
     */
    public Bitmap getMiniKindImageThumbnail(Context context) {
        return getImageThumbnail(context, MediaStore.Images.Thumbnails.MINI_KIND);
    }

    /**
     * Gets the FULL_SCREEN image thumbnail.
     *
     * @param context the context
     * @return the FULL_SCREEN thumbnail it it exists
     */
    public Bitmap getFullScreenImageKindThumbnail(Context context) {
        return getImageThumbnail(context, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
    }

    /**
     * Gets the image thumbnail.
     *
     * @param context the context.
     * @param kind    the thumbnail kind.
     * @return the thumbnail.
     */
    private Bitmap getImageThumbnail(Context context, int kind) {
        // sanity check
        if ((null == getMimeType(context)) || !getMimeType(context).startsWith("image/")) {
            return null;
        }

        Bitmap thumbnailBitmap = null;

        try {
            ContentResolver resolver = context.getContentResolver();

            List uriPath = getUri().getPathSegments();
            Long imageId;
            String lastSegment = (String) uriPath.get(uriPath.size() - 1);

            // > Kitkat
            if (lastSegment.startsWith("image:")) {
                lastSegment = lastSegment.substring("image:".length());
            }

            try {
                imageId = Long.parseLong(lastSegment);
            } catch (Exception e) {
                imageId = null;
            }

            if (null != imageId) {
                thumbnailBitmap = MediaStore.Images.Thumbnails.getThumbnail(resolver, imageId, kind, null);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "MediaStore.Images.Thumbnails.getThumbnail " + e.getMessage(), e);
        }

        return thumbnailBitmap;
    }

    /**
     * @param context the context
     * @return the filename
     */
    public String getFileName(Context context) {
        if ((null == mFileName) && (null != getUri())) {
            Uri mediaUri = getUri();

            if (null != mediaUri) {
                try {
                    if (mediaUri.toString().startsWith("content://")) {
                        Cursor cursor = null;
                        try {
                            cursor = context.getContentResolver().query(mediaUri, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                mFileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "cursor.getString " + e.getMessage(), e);
                        } finally {
                            if (null != cursor) {
                                cursor.close();
                            }
                        }

                        if (TextUtils.isEmpty(mFileName)) {
                            List uriPath = mediaUri.getPathSegments();
                            mFileName = (String) uriPath.get(uriPath.size() - 1);
                        }
                    } else if (mediaUri.toString().startsWith("file://")) {
                        mFileName = mediaUri.getLastPathSegment();
                    }
                } catch (Exception e) {
                    mFileName = null;
                }
            }
        }

        return mFileName;
    }

    /**
     * Save a media into a dedicated folder
     *
     * @param context the context
     * @param folder  the folder.
     */
    public void saveMedia(Context context, File folder) {
        mFileName = null;
        Uri mediaUri = getUri();

        if (null != mediaUri) {
            try {
                ResourceUtils.Resource resource = ResourceUtils.openResource(context, mediaUri, getMimeType(context));

                if (null == resource) {
                    Log.e(LOG_TAG, "## saveMedia : Fail to retrieve the resource " + mediaUri);
                } else {
                    mUri = saveFile(folder, resource.mContentStream, getFileName(context), resource.mMimeType);
                    resource.mContentStream.close();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## saveMedia : failed " + e.getMessage(), e);
            }
        }
    }

    /**
     * Save a file in a dedicated directory.
     * The filename is optional.
     *
     * @param folder          the destination folder
     * @param stream          the file stream
     * @param defaultFileName the filename, null to generate a new one
     * @param mimeType        the file mimetype.
     * @return the file uri
     */
    private static Uri saveFile(File folder, InputStream stream, String defaultFileName, String mimeType) {
        String filename = defaultFileName;

        if (null == filename) {
            filename = "file" + System.currentTimeMillis();

            if (null != mimeType) {
                String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

                if (null != extension) {
                    filename += "." + extension;
                }
            }
        }

        Uri fileUri = null;

        try {
            File file = new File(folder, filename);

            // if the file exits, delete it
            if (file.exists()) {
                file.delete();
            }

            FileOutputStream fos = new FileOutputStream(file.getPath());

            try {
                byte[] buf = new byte[1024 * 32];

                int len;
                while ((len = stream.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "## saveFile failed " + e.getMessage(), e);
            }

            fos.flush();
            fos.close();
            stream.close();

            fileUri = Uri.fromFile(file);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## saveFile failed " + e.getMessage(), e);
        }

        return fileUri;
    }

    //==============================================================================================================
    // Dispatchers
    //==============================================================================================================

    /**
     * Dispatch onEventCreated.
     */
    void onEventCreated() {
        if (null != getEventCreationListener()) {
            try {
                getEventCreationListener().onEventCreated(this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onEventCreated() failed : " + e.getMessage(), e);
            }
        }

        // clear the listener
        mEventCreationListener = null;
    }

    /**
     * Dispatch onEventCreationFailed.
     */
    void onEventCreationFailed(String errorMessage) {
        if (null != getEventCreationListener()) {
            try {
                getEventCreationListener().onEventCreationFailed(this, errorMessage);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onEventCreationFailed() failed : " + e.getMessage(), e);
            }
        }

        // clear the listeners
        mMediaUploadListener = null;
        mEventSendingCallback = null;
        mEventCreationListener = null;
    }

    /**
     * Dispatch onEncryptionFailed.
     */
    void onEncryptionFailed() {
        if (null != getEventCreationListener()) {
            try {
                getEventCreationListener().onEncryptionFailed(this);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onEncryptionFailed() failed : " + e.getMessage(), e);
            }
        }

        // clear the listeners
        mMediaUploadListener = null;
        mEventSendingCallback = null;
        mEventCreationListener = null;
    }

    //==============================================================================================================
    // Retrieve RoomMediaMessages from intents.
    //==============================================================================================================

    /**
     * List the item provided in an intent.
     *
     * @param intent the intent.
     * @return the RoomMediaMessages list
     */
    public static List<RoomMediaMessage> listRoomMediaMessages(Intent intent) {
        return listRoomMediaMessages(intent, null);
    }

    /**
     * List the item provided in an intent.
     *
     * @param intent the intent.
     * @param loader the class loader.
     * @return the room list
     */
    public static List<RoomMediaMessage> listRoomMediaMessages(Intent intent, ClassLoader loader) {
        List<RoomMediaMessage> roomMediaMessages = new ArrayList<>();


        if (null != intent) {
            // chrome adds many items when sharing an web page link
            // so, test first the type
            if (TextUtils.equals(intent.getType(), ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                String message = intent.getStringExtra(Intent.EXTRA_TEXT);

                if (null == message) {
                    CharSequence sequence = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                    if (null != sequence) {
                        message = sequence.toString();
                    }
                }

                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

                if (!TextUtils.isEmpty(subject)) {
                    if (TextUtils.isEmpty(message)) {
                        message = subject;
                    } else if (android.util.Patterns.WEB_URL.matcher(message).matches()) {
                        message = subject + "\n" + message;
                    }
                }

                if (!TextUtils.isEmpty(message)) {
                    roomMediaMessages.add(new RoomMediaMessage(message, null, intent.getType()));
                    return roomMediaMessages;
                }
            }

            ClipData clipData = null;
            List<String> mimetypes = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                clipData = intent.getClipData();
            }

            // multiple data
            if (null != clipData) {
                if (null != clipData.getDescription()) {
                    if (0 != clipData.getDescription().getMimeTypeCount()) {
                        mimetypes = new ArrayList<>();

                        for (int i = 0; i < clipData.getDescription().getMimeTypeCount(); i++) {
                            mimetypes.add(clipData.getDescription().getMimeType(i));
                        }

                        // if the filter is "accept anything" the mimetype does not make sense
                        if (1 == mimetypes.size()) {
                            if (mimetypes.get(0).endsWith("/*")) {
                                mimetypes = null;
                            }
                        }
                    }
                }

                int count = clipData.getItemCount();

                for (int i = 0; i < count; i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    String mimetype = null;

                    if (null != mimetypes) {
                        if (i < mimetypes.size()) {
                            mimetype = mimetypes.get(i);
                        } else {
                            mimetype = mimetypes.get(0);
                        }

                        // uris list is not a valid mimetype
                        if (TextUtils.equals(mimetype, ClipDescription.MIMETYPE_TEXT_URILIST)) {
                            mimetype = null;
                        }
                    }

                    roomMediaMessages.add(new RoomMediaMessage(item, mimetype));
                }
            } else if (null != intent.getData()) {
                roomMediaMessages.add(new RoomMediaMessage(intent.getData()));
            } else {
                Bundle bundle = intent.getExtras();

                if (null != bundle) {
                    // provide a custom loader
                    bundle.setClassLoader(RoomMediaMessage.class.getClassLoader());
                    // list the Uris list
                    if (bundle.containsKey(Intent.EXTRA_STREAM)) {
                        try {
                            Object streamUri = bundle.get(Intent.EXTRA_STREAM);

                            if (streamUri instanceof Uri) {
                                roomMediaMessages.add(new RoomMediaMessage((Uri) streamUri));
                            } else if (streamUri instanceof List) {
                                List<Object> streams = (List<Object>) streamUri;

                                for (Object object : streams) {
                                    if (object instanceof Uri) {
                                        roomMediaMessages.add(new RoomMediaMessage((Uri) object));
                                    } else if (object instanceof RoomMediaMessage) {
                                        roomMediaMessages.add((RoomMediaMessage) object);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "fail to extract the extra stream", e);
                        }
                    }
                }
            }
        }

        return roomMediaMessages;
    }
}
