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
package im.vector.matrix.android.internal.legacy.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.TreeSet;

import im.vector.matrix.android.internal.legacy.rest.json.BooleanDeserializer;
import im.vector.matrix.android.internal.legacy.rest.json.ConditionDeserializer;
import im.vector.matrix.android.internal.legacy.rest.json.MatrixFieldNamingStrategy;
import im.vector.matrix.android.internal.legacy.rest.model.ContentResponse;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.EventContent;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.PowerLevels;
import im.vector.matrix.android.internal.legacy.rest.model.RoomCreateContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomMember;
import im.vector.matrix.android.internal.legacy.rest.model.RoomPinnedEventsContent;
import im.vector.matrix.android.internal.legacy.rest.model.RoomTags;
import im.vector.matrix.android.internal.legacy.rest.model.RoomTombstoneContent;
import im.vector.matrix.android.internal.legacy.rest.model.StateEvent;
import im.vector.matrix.android.internal.legacy.rest.model.User;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.Condition;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.EncryptedEventContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.ForwardedRoomKeyContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.OlmEventContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.OlmPayloadContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyContent;
import im.vector.matrix.android.internal.legacy.rest.model.crypto.RoomKeyRequest;
import im.vector.matrix.android.internal.legacy.rest.model.login.RegistrationFlowResponse;
import im.vector.matrix.android.internal.legacy.rest.model.message.AudioMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.FileMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.ImageMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.LocationMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;
import im.vector.matrix.android.internal.legacy.rest.model.message.StickerJsonMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.StickerMessage;
import im.vector.matrix.android.internal.legacy.rest.model.message.VideoMessage;
import im.vector.matrix.android.internal.legacy.rest.model.pid.RoomThirdPartyInvite;

/**
 * Static methods for converting json into objects.
 */
public class JsonUtils {
    private static final String LOG_TAG = JsonUtils.class.getSimpleName();

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingStrategy(new MatrixFieldNamingStrategy())
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .registerTypeAdapter(Condition.class, new ConditionDeserializer())
            .registerTypeAdapter(boolean.class, new BooleanDeserializer(false))
            .registerTypeAdapter(Boolean.class, new BooleanDeserializer(true))
            .create();

    // add a call to serializeNulls().
    // by default the null parameters are not sent in the requests.
    // serializeNulls forces to add them.
    private static final Gson gsonWithNullSerialization = new GsonBuilder()
            .setFieldNamingStrategy(new MatrixFieldNamingStrategy())
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .serializeNulls()
            .registerTypeAdapter(Condition.class, new ConditionDeserializer())
            .registerTypeAdapter(boolean.class, new BooleanDeserializer(false))
            .registerTypeAdapter(Boolean.class, new BooleanDeserializer(true))
            .create();

    // for crypto (canonicalize)
    // avoid converting "=" to \u003d
    private static final Gson gsonWithoutHtmlEscaping = new GsonBuilder()
            .setFieldNamingStrategy(new MatrixFieldNamingStrategy())
            .disableHtmlEscaping()
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .registerTypeAdapter(Condition.class, new ConditionDeserializer())
            .registerTypeAdapter(boolean.class, new BooleanDeserializer(false))
            .registerTypeAdapter(Boolean.class, new BooleanDeserializer(true))
            .create();

    /**
     * Provides the JSON parser.
     *
     * @param withNullSerialization true to serialise the null parameters
     * @return the JSON parser
     */
    public static Gson getGson(boolean withNullSerialization) {
        return withNullSerialization ? gsonWithNullSerialization : gson;
    }

    /**
     * Convert a JSON object to a state event.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a room state
     */
    public static StateEvent toStateEvent(JsonElement jsonObject) {
        return toClass(jsonObject, StateEvent.class);
    }

    /**
     * Convert a JSON object to an User.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an user
     */
    public static User toUser(JsonElement jsonObject) {
        return toClass(jsonObject, User.class);
    }

    /**
     * Convert a JSON object to a RoomMember.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a RoomMember
     */
    public static RoomMember toRoomMember(JsonElement jsonObject) {
        return toClass(jsonObject, RoomMember.class);
    }

    /**
     * Convert a JSON object to a RoomTags.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a RoomTags
     */
    public static RoomTags toRoomTags(JsonElement jsonObject) {
        return toClass(jsonObject, RoomTags.class);
    }

