/*
 * Copyright 2014 OpenMarket Ltd
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
package im.vector.matrix.android.internal.legacy.rest.model.message;

import com.google.gson.annotations.SerializedName;

public class Message {
    public static final String MSGTYPE_TEXT = "m.text";
    public static final String MSGTYPE_EMOTE = "m.emote";
    public static final String MSGTYPE_NOTICE = "m.notice";
    public static final String MSGTYPE_IMAGE = "m.image";
    public static final String MSGTYPE_AUDIO = "m.audio";
    public static final String MSGTYPE_VIDEO = "m.video";
    public static final String MSGTYPE_LOCATION = "m.location";
    public static final String MSGTYPE_FILE = "m.file";
    public static final String FORMAT_MATRIX_HTML = "org.matrix.custom.html";

    // Add, in local, a fake message type in order to StickerMessage can inherit Message class
    // Because sticker isn't a message type but a event type without msgtype field
    public static final String MSGTYPE_STICKER_LOCAL = "org.matrix.android.sdk.sticker";

    public String msgtype;
    public String body;

    public String format;
    public String formatted_body;

    @SerializedName("m.relates_to")
    public RelatesTo relatesTo;
}