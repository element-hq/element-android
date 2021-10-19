/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.network

import org.matrix.android.sdk.internal.network.NetworkConstants

enum class ApiPath(val path: String, val method: String) {
    // AuthApi
    VERSIONS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "versions", "GET"),
    REGISTER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register", "POST"),
    ADD_3PID(NetworkConstants.URI_API_PREFIX_PATH_R0 + "register/{threePid}/requestToken", "POST"),
    LOGIN_FLOWS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login", "GET"),
    LOGIN(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login", "POST"),
    RESET_PASSWORD(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password/email/requestToken", "POST"),
    RESET_PASSWORD_MAIL_CONFIRMED(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password", "POST"),

    // DirectoryApi
    ROOM_ID_BY_ALIAS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}", "GET"),
    ROOM_DIRECTORY_VISIBILITY(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}", "GET"),
    SET_ROOM_DIRECTORY_VISIBILITY(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/list/room/{roomId}", "PUT"),
    ADD_ROOM_ALIAS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}", "PUT"),
    DELETE_ROOM_ALIAS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "directory/room/{roomAlias}", "DELETE"),

    // CryptoApi
    GET_DEVICES(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices", "GET"),
    GET_DEVICE_INFO(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{deviceId}", "GET"),
    UPLOAD_KEYS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/upload", "POST"),
    DOWNLOAD_KEYS_FOR_USERS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/query", "POST"),
    UPLOAD_SIGNING_KEYS(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "keys/device_signing/upload", "POST"),
    UPLOAD_SIGNATURES(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "keys/signatures/upload", "POST"),
    CLAIM_ONE_TIME_KEYS_FOR_USERS_DEVICES(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/claim", "POST"),
    SEND_TO_DEVICE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "sendToDevice/{eventType}/{txnId}", "PUT"),
    DELETE_DEVICE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{device_id}", "DELETE"),
    UPDATE_DEVICE_INFO(NetworkConstants.URI_API_PREFIX_PATH_R0 + "devices/{device_id}", "PUT"),
    GET_KEY_CHANGES(NetworkConstants.URI_API_PREFIX_PATH_R0 + "keys/changes", "GET"),

    // RoomKeysApi
    CREATE_KEYS_BACKUP_VERSION(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version", "POST"),
    GET_KEYS_BACKUP_LAST_VERSION(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version", "GET"),
    GET_KEYS_BACKUP_VERSION(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}", "GET"),
    UPDATE_KEYS_BACKUP_VERSION(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}", "PUT"),
    STORE_ROOM_SESSION_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}", "PUT"),
    STORE_ROOM_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}", "PUT"),
    STORE_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys", "PUT"),
    GET_ROOM_SESSION_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}", "GET"),
    GET_ROOM_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}", "GET"),
    GET_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys", "GET"),
    DELETE_ROOM_SESSION_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}/{sessionId}", "DELETE"),
    DELETE_ROOM_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys/{roomId}", "DELETE"),
    DELETE_SESSIONS_DATA(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/keys", "DELETE"),
    DELETE_BACKUP(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "room_keys/version/{version}", "DELETE"),

    // AccountApi
    CHANGE_PASSWORD(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/password", "POST"),
    DEACTIVATE_ACCOUNT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/deactivate", "POST"),

    // SearchApi
    SEARCH(NetworkConstants.URI_API_PREFIX_PATH_R0 + "search", "POST"),

    // FederationApi
    GET_FEDERATION_VERSION(NetworkConstants.URI_FEDERATION_PATH + "version", "GET"),

    // VoipApi
    GET_TURN_SERVER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "voip/turnServer", "GET"),

    // PushGatewayApi
    NOTIFY_PUSH_GATEWAY(NetworkConstants.URI_PUSH_GATEWAY_PREFIX_PATH + "notify", "POST"),

    // GroupApi
    GET_GROUP_SUMMARY(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/summary", "GET"),
    GET_GROUP_ROOMS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/rooms", "GET"),
    GET_GROUP_USERS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "groups/{groupId}/users", "GET"),

    // CapabilitiesApi
    GET_CAPABILITIES(NetworkConstants.URI_API_PREFIX_PATH_R0 + "capabilities", "GET"),
    GET_VERSIONS(NetworkConstants.URI_API_PREFIX_PATH_ + "versions", "GET"),
    PING(NetworkConstants.URI_API_PREFIX_PATH_ + "versions", "GET"),

    // IdentityApi
    GET_ACCOUNT(NetworkConstants.URI_IDENTITY_PATH_V2 + "account", "GET"),
    LOGOUT(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/logout", "POST"),
    IDENTITY_HAS_DETAILS(NetworkConstants.URI_IDENTITY_PATH_V2 + "hash_details", "GET"),
    LOOKUP(NetworkConstants.URI_IDENTITY_PATH_V2 + "lookup", "POST"),
    REQUEST_TOKEN_TO_BIND_EMAIL(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/email/requestToken", "POST"),
    REQUEST_TOKEN_TO_BIND_MSISDN(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/msisdn/requestToken", "POST"),
    SUBMIT_TOKEN(NetworkConstants.URI_IDENTITY_PATH_V2 + "validate/{medium}/submitToken", "POST"),

    // FilterApi
    UPLOAD_FILTER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter", "POST"),
    GET_FILTER_BY_ID(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/filter/{filterId}", "GET"),

    // IndentityAuthApi
    IDENTITY_REGISTER(NetworkConstants.URI_IDENTITY_PATH_V2 + "account/register", "POST"),

    // MediaApi
    GET_MEDIA_CONFIG(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "config", "GET"),
    GET_PREVIEW_URL_DATA(NetworkConstants.URI_API_MEDIA_PREFIX_PATH_R0 + "preview_url", "GET"),

    // OpenIdApi
    OPEN_ID_TOKEN(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/openid/request_token", "POST"),

    // ProfileApi
    GET_PROFILE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}", "GET"),
    GET_THREE_PIDS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid", "GET"),
    SET_DISPLAY_NAME(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/displayname", "PUT"),
    SET_AVATAR_URL(NetworkConstants.URI_API_PREFIX_PATH_R0 + "profile/{userId}/avatar_url", "PUT"),
    BIND_THREE_PID(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/bind", "POST"),
    UNBIND_THREE_PID(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "account/3pid/unbind", "POST"),
    ADD_EMAIL(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/email/requestToken", "POST"),
    ADD_MSISDN(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/msisdn/requestToken", "POST"),
    FINALIZE_ADD_THREE_PID(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/add", "POST"),
    DELETE_THREE_PID(NetworkConstants.URI_API_PREFIX_PATH_R0 + "account/3pid/delete", "POST"),

    // PusherRulesApi
    GET_ALL_PUSHER_RULES(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/", "GET"),
    UPDATE_ENABLE_PUSH_RULE_STATUS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/enabled", "PUT"),
    UPDATE_PUSH_RULE_ACTIONS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}/actions", "PUT"),
    DELETE_PUSH_RULE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}", "DELETE"),
    ADD_PUSH_RULE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushrules/global/{kind}/{ruleId}", "PUT"),

    // PusherApi
    GET_PUSHERS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers", "GET"),
    SET_PUSHER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "pushers/set", "POST"),

    // SignOutApi
    LOGIN_AGAIN(NetworkConstants.URI_API_PREFIX_PATH_R0 + "login", "POST"),
    SIGN_OUT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "logout", "POST"),

    // RoomApi
    GET_PUBLIC_ROOMS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "publicRooms", "POST"),
    CREATE_ROOM(NetworkConstants.URI_API_PREFIX_PATH_R0 + "createRoom", "POST"),
    GET_ROOM_MESSAGES_FROM(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/messages", "GET"),
    GET_MEMBERS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/members", "GET"),
    SEND_EVENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/send/{eventType}/{txId}", "PUT"),
    GET_CONTEXT_OF_EVENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/context/{eventId}", "GET"),
    GET_EVENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/event/{eventId}", "GET"),
    SEND_READ_MARKER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/read_markers", "POST"),
    INVITE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/invite", "POST"),
    INVITE_USING_THREE_PID(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/invite", "POST"),
    SEND_STATE_EVENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/state/{state_event_type}", "PUT"),
    SEND_STATE_EVENT_WITH_STATE_KEY(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/state/{state_event_type}/{state_key}", "PUT"),
    GET_ROOM_STATE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/state", "GET"),
    GET_RELATIONS(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "rooms/{roomId}/relations/{eventId}/{relationType}/{eventType}", "GET"),
    JOIN_ROOM(NetworkConstants.URI_API_PREFIX_PATH_R0 + "join/{roomIdOrAlias}", "POST"),
    LEAVE_ROOM(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/leave", "POST"),
    BAN_USER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/ban", "POST"),
    UNBAN_USER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/unban", "POST"),
    KICK_USER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/kick", "POST"),
    REDACT_EVENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/redact/{eventId}/{txnId}", "PUT"),
    REPORT_CONTENT(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/report/{eventId}", "POST"),
    GET_ALIASES(NetworkConstants.URI_API_PREFIX_PATH_UNSTABLE + "org.matrix.msc2432/rooms/{roomId}/aliases", "GET"),
    SEND_TYPING_STATE(NetworkConstants.URI_API_PREFIX_PATH_R0 + "rooms/{roomId}/typing/{userId}", "PUT"),
    PUT_TAG(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/rooms/{roomId}/tags/{tag}", "PUT"),
    DELETE_TAG(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/rooms/{roomId}/tags/{tag}", "DELETE"),

    // SyncApi
    SYNC(NetworkConstants.URI_API_PREFIX_PATH_R0 + "sync", "GET"),

    // ThirdPartyApi
    THIRD_PARTY_PROTOCOLS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols", "GET"),
    THIRD_PARTY_USER(NetworkConstants.URI_API_PREFIX_PATH_R0 + "thirdparty/protocols/user/{protocol}", "GET"),

    // SearchUserApi
    SEARCH_USERS(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user_directory/search", "POST"),

    // AccountDataApi
    SET_ACCOUNT_DATA(NetworkConstants.URI_API_PREFIX_PATH_R0 + "user/{userId}/account_data/{type}", "PUT")
}
