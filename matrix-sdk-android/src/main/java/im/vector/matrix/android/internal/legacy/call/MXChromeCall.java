/*
 * Copyright 2015 OpenMarket Ltd
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

package im.vector.matrix.android.internal.legacy.call;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class MXChromeCall extends MXCall {
    private static final String LOG_TAG = MXChromeCall.class.getSimpleName();

    private WebView mWebView = null;
    private CallWebAppInterface mCallWebAppInterface = null;

    private boolean mIsIncomingPrepared = false;

    private JsonObject mCallInviteParams = null;

    private JsonArray mPendingCandidates = new JsonArray();

    /**
     * @return true if this stack can perform calls.
     */
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    // creator
    public MXChromeCall(MXSession session, Context context, JsonElement turnServer) {
        if (!isSupported()) {
            throw new AssertionError("MXChromeCall : not supported with the current android version");
        }

        if (null == session) {
            throw new AssertionError("MXChromeCall : session cannot be null");
        }

        if (null == context) {
            throw new AssertionError("MXChromeCall : context cannot be null");
        }

        mCallId = "c" + System.currentTimeMillis();
        mSession = session;
        mContext = context;
        mTurnServer = turnServer;
    }

    @Override
    @SuppressLint("NewApi")
    public void createCallView() {
        super.createCallView();
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView = new WebView(mContext);
                mWebView.setBackgroundColor(Color.BLACK);

                // warn that the webview must be added in an activity/fragment
                dispatchOnCallViewCreated(mWebView);

                mUIThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCallWebAppInterface = new CallWebAppInterface();
                        mWebView.addJavascriptInterface(mCallWebAppInterface, "Android");

                        WebView.setWebContentsDebuggingEnabled(true);
                        WebSettings settings = mWebView.getSettings();

                        // Enable Javascript
                        settings.setJavaScriptEnabled(true);

                        // Use WideViewport and Zoom out if there is no viewport defined
                        settings.setUseWideViewPort(true);
                        settings.setLoadWithOverviewMode(true);

                        // Enable pinch to zoom without the zoom buttons
                        settings.setBuiltInZoomControls(true);

                        // Allow use of Local Storage
                        settings.setDomStorageEnabled(true);

                        settings.setAllowFileAccessFromFileURLs(true);
                        settings.setAllowUniversalAccessFromFileURLs(true);

                        settings.setDisplayZoomControls(false);

                        mWebView.setWebViewClient(new WebViewClient());

                        // AppRTC requires third party cookies to work
                        android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
                        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

                        final String url = "file:///android_asset/www/call.html";
                        mWebView.loadUrl(url);

                        mWebView.setWebChromeClient(new WebChromeClient() {
                            @Override
                            public void onPermissionRequest(final PermissionRequest request) {
                                mUIThreadHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        request.grant(request.getResources());
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * Start a call.
     */
    @Override
    public void placeCall(VideoLayoutConfiguration aLocalVideoPosition) {
        super.placeCall(aLocalVideoPosition);
        if (CALL_STATE_READY.equals(getCallState())) {
            mIsIncoming = false;

            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl(mIsVideoCall ? "javascript:placeVideoCall()" : "javascript:placeVoiceCall()");
                }
            });
        }
    }

    /**
     * Prepare a call reception.
     *
     * @param aCallInviteParams   the invitation Event content
     * @param aCallId             the call ID
     * @param aLocalVideoPosition position of the local video attendee
     */
    @Override
    public void prepareIncomingCall(final JsonObject aCallInviteParams, final String aCallId, VideoLayoutConfiguration aLocalVideoPosition) {
        Log.d(LOG_TAG, "## prepareIncomingCall : call state " + getCallState());
        super.prepareIncomingCall(aCallInviteParams, aCallId, aLocalVideoPosition);
        mCallId = aCallId;

        if (CALL_STATE_READY.equals(getCallState())) {
            mIsIncoming = true;

            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:initWithInvite('" + aCallId + "'," + aCallInviteParams.toString() + ")");
                    mIsIncomingPrepared = true;

                    mWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            checkPendingCandidates();
                        }
                    });
                }
            });
        } else if (CALL_STATE_CREATED.equals(getCallState())) {
            mCallInviteParams = aCallInviteParams;

            // detect call type from the sdp
            try {
                JsonObject offer = mCallInviteParams.get("offer").getAsJsonObject();
                JsonElement sdp = offer.get("sdp");
                String sdpValue = sdp.getAsString();
                setIsVideo(sdpValue.contains("m=video"));
            } catch (Exception e) {
                Log.e(LOG_TAG, "## prepareIncomingCall() ; " + e.getMessage(), e);
            }
        }
    }

    /**
     * The call has been detected as an incoming one.
     * The application launched the dedicated activity and expects to launch the incoming call.
     *
     * @param aLocalVideoPosition local video position
     */
    @Override
    public void launchIncomingCall(VideoLayoutConfiguration aLocalVideoPosition) {
        super.launchIncomingCall(aLocalVideoPosition);
        if (CALL_STATE_READY.equals(getCallState())) {
            prepareIncomingCall(mCallInviteParams, mCallId, null);
        }
    }

    /**
     * The callee accepts the call.
     *
     * @param event the event
     */
    private void onCallAnswer(final Event event) {
        if (!CALL_STATE_CREATED.equals(getCallState()) && (null != mWebView)) {
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:receivedAnswer(" + event.getContent().toString() + ")");
                }
            });
        }
    }

    /**
     * The other call member hangs up the call.
     *
     * @param event the event
     */
    private void onCallHangup(final Event event) {
        if (!CALL_STATE_CREATED.equals(getCallState()) && (null != mWebView)) {
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:onHangupReceived(" + event.getContent().toString() + ")");

                    mWebView.post(new Runnable() {
                        @Override
                        public void run() {
                            dispatchOnCallEnd(END_CALL_REASON_PEER_HANG_UP);
                        }
                    });
                }
            });
        }
    }

    /**
     * A new Ice candidate is received
     *
     * @param candidates the ice candidates
     */
    public void onNewCandidates(final JsonElement candidates) {
        if (!CALL_STATE_CREATED.equals(getCallState()) && (null != mWebView)) {
            mWebView.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:gotRemoteCandidates(" + candidates.toString() + ")");
                }
            });
        }
    }

    /**
     * Add ice candidates
     *
     * @param candidates ic candidates
     */
    private void addCandidates(JsonArray candidates) {
        if (mIsIncomingPrepared || !isIncoming()) {
            onNewCandidates(candidates);
        } else {
            synchronized (LOG_TAG) {
                mPendingCandidates.addAll(candidates);
            }
        }
    }

    /**
     * Some Ice candidates could have been received while creating the call view.
     * Check if some of them have been defined.
     */
    public void checkPendingCandidates() {
        synchronized (LOG_TAG) {
            onNewCandidates(mPendingCandidates);
            mPendingCandidates = new JsonArray();
        }
    }

    // events thread

    /**
     * Manage the call events.
     *
     * @param event the call event.
     */
    @Override
    public void handleCallEvent(Event event) {
        super.handleCallEvent(event);

        String eventType = event.getType();

        if (event.isCallEvent()) {
            // event from other member
            if (!TextUtils.equals(event.getSender(), mSession.getMyUserId())) {
                if (Event.EVENT_TYPE_CALL_ANSWER.equals(eventType) && !mIsIncoming) {
                    onCallAnswer(event);
                } else if (Event.EVENT_TYPE_CALL_CANDIDATES.equals(eventType)) {
                    JsonArray candidates = event.getContentAsJsonObject().getAsJsonArray("candidates");
                    addCandidates(candidates);
                } else if (Event.EVENT_TYPE_CALL_HANGUP.equals(eventType)) {
                    onCallHangup(event);
                }
            } else if (Event.EVENT_TYPE_CALL_INVITE.equals(eventType)) {
                // server echo : assume that the other device is ringing
                mCallWebAppInterface.mCallState = IMXCall.CALL_STATE_RINGING;

                // warn in the UI thread
                mUIThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dispatchOnStateDidChange(mCallWebAppInterface.mCallState);
                    }
                });

            } else if (Event.EVENT_TYPE_CALL_ANSWER.equals(eventType)) {
                // check if the call has not been answer in another device
                mUIThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // ring on this side
                        if (getCallState().equals(IMXCall.CALL_STATE_RINGING)) {
                            onAnsweredElsewhere();
                        }
                    }
                });

            }
        }
    }

    // user actions

    /**
     * The call is accepted.
     */
    @Override
    public void answer() {
        super.answer();
        if (!CALL_STATE_CREATED.equals(getCallState()) && (null != mWebView)) {
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:answerCall()");
                }
            });
        }
    }

    /**
     * The call is hung up.
     */
    @Override
    public void hangup(String reason) {
        super.hangup(reason);

        if (!CALL_STATE_CREATED.equals(getCallState()) && (null != mWebView)) {
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadUrl("javascript:hangup()");
                }
            });
        } else {
            sendHangup(reason);
        }
    }

    // getters / setters

    /**
     * @return the callstate (must be a CALL_STATE_XX value)
     */
    @Override
    public String getCallState() {
        if (null != mCallWebAppInterface) {
            return mCallWebAppInterface.mCallState;
        } else {
            return CALL_STATE_CREATED;
        }
    }

    /**
     * @return the callView
     */
    @Override
    public View getCallView() {
        return mWebView;
    }

    /**
     * @return the callView visibility
     */
    @Override
    public int getVisibility() {
        if (null != mWebView) {
            return mWebView.getVisibility();
        } else {
            return View.GONE;
        }
    }

    /**
     * Set the callview visibility
     *
     * @return true if the operation succeeds
     */
    public boolean setVisibility(int visibility) {
        if (null != mWebView) {
            mWebView.setVisibility(visibility);
            return true;
        }
        return false;
    }

    @Override
    public void onAnsweredElsewhere() {
        super.onAnsweredElsewhere();
        mUIThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript:onAnsweredElsewhere()");
            }
        });

        dispatchAnsweredElsewhere();
    }

    // private class
    private class CallWebAppInterface {
        public String mCallState = CALL_STATE_CREATING_CALL_VIEW;
        private Timer mCallTimeoutTimer = null;

        CallWebAppInterface() {
            if (null == mCallingRoom) {
                throw new AssertionError("MXChromeCall : room cannot be null");
            }
        }

        // JS <-> android calls
        @JavascriptInterface
        public String wgetCallId() {
            return mCallId;
        }

        @JavascriptInterface
        public String wgetRoomId() {
            return mCallSignalingRoom.getRoomId();
        }

        @JavascriptInterface
        public String wgetTurnServer() {
            if (null != mTurnServer) {
                return mTurnServer.toString();
            } else {
                return null;
            }
        }

        @JavascriptInterface
        public void wlog(String message) {
            Log.d(LOG_TAG, "WebView Message : " + message);
        }

        @JavascriptInterface
        public void wCallError(String message) {
            Log.e(LOG_TAG, "WebView error Message : " + message);
            if ("ice_failed".equals(message)) {
                dispatchOnCallError(CALL_ERROR_ICE_FAILED);
            } else if ("user_media_failed".equals(message)) {
                dispatchOnCallError(CALL_ERROR_CAMERA_INIT_FAILED);
            }
        }

        @JavascriptInterface
        public void wOnStateUpdate(String jsstate) {
            String nextState = null;

            if ("fledgling".equals(jsstate)) {
                nextState = CALL_STATE_READY;
            } else if ("wait_local_media".equals(jsstate)) {
                nextState = CALL_STATE_WAIT_LOCAL_MEDIA;
            } else if ("create_offer".equals(jsstate)) {
                nextState = CALL_STATE_WAIT_CREATE_OFFER;
            } else if ("invite_sent".equals(jsstate)) {
                nextState = CALL_STATE_INVITE_SENT;
            } else if ("ringing".equals(jsstate)) {
                nextState = CALL_STATE_RINGING;
            } else if ("create_answer".equals(jsstate)) {
                nextState = CALL_STATE_CREATE_ANSWER;
            } else if ("connecting".equals(jsstate)) {
                nextState = CALL_STATE_CONNECTING;
            } else if ("connected".equals(jsstate)) {
                nextState = CALL_STATE_CONNECTED;
            } else if ("ended".equals(jsstate)) {
                nextState = CALL_STATE_ENDED;
            }

            // is there any state update ?
            if ((null != nextState) && !mCallState.equals(nextState)) {
                mCallState = nextState;

                // warn in the UI thread
                mUIThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // call timeout management
                        if (CALL_STATE_CONNECTING.equals(mCallState) || CALL_STATE_CONNECTING.equals(mCallState)) {
                            if (null != mCallTimeoutTimer) {
                                mCallTimeoutTimer.cancel();
                                mCallTimeoutTimer = null;
                            }
                        }

                        dispatchOnStateDidChange(mCallState);
                    }
                });
            }
        }

        @JavascriptInterface
        public void wOnLoaded() {
            mCallState = CALL_STATE_READY;

            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    dispatchOnReady();
                }
            });
        }

        private void sendHangup(final Event event) {
            if (null != mCallTimeoutTimer) {
                mCallTimeoutTimer.cancel();
                mCallTimeoutTimer = null;
            }

            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    dispatchOnCallEnd(END_CALL_REASON_UNDEFINED);
                }
            });

            mPendingEvents.clear();

            mCallSignalingRoom.sendEvent(event, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void info) {
                }

                @Override
                public void onNetworkError(Exception e) {
                    // try again
                    sendHangup(event);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                }

                @Override
                public void onUnexpectedError(Exception e) {
                }
            });
        }

        @JavascriptInterface
        public void wSendEvent(final String roomId, final String eventType, final String jsonContent) {
            mUIThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean addIt = true;
                        JsonObject content = (JsonObject) new JsonParser().parse(jsonContent);

                        // merge candidates
                        if (TextUtils.equals(eventType, Event.EVENT_TYPE_CALL_CANDIDATES) && (mPendingEvents.size() > 0)) {
                            try {
                                Event lastEvent = mPendingEvents.get(mPendingEvents.size() - 1);

                                if (TextUtils.equals(lastEvent.getType(), Event.EVENT_TYPE_CALL_CANDIDATES)) {
                                    JsonObject lastContent = lastEvent.getContentAsJsonObject();

                                    JsonArray lastContentCandidates = lastContent.get("candidates").getAsJsonArray();
                                    JsonArray newContentCandidates = content.get("candidates").getAsJsonArray();

                                    Log.d(LOG_TAG, "Merge candidates from " + lastContentCandidates.size()
                                            + " to " + (lastContentCandidates.size() + newContentCandidates.size() + " items."));

                                    lastContentCandidates.addAll(newContentCandidates);

                                    lastContent.remove("candidates");
                                    lastContent.add("candidates", lastContentCandidates);
                                    addIt = false;
                                }
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "## wSendEvent() ; " + e.getMessage(), e);
                            }
                        }

                        if (addIt) {
                            Event event = new Event(eventType, content, mSession.getCredentials().getUserId(), mCallSignalingRoom.getRoomId());

                            if (null != event) {
                                // receive an hangup -> close the window asap
                                if (TextUtils.equals(eventType, Event.EVENT_TYPE_CALL_HANGUP)) {
                                    sendHangup(event);
                                } else {
                                    mPendingEvents.add(event);
                                }

                                // the calleee has 30s to answer to call
                                if (TextUtils.equals(eventType, Event.EVENT_TYPE_CALL_INVITE)) {
                                    try {
                                        mCallTimeoutTimer = new Timer();
                                        mCallTimeoutTimer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                try {
                                                    if (getCallState().equals(IMXCall.CALL_STATE_RINGING)
                                                            || getCallState().equals(IMXCall.CALL_STATE_INVITE_SENT)) {
                                                        dispatchOnCallError(CALL_ERROR_USER_NOT_RESPONDING);
                                                        hangup(null);
                                                    }

                                                    // cancel the timer
                                                    mCallTimeoutTimer.cancel();
                                                    mCallTimeoutTimer = null;
                                                } catch (Exception e) {
                                                    Log.e(LOG_TAG, "## wSendEvent() ; " + e.getMessage(), e);
                                                }
                                            }
                                        }, CALL_TIMEOUT_MS);
                                    } catch (Throwable throwable) {
                                        if (null != mCallTimeoutTimer) {
                                            mCallTimeoutTimer.cancel();
                                            mCallTimeoutTimer = null;
                                        }
                                        Log.e(LOG_TAG, "## wSendEvent() ; " + throwable.getMessage(), throwable);
                                    }
                                }
                            }
                        }

                        // send events
                        sendNextEvent();

                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## wSendEvent() ; " + e.getMessage(), e);
                    }
                }
            });
        }
    }
}