    /**
     * Convert a JSON object to a MatrixError.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a MatrixError
     */
    public static MatrixError toMatrixError(JsonElement jsonObject) {
        return toClass(jsonObject, MatrixError.class);
    }

    /**
     * Retrieves the message type from a Json object.
     *
     * @param jsonObject the json object
     * @return the message type
     */
    @Nullable
    public static String getMessageMsgType(JsonElement jsonObject) {
        try {
            Message message = gson.fromJson(jsonObject, Message.class);
            return message.msgtype;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## getMessageMsgType failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Convert a JSON object to a Message.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a Message
     */
    @NonNull
    public static Message toMessage(JsonElement jsonObject) {
        try {
            Message message = gson.fromJson(jsonObject, Message.class);

            // Try to return the right subclass
            if (Message.MSGTYPE_IMAGE.equals(message.msgtype)) {
                return toImageMessage(jsonObject);
            }

            if (Message.MSGTYPE_VIDEO.equals(message.msgtype)) {
                return toVideoMessage(jsonObject);
            }

            if (Message.MSGTYPE_LOCATION.equals(message.msgtype)) {
                return toLocationMessage(jsonObject);
            }

            // Try to return the right subclass
            if (Message.MSGTYPE_FILE.equals(message.msgtype)) {
                return toFileMessage(jsonObject);
            }

            if (Message.MSGTYPE_AUDIO.equals(message.msgtype)) {
                return toAudioMessage(jsonObject);
            }

            // Fall back to the generic Message type
            return message;
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toMessage failed " + e.getMessage(), e);
        }

        return new Message();
    }

    /**
     * Convert a JSON object to an Event.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an Event
     */
    public static Event toEvent(JsonElement jsonObject) {
        return toClass(jsonObject, Event.class);
    }

    /**
     * Convert a JSON object to an EncryptedEventContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an EncryptedEventContent
     */
    public static EncryptedEventContent toEncryptedEventContent(JsonElement jsonObject) {
        return toClass(jsonObject, EncryptedEventContent.class);
    }

    /**
     * Convert a JSON object to an OlmEventContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an OlmEventContent
     */
    public static OlmEventContent toOlmEventContent(JsonElement jsonObject) {
        return toClass(jsonObject, OlmEventContent.class);
    }

    /**
     * Convert a JSON object to an OlmPayloadContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an OlmPayloadContent
     */
    public static OlmPayloadContent toOlmPayloadContent(JsonElement jsonObject) {
        return toClass(jsonObject, OlmPayloadContent.class);
    }

    /**
     * Convert a JSON object to an EventContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an EventContent
     */
    public static EventContent toEventContent(JsonElement jsonObject) {
        return toClass(jsonObject, EventContent.class);
    }

    /**
     * Convert a JSON object to an RoomKeyContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an RoomKeyContent
     */
    public static RoomKeyContent toRoomKeyContent(JsonElement jsonObject) {
        return toClass(jsonObject, RoomKeyContent.class);
    }

    /**
     * Convert a JSON object to an RoomKeyRequest.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an RoomKeyRequest
     */
    public static RoomKeyRequest toRoomKeyRequest(JsonElement jsonObject) {
        return toClass(jsonObject, RoomKeyRequest.class);
    }

    /**
     * Convert a JSON object to an ForwardedRoomKeyContent.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an ForwardedRoomKeyContent
     */
    public static ForwardedRoomKeyContent toForwardedRoomKeyContent(JsonElement jsonObject) {
        return toClass(jsonObject, ForwardedRoomKeyContent.class);
    }

    /**
     * Convert a JSON object to an ImageMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an ImageMessage
     */
    public static ImageMessage toImageMessage(JsonElement jsonObject) {
        return toClass(jsonObject, ImageMessage.class);
    }

    /**
     * Convert a JSON object to a StickerMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a StickerMessage
     */
    public static StickerMessage toStickerMessage(JsonElement jsonObject) {
        final StickerJsonMessage stickerJsonMessage = toClass(jsonObject, StickerJsonMessage.class);
        return new StickerMessage(stickerJsonMessage);
    }

    /**
     * Convert a JSON object to an FileMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an FileMessage
     */
    public static FileMessage toFileMessage(JsonElement jsonObject) {
        return toClass(jsonObject, FileMessage.class);
    }

    /**
     * Convert a JSON object to an AudioMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return an AudioMessage
     */
    public static AudioMessage toAudioMessage(JsonElement jsonObject) {
        return toClass(jsonObject, AudioMessage.class);
    }

    /**
     * Convert a JSON object to a VideoMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a VideoMessage
     */
    public static VideoMessage toVideoMessage(JsonElement jsonObject) {
        return toClass(jsonObject, VideoMessage.class);
    }

    /**
     * Convert a JSON object to a LocationMessage.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a LocationMessage
     */
    public static LocationMessage toLocationMessage(JsonElement jsonObject) {
        return toClass(jsonObject, LocationMessage.class);
    }

    /**
     * Convert a JSON object to a ContentResponse.
     * The result is never null.
     *
     * @param jsonString the json as string to convert
     * @return a ContentResponse
     */
    public static ContentResponse toContentResponse(String jsonString) {
        return toClass(jsonString, ContentResponse.class);
    }

    /**
     * Convert a JSON object to a PowerLevels.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a PowerLevels
     */
    public static PowerLevels toPowerLevels(JsonElement jsonObject) {
        return toClass(jsonObject, PowerLevels.class);
    }

    /**
     * Convert a JSON object to a RoomThirdPartyInvite.
     * The result is never null.
     *
     * @param jsonObject the json to convert
     * @return a RoomThirdPartyInvite
     */
    public static RoomThirdPartyInvite toRoomThirdPartyInvite(JsonElement jsonObject) {
        return toClass(jsonObject, RoomThirdPartyInvite.class);
    }

    /**
     * Convert a stringified JSON object to a RegistrationFlowResponse.
     * The result is never null.
     *
     * @param jsonString the json as string to convert
     * @return a RegistrationFlowResponse
     */
    public static RegistrationFlowResponse toRegistrationFlowResponse(String jsonString) {
        return toClass(jsonString, RegistrationFlowResponse.class);
    }

    /**
     * Convert a JSON object to a RoomTombstoneContent.
     * The result is never null.
     *
     * @param jsonElement the json to convert
     * @return a RoomTombstoneContent
     */
    public static RoomTombstoneContent toRoomTombstoneContent(final JsonElement jsonElement) {
        return toClass(jsonElement, RoomTombstoneContent.class);
    }

    /**
     * Convert a JSON object to a RoomCreateContent.
     * The result is never null.
     *
     * @param jsonElement the json to convert
     * @return a RoomCreateContent
     */
    public static RoomCreateContent toRoomCreateContent(final JsonElement jsonElement) {
        return toClass(jsonElement, RoomCreateContent.class);
    }

    /**
     * Convert a JSON object to a RoomPinnedEventsContent.
     * The result is never null.
     *
     * @param jsonElement the json to convert
     * @return a RoomPinnedEventsContent
     */
    public static RoomPinnedEventsContent toRoomPinnedEventsContent(final JsonElement jsonElement) {
        return toClass(jsonElement, RoomPinnedEventsContent.class);
    }

    /**
     * Convert a JSON object into a class instance.
     * The returned value cannot be null.
     *
     * @param jsonObject the json object to convert
     * @param aClass     the class
     * @return the converted object
     */
    public static <T> T toClass(JsonElement jsonObject, Class<T> aClass) {
        T object = null;
        try {
            object = gson.fromJson(jsonObject, aClass);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toClass failed " + e.getMessage(), e);
        }
        if (null == object) {
            try {
                final Constructor<T> constructor = aClass.getConstructor();
                object = constructor.newInstance();
            } catch (Throwable t) {
                Log.e(LOG_TAG, "## toClass failed " + t.getMessage(), t);
            }
        }
        return object;
    }

    /**
     * Convert a stringified JSON into a class instance.
     * The returned value cannot be null.
     *
     * @param jsonObjectAsString the json object as string to convert
     * @param aClass             the class
     * @return the converted object
     */
    public static <T> T toClass(String jsonObjectAsString, Class<T> aClass) {
        T object = null;
        try {
            object = gson.fromJson(jsonObjectAsString, aClass);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toClass failed " + e.getMessage(), e);
        }
        if (null == object) {
            try {
                final Constructor<T> constructor = aClass.getConstructor();
                object = constructor.newInstance();
            } catch (Throwable t) {
                Log.e(LOG_TAG, "## toClass failed " + t.getMessage(), t);
            }
        }
        return object;
    }

    /**
     * Convert an Event instance to a Json object.
     *
     * @param event the event instance.
     * @return the json object
     */
    public static JsonObject toJson(Event event) {
        try {
            return (JsonObject) gson.toJsonTree(event);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toJson failed " + e.getMessage(), e);
        }

        return new JsonObject();
    }

    public static JsonObject toJson(Map<String, Object> data) {
        try {
            return (JsonObject) gson.toJsonTree(data);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toJson failed " + e.getMessage(), e);
        }

        return new JsonObject();
    }

    /**
     * Convert an Message instance into a Json object.
     *
     * @param message the Message instance.
     * @return the json object
     */
    public static JsonObject toJson(Message message) {
        try {
            return (JsonObject) gson.toJsonTree(message);
        } catch (Exception e) {
            Log.e(LOG_TAG, "## toJson failed " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Create a canonicalized json string for an object
     *
     * @param object the object to convert
     * @return the canonicalized string
     */
    public static String getCanonicalizedJsonString(Object object) {
        String canonicalizedJsonString = null;

        if (null != object) {
            if (object instanceof JsonElement) {
                canonicalizedJsonString = gsonWithoutHtmlEscaping.toJson(canonicalize((JsonElement) object));
            } else {
                canonicalizedJsonString = gsonWithoutHtmlEscaping.toJson(canonicalize(gsonWithoutHtmlEscaping.toJsonTree(object)));
            }

            if (null != canonicalizedJsonString) {
                canonicalizedJsonString = canonicalizedJsonString.replace("\\/", "/");
            }
        }

        return canonicalizedJsonString;
    }

    /**
     * Canonicalize a JsonElement element
     *
     * @param src the src
     * @return the canonicalize element
     */
    public static JsonElement canonicalize(JsonElement src) {
        // sanity check
        if (null == src) {
            return null;
        }

        if (src instanceof JsonArray) {
            // Canonicalize each element of the array
            JsonArray srcArray = (JsonArray) src;
            JsonArray result = new JsonArray();
            for (int i = 0; i < srcArray.size(); i++) {
                result.add(canonicalize(srcArray.get(i)));
            }
            return result;
        } else if (src instanceof JsonObject) {
            // Sort the attributes by name, and the canonicalize each element of the object
            JsonObject srcObject = (JsonObject) src;
            JsonObject result = new JsonObject();
            TreeSet<String> attributes = new TreeSet<>();

            for (Map.Entry<String, JsonElement> entry : srcObject.entrySet()) {
                attributes.add(entry.getKey());
            }
            for (String attribute : attributes) {
                result.add(attribute, canonicalize(srcObject.get(attribute)));
            }
            return result;
        } else {
            return src;
        }
    }

    /**
     * Convert a string from an UTF8 String
     *
     * @param s the string to convert
     * @return the utf-16 string
     */
    public static String convertFromUTF8(String s) {
        String out = s;

        if (null != out) {
            try {
                byte[] bytes = out.getBytes();
                out = new String(bytes, "UTF-8");
            } catch (Exception e) {
                Log.e(LOG_TAG, "## convertFromUTF8()  failed " + e.getMessage(), e);
            }
        }

        return out;
    }

    /**
     * Convert a string to an UTF8 String
     *
     * @param s the string to convert
     * @return the utf-8 string
     */
    public static String convertToUTF8(String s) {
        String out = s;

        if (null != out) {
            try {
                byte[] bytes = out.getBytes("UTF-8");
                out = new String(bytes);
            } catch (Exception e) {
                Log.e(LOG_TAG, "## convertToUTF8()  failed " + e.getMessage(), e);
            }
        }

        return out;
    }

    /**
     * Returns a dedicated parameter as a string
     *
     * @param paramName the parameter name
     * @return the string value, or null if not defined or not a String
     */
    @Nullable
    public static String getAsString(Map<String, Object> map, String paramName) {
        if (map.containsKey(paramName) && map.get(paramName) instanceof String) {
            return (String) map.get(paramName);
        }

        return null;
    }
}
