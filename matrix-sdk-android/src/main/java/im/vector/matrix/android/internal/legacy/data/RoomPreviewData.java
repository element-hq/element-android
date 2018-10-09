/*
 * Copyright 2016 OpenMarket Ltd
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

import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.data.timeline.EventTimeline;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.publicroom.PublicRoom;
import im.vector.matrix.android.internal.legacy.rest.model.sync.RoomResponse;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.Map;

/**
 * The `RoomEmailInvitation` gathers information for displaying the preview of a room that is unknown for the user.
 * Such room can come from an email invitation link or a link to a room.
 */
public class RoomPreviewData {

    private static final String LOG_TAG = RoomPreviewData.class.getSimpleName();

    // The id of the room to preview.
    private String mRoomId;

    // the room Alias
    private String mRoomAlias;

    // the id of the event to preview
    private String mEventId;

    // In case of email invitation, the information extracted from the email invitation link.
    private RoomEmailInvitation mRoomEmailInvitation;

    // preview information
    // comes from the email invitation or retrieve from an initialSync
    private String mRoomName;
    private String mRoomAvatarUrl;

    // the room state
    private RoomState mRoomState;

    // If the RoomState cannot be retrieved, this may contains some data
    private PublicRoom mPublicRoom;

    // the initial sync data
    private RoomResponse mRoomResponse;

    // the session
    private MXSession mSession;

    /**
     * Create an RoomPreviewData instance
     *
     * @param session               the session.
     * @param roomId                the room Id to preview
     * @param eventId               the event Id to preview (optional)
     * @param roomAlias             the room alias (optional)
     * @param emailInvitationParams the email invitation parameters (optional)
     */
    public RoomPreviewData(MXSession session, String roomId, String eventId, String roomAlias, Map<String, String> emailInvitationParams) {
        mSession = session;
        mRoomId = roomId;
        mRoomAlias = roomAlias;
        mEventId = eventId;

        if (null != emailInvitationParams) {
            mRoomEmailInvitation = new RoomEmailInvitation(emailInvitationParams);
            mRoomName = mRoomEmailInvitation.roomName;
            mRoomAvatarUrl = mRoomEmailInvitation.roomAvatarUrl;
        }
    }

    /**
     * @return the room state
     */
    @Nullable
    public RoomState getRoomState() {
        return mRoomState;
    }

    /**
     * @return the public room data
     */
    @Nullable
    public PublicRoom getPublicRoom() {
        return mPublicRoom;
    }

    /**
     * Update the room state.
     *
     * @param roomState the new roomstate
     */
    public void setRoomState(RoomState roomState) {
        mRoomState = roomState;
    }

    /**
     * @return the room name
     */
    public String getRoomName() {
        String roomName = mRoomName;

        if (TextUtils.isEmpty(roomName)) {
            roomName = getRoomIdOrAlias();
        }

        return roomName;
    }

    /**
     * Set the room name.
     *
     * @param aRoomName the new room name
     */
    public void setRoomName(String aRoomName) {
        mRoomName = aRoomName;
    }

    /**
     * @return the room avatar URL
     */
    public String getRoomAvatarUrl() {
        return mRoomAvatarUrl;
    }

    /**
     * @return the room id
     */
    public String getRoomId() {
        return mRoomId;
    }

    /**
     * @return the room id or the alias (alias is preferred)
     */
    public String getRoomIdOrAlias() {
        if (!TextUtils.isEmpty(mRoomAlias)) {
            return mRoomAlias;
        } else {
            return mRoomId;
        }
    }

    /**
     * @return the event id.
     */
    public String getEventId() {
        return mEventId;
    }

    /**
     * @return the session
     */
    public MXSession getSession() {
        return mSession;
    }

    /**
     * @return the initial sync response
     */
    public RoomResponse getRoomResponse() {
        return mRoomResponse;
    }

    /**
     * @return the room invitation
     */
    public RoomEmailInvitation getRoomEmailInvitation() {
        return mRoomEmailInvitation;
    }

    /**
     * Attempt to get more information from the homeserver about the room.
     *
     * @param apiCallback the callback when the operation is done.
     */
    public void fetchPreviewData(final ApiCallback<Void> apiCallback) {
        mSession.getRoomsApiClient().initialSync(mRoomId, new ApiCallback<RoomResponse>() {
            @Override
            public void onSuccess(final RoomResponse roomResponse) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        // save the initial sync response
                        mRoomResponse = roomResponse;

                        mRoomState = new RoomState();
                        mRoomState.roomId = mRoomId;

                        for (Event event : roomResponse.state) {
                            mRoomState.applyState(null, event, EventTimeline.Direction.FORWARDS);
                        }

                        // TODO LazyLoading handle case where room has no name
                        mRoomName = mRoomState.name;
                        mRoomAvatarUrl = mRoomState.getAvatarUrl();
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void args) {
                        apiCallback.onSuccess(null);
                    }
                };
                try {
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "## fetchPreviewData() failed " + e.getMessage(), e);
                    task.cancel(true);

                    (new android.os.Handler(Looper.getMainLooper())).post(new Runnable() {
                        @Override
                        public void run() {
                            if (null != apiCallback) {
                                apiCallback.onUnexpectedError(e);
                            }
                        }
                    });
                }
            }

            @Override
            public void onNetworkError(Exception e) {
                mRoomState = new RoomState();
                mRoomState.roomId = mRoomId;
                apiCallback.onNetworkError(e);
            }

            @Override
            public void onMatrixError(MatrixError e) {
                mRoomState = new RoomState();
                mRoomState.roomId = mRoomId;
                apiCallback.onMatrixError(e);
            }

            @Override
            public void onUnexpectedError(Exception e) {
                mRoomState = new RoomState();
                mRoomState.roomId = mRoomId;
                apiCallback.onUnexpectedError(e);
            }
        });
    }

    /**
     * Set Public RoomData, In case RoomState cannot be retrieved
     *
     * @param publicRoom
     */
    public void setPublicRoom(PublicRoom publicRoom) {
        mPublicRoom = publicRoom;
    }
}
