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
package im.vector.matrix.android.internal.legacy.util;

import android.text.TextUtils;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.MXSession;
import im.vector.matrix.android.internal.legacy.data.MyUser;
import im.vector.matrix.android.internal.legacy.data.Room;
import im.vector.matrix.android.internal.legacy.listeners.IMXNetworkEventListener;
import im.vector.matrix.android.internal.legacy.network.NetworkConnectivityReceiver;
import im.vector.matrix.android.internal.legacy.rest.callback.ApiCallback;
import im.vector.matrix.android.internal.legacy.rest.callback.SimpleApiCallback;
import im.vector.matrix.android.internal.legacy.rest.client.PushRulesRestClient;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.MatrixError;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.Condition;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.ContainsDisplayNameCondition;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.ContentRule;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.EventMatchCondition;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.PushRuleSet;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.PushRulesResponse;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.RoomMemberCountCondition;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.SenderNotificationPermissionCondition;
import im.vector.matrix.android.internal.legacy.rest.model.message.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Object that gets and processes bing rules from the server.
 */
public class BingRulesManager {
    private static final String LOG_TAG = BingRulesManager.class.getSimpleName();

    /**
     * Bing rule listener
     */
    public interface onBingRuleUpdateListener {
        /**
         * The manager succeeds to update the bingrule enable status.
         */
        void onBingRuleUpdateSuccess();

        /**
         * The manager fails to update the bingrule enable status.
         *
         * @param errorMessage the error message.
         */
        void onBingRuleUpdateFailure(String errorMessage);
    }

    /**
     * Bing rules update
     */
    public interface onBingRulesUpdateListener {
        /**
         * Warn that some bing rules have been updated
         */
        void onBingRulesUpdate();
    }

    // general members
    private final PushRulesRestClient mApiClient;
    private final MXSession mSession;
    private final String mMyUserId;
    private final MXDataHandler mDataHandler;

    // the rules set to apply
    private PushRuleSet mRulesSet = new PushRuleSet();

    // the rules list
    private final List<BingRule> mRules = new ArrayList<>();

    // the default bing rule
    private BingRule mDefaultBingRule = new BingRule(true);

    // tell if the bing rules set is initialized
    private boolean mIsInitialized = false;

    // map to check if a room is "mention only"
    private final Map<String, Boolean> mIsMentionOnlyMap = new HashMap<>();

    // network management
    private NetworkConnectivityReceiver mNetworkConnectivityReceiver;
    private IMXNetworkEventListener mNetworkListener;
    private ApiCallback<Void> mLoadRulesCallback;

    //  listener
    private final Set<onBingRulesUpdateListener> mBingRulesUpdateListeners = new HashSet<>();

    /**
     * Defines the room notification state
     */
    public enum RoomNotificationState {
        /**
         * All the messages will trigger a noisy notification
         */
        ALL_MESSAGES_NOISY,

        /**
         * All the messages will trigger a notification
         */
        ALL_MESSAGES,

        /**
         * Only the messages with user display name / user name will trigger notifications
         */
        MENTIONS_ONLY,

        /**
         * No notifications
         */
        MUTE
    }

    private Map<String, RoomNotificationState> mRoomNotificationStateByRoomId = new HashMap<>();

    /**
     * Constructor
     *
     * @param session                     the session
     * @param networkConnectivityReceiver the network events listener
     */
    public BingRulesManager(MXSession session, NetworkConnectivityReceiver networkConnectivityReceiver) {
        mSession = session;
        mApiClient = session.getBingRulesApiClient();
        mMyUserId = session.getCredentials().getUserId();
        mDataHandler = session.getDataHandler();

        mNetworkListener = new IMXNetworkEventListener() {
            @Override
            public void onNetworkConnectionUpdate(boolean isConnected) {
                // mLoadRulesCallback is set when a loadRules failed
                // so when a network is available, trigger again loadRules
                if (isConnected && (null != mLoadRulesCallback)) {
                    loadRules(mLoadRulesCallback);
                }
            }
        };

        mNetworkConnectivityReceiver = networkConnectivityReceiver;
        networkConnectivityReceiver.addEventListener(mNetworkListener);
    }

    /**
     * @return true if it is ready to be used (i.e initializedà
     */
    public boolean isReady() {
        return mIsInitialized;
    }

    /**
     * Remove the network events listener.
     * This listener is only used to initialize the rules at application launch.
     */
    private void removeNetworkListener() {
        if ((null != mNetworkConnectivityReceiver) && (null != mNetworkListener)) {
            mNetworkConnectivityReceiver.removeEventListener(mNetworkListener);
            mNetworkConnectivityReceiver = null;
            mNetworkListener = null;
        }
    }

    /**
     * Add a listener
     *
     * @param listener the listener
     */
    public void addBingRulesUpdateListener(onBingRulesUpdateListener listener) {
        if (null != listener) {
            mBingRulesUpdateListeners.add(listener);
        }
    }

    /**
     * remove a listener
     *
     * @param listener the listener
     */
    public void removeBingRulesUpdateListener(onBingRulesUpdateListener listener) {
        if (null != listener) {
            mBingRulesUpdateListeners.remove(listener);
        }
    }

    /**
     * Some rules have been updated.
     */
    private void onBingRulesUpdate() {
        // delete cached data
        mRoomNotificationStateByRoomId.clear();

        for (onBingRulesUpdateListener listener : mBingRulesUpdateListeners) {
            try {
                listener.onBingRulesUpdate();
            } catch (Exception e) {
                Log.e(LOG_TAG, "## onBingRulesUpdate() : onBingRulesUpdate failed " + e.getMessage(), e);
            }
        }
    }

    /**
     * Load the bing rules from the server.
     *
     * @param callback an async callback called when the rules are loaded
     */
    public void loadRules(final ApiCallback<Void> callback) {
        mLoadRulesCallback = null;

        Log.d(LOG_TAG, "## loadRules() : refresh the bing rules");
        mApiClient.getAllRules(new ApiCallback<PushRulesResponse>() {
            @Override
            public void onSuccess(PushRulesResponse info) {
                Log.d(LOG_TAG, "## loadRules() : succeeds");

                buildRules(info);
                mIsInitialized = true;

                if (callback != null) {
                    callback.onSuccess(null);
                }

                removeNetworkListener();
            }

            private void onError(String errorMessage) {
                Log.e(LOG_TAG, "## loadRules() : failed " + errorMessage);
                // the callback will be called when the request will succeed
                mLoadRulesCallback = callback;
            }

            @Override
            public void onNetworkError(Exception e) {
                onError(e.getMessage());

                if (null != callback) {
                    callback.onNetworkError(e);
                }
            }

            @Override
            public void onMatrixError(MatrixError e) {
                onError(e.getMessage());

                if (null != callback) {
                    callback.onMatrixError(e);
                }
            }

            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getMessage());

                if (null != callback) {
                    callback.onUnexpectedError(e);
                }
            }
        });
    }

    /**
     * Returns whether a string contains an occurrence of another, as a standalone word, regardless of case.
     *
     * @param subString  the string to search for
     * @param longString the string to search in
     * @return whether a match was found
     */
    private static boolean caseInsensitiveFind(String subString, String longString) {
        // sanity check
        if (TextUtils.isEmpty(subString) || TextUtils.isEmpty(longString)) {
            return false;
        }

        boolean found = false;

        try {
            Pattern pattern = Pattern.compile("(\\W|^)" + subString + "(\\W|$)", Pattern.CASE_INSENSITIVE);
            found = pattern.matcher(longString).find();
        } catch (Exception e) {
            Log.e(LOG_TAG, "caseInsensitiveFind : pattern.matcher failed with " + e.getMessage(), e);
        }

        return found;
    }

    /**
     * Returns the first highlighted notifiable bing rule which fulfills its condition with this event.
     *
     * @param event the event
     * @return the first matched bing rule, null if none
     */
    public BingRule fulfilledHighlightBingRule(Event event) {
        return fulfilledBingRule(event, true);
    }

    /**
     * Returns the first notifiable bing rule which fulfills its condition with this event.
     *
     * @param event the event
     * @return the first matched bing rule, null if none
     */
    public BingRule fulfilledBingRule(Event event) {
        return fulfilledBingRule(event, false);
    }

    /**
     * Returns the first notifiable bing rule which fulfills its condition with this event.
     *
     * @param event             the event
     * @param highlightRuleOnly true to only check the highlight rule
     * @return the first matched bing rule, null if none
     */
    private BingRule fulfilledBingRule(Event event, boolean highlightRuleOnly) {
        // sanity check
        if (null == event) {
            Log.e(LOG_TAG, "## fulfilledBingRule() : null event");
            return null;
        }

        if (!mIsInitialized) {
            Log.e(LOG_TAG, "## fulfilledBingRule() : not initialized");
            return null;
        }

        if (0 == mRules.size()) {
            Log.e(LOG_TAG, "## fulfilledBingRule() : no rules");
            return null;
        }

        // do not trigger notification for oneself messages
        if ((null != event.getSender()) && TextUtils.equals(event.getSender(), mMyUserId)) {
            return null;
        }

        String eventType = event.getType();

        // some types are not bingable
        if (TextUtils.equals(eventType, Event.EVENT_TYPE_PRESENCE)
                || TextUtils.equals(eventType, Event.EVENT_TYPE_TYPING)
                || TextUtils.equals(eventType, Event.EVENT_TYPE_REDACTION)
                || TextUtils.equals(eventType, Event.EVENT_TYPE_RECEIPT)
                || TextUtils.equals(eventType, Event.EVENT_TYPE_TAGS)) {
            return null;
        }

        // GA issue
        final List<BingRule> rules;

        synchronized (this) {
            rules = new ArrayList<>(mRules);
        }

        // Go down the rule list until we find a match
        for (BingRule bingRule : rules) {
            if (bingRule.isEnabled && (!highlightRuleOnly || bingRule.shouldHighlight())) {
                boolean isFullfilled = false;

                // some rules have no condition
                // so their ruleId defines the method
                if (BingRule.RULE_ID_CONTAIN_USER_NAME.equals(bingRule.ruleId) || BingRule.RULE_ID_CONTAIN_DISPLAY_NAME.equals(bingRule.ruleId)) {
                    if (Event.EVENT_TYPE_MESSAGE.equals(event.getType())) {
                        Message message = JsonUtils.toMessage(event.getContent());
                        MyUser myUser = mSession.getMyUser();
                        String pattern = null;

                        if (BingRule.RULE_ID_CONTAIN_USER_NAME.equals(bingRule.ruleId)) {
                            if (mMyUserId.indexOf(":") >= 0) {
                                pattern = mMyUserId.substring(1, mMyUserId.indexOf(":"));
                            } else {
                                pattern = mMyUserId;
                            }
                        } else if (BingRule.RULE_ID_CONTAIN_DISPLAY_NAME.equals(bingRule.ruleId)) {
                            pattern = myUser.displayname;
                            if ((null != mSession.getDataHandler()) && (null != mSession.getDataHandler().getStore())) {
                                Room room = mSession.getDataHandler().getStore().getRoom(event.roomId);

                                if ((null != room) && (null != room.getState())) {
                                    String disambiguousedName = room.getState().getMemberName(mMyUserId);

                                    if (!TextUtils.equals(disambiguousedName, mMyUserId)) {
                                        pattern = Pattern.quote(disambiguousedName);
                                    }
                                }
                            }
                        }

                        if (!TextUtils.isEmpty(pattern)) {
                            isFullfilled = caseInsensitiveFind(pattern, message.body);
                        }
                    }
                } else if (BingRule.RULE_ID_FALLBACK.equals(bingRule.ruleId)) {
                    isFullfilled = true;
                } else {
                    // some default rules define conditions
                    // so use them instead of doing a custom treatment
                    // RULE_ID_ONE_TO_ONE_ROOM
                    // RULE_ID_SUPPRESS_BOTS_NOTIFICATIONS
                    isFullfilled = eventMatchesConditions(event, bingRule.conditions);
                }

                if (isFullfilled) {
                    return bingRule;
                }
            }
        }

        // no rules are fulfilled
        return null;
    }

    /**
     * Check if an event matches a conditions set
     *
     * @param event      the event to test
     * @param conditions the conditions set
     * @return true if the event matches all the conditions set.
     */
    private boolean eventMatchesConditions(Event event, List<Condition> conditions) {
        try {
            if ((conditions != null) && (event != null)) {
                for (Condition condition : conditions) {
                    if (condition instanceof EventMatchCondition) {
                        if (!((EventMatchCondition) condition).isSatisfied(event)) {
                            return false;
                        }
                    } else if (condition instanceof ContainsDisplayNameCondition) {
                        String myDisplayName = null;

                        if (event.roomId != null) {
                            Room room = mDataHandler.getRoom(event.roomId, false);

                            // sanity checks
                            if (room != null && room.getMember(mMyUserId) != null) {
                                // Best way to get your display name for now
                                myDisplayName = room.getMember(mMyUserId).displayname;
                            }
                        }

                        if (TextUtils.isEmpty(myDisplayName)) {
                            // RoomMember is maybe not known due to lazy loading
                            // Get displayName from the session
                            myDisplayName = mSession.getMyUser().displayname;
                        }

                        if (!((ContainsDisplayNameCondition) condition).isSatisfied(event, myDisplayName)) {
                            return false;
                        }
                    } else if (condition instanceof RoomMemberCountCondition) {
                        if (event.roomId != null) {
                            Room room = mDataHandler.getRoom(event.roomId, false);

                            if (!((RoomMemberCountCondition) condition).isSatisfied(room)) {
                                return false;
                            }
                        }
                    } else if (condition instanceof SenderNotificationPermissionCondition) {
                        if (event.roomId != null) {
                            Room room = mDataHandler.getRoom(event.roomId, false);

                            if (!((SenderNotificationPermissionCondition) condition).isSatisfied(room.getState().getPowerLevels(), event.sender)) {
                                return false;
                            }
                        }
                    } else {
                        // unknown conditions: we previously matched all unknown conditions,
                        // but given that rules can be added to the base rules on a server,
                        // it's probably better to not match unknown conditions.
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "## eventMatchesConditions() failed " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Build the internal push rules
     *
     * @param pushRulesResponse the server request response.
     */
    public void buildRules(PushRulesResponse pushRulesResponse) {
        if (null != pushRulesResponse) {
            updateRulesSet(pushRulesResponse.global);
            onBingRulesUpdate();
        }
    }

    /**
     * @return the rules set
     */
    public PushRuleSet pushRules() {
        return mRulesSet;
    }

    /**
     * Update mRulesSet with the new one.
     *
     * @param ruleSet the new ruleSet to apply
     */
    private void updateRulesSet(PushRuleSet ruleSet) {
        synchronized (this) {
            // clear the rules list
            // it is
            mRules.clear();

            // sanity check
            if (null == ruleSet) {
                mRulesSet = new PushRuleSet();
                return;
            }

            // Replace the list by ArrayList to be able to add/remove rules
            // Add the rule kind in each rule
            // Ensure that the null pointers are replaced by an empty list
            if (ruleSet.override != null) {
                ruleSet.override = new ArrayList<>(ruleSet.override);
                for (BingRule rule : ruleSet.override) {
                    rule.kind = BingRule.KIND_OVERRIDE;
                }
                mRules.addAll(ruleSet.override);
            } else {
                ruleSet.override = new ArrayList<>(ruleSet.override);
            }

            if (ruleSet.content != null) {
                ruleSet.content = new ArrayList<>(ruleSet.content);
                for (BingRule rule : ruleSet.content) {
                    rule.kind = BingRule.KIND_CONTENT;
                }
                addContentRules(ruleSet.content);
            } else {
                ruleSet.content = new ArrayList<>();
            }

            mIsMentionOnlyMap.clear();
            if (ruleSet.room != null) {
                ruleSet.room = new ArrayList<>(ruleSet.room);

                for (BingRule rule : ruleSet.room) {
                    rule.kind = BingRule.KIND_ROOM;
                }
                addRoomRules(ruleSet.room);
            } else {
                ruleSet.room = new ArrayList<>();
            }

            if (ruleSet.sender != null) {
                ruleSet.sender = new ArrayList<>(ruleSet.sender);

                for (BingRule rule : ruleSet.sender) {
                    rule.kind = BingRule.KIND_SENDER;
                }
                addSenderRules(ruleSet.sender);
            } else {
                ruleSet.sender = new ArrayList<>();
            }

            if (ruleSet.underride != null) {
                ruleSet.underride = new ArrayList<>(ruleSet.underride);
                for (BingRule rule : ruleSet.underride) {
                    rule.kind = BingRule.KIND_UNDERRIDE;
                }
                mRules.addAll(ruleSet.underride);
            } else {
                ruleSet.underride = new ArrayList<>();
            }

            mRulesSet = ruleSet;

            Log.d(LOG_TAG, "## updateRules() : has " + mRules.size() + " rules");
        }
    }

    /**
     * Create a content EventMatchConditions list from a ContentRules list
     *
     * @param rules the ContentRules list
     */
    private void addContentRules(List<ContentRule> rules) {
        // sanity check
        if (null != rules) {
            for (ContentRule rule : rules) {
                EventMatchCondition condition = new EventMatchCondition();
                condition.kind = Condition.KIND_EVENT_MATCH;
                condition.key = "content.body";
                condition.pattern = rule.pattern;

                rule.addCondition(condition);

                mRules.add(rule);
            }
        }
    }

    /**
     * Create a room EventMatchConditions list from a BingRule list
     *
     * @param rules the BingRule list
     */
    private void addRoomRules(List<BingRule> rules) {
        if (null != rules) {
            for (BingRule rule : rules) {
                EventMatchCondition condition = new EventMatchCondition();
                condition.kind = Condition.KIND_EVENT_MATCH;
                condition.key = "room_id";
                condition.pattern = rule.ruleId;

                rule.addCondition(condition);

                mRules.add(rule);
            }
        }
    }

    /**
     * Create a sender EventMatchConditions list from a BingRule list
     *
     * @param rules the BingRule list
     */
    private void addSenderRules(List<BingRule> rules) {
        if (null != rules) {
            for (BingRule rule : rules) {
                EventMatchCondition condition = new EventMatchCondition();
                condition.kind = Condition.KIND_EVENT_MATCH;
                condition.key = "user_id";
                condition.pattern = rule.ruleId;

                rule.addCondition(condition);

                mRules.add(rule);
            }
        }
    }

    /**
     * Force to refresh the rules.
     * The listener is called when the rules are refreshed.
     *
     * @param errorMsg the error message to dispatch.
     * @param listener the asynchronous listener
     */
    private void forceRulesRefresh(final String errorMsg, final onBingRuleUpdateListener listener) {
        // refresh only there is a listener
        if (null != listener) {
            // clear cached data
            mRoomNotificationStateByRoomId.clear();

            loadRules(new ApiCallback<Void>() {
                private void onDone(String error) {
                    // clear cached data
                    mRoomNotificationStateByRoomId.clear();

                    try {
                        if (TextUtils.isEmpty(error) && TextUtils.isEmpty(errorMsg)) {
                            listener.onBingRuleUpdateSuccess();
                        } else {
                            listener.onBingRuleUpdateFailure(TextUtils.isEmpty(errorMsg) ? error : errorMsg);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## forceRulesRefresh() : failed " + e.getMessage(), e);
                    }
                }

                @Override
                public void onSuccess(Void info) {
                    onDone(null);
                }

                @Override
                public void onNetworkError(Exception e) {
                    onDone(e.getLocalizedMessage());
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    onDone(e.getLocalizedMessage());
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    onDone(e.getLocalizedMessage());
                }
            });
        }
    }

    /**
     * Get the rules update callback.
     *
     * @param listener the listener
     * @return the asynchronous callback
     */
    private ApiCallback<Void> getUpdateCallback(final onBingRuleUpdateListener listener) {
        return new ApiCallback<Void>() {
            @Override
            public void onSuccess(Void info) {
                forceRulesRefresh(null, listener);
            }

            private void onError(String message) {
                forceRulesRefresh(message, listener);
            }

            /**
             * Called if there is a network error.
             *
             * @param e the exception
             */
            @Override
            public void onNetworkError(Exception e) {
                onError(e.getLocalizedMessage());
            }

            /**
             * Called in case of a Matrix error.
             *
             * @param e the Matrix error
             */
            @Override
            public void onMatrixError(MatrixError e) {
                onError(e.getLocalizedMessage());
            }

            /**
             * Called for some other type of error.
             *
             * @param e the exception
             */
            @Override
            public void onUnexpectedError(Exception e) {
                onError(e.getLocalizedMessage());
            }
        };
    }

    /**
     * Update the rule enable status.
     * The rules lits are refreshed when the listener is called.
     *
     * @param rule     the bing rule to toggle.
     * @param listener the rule update listener.
     */
    public void updateEnableRuleStatus(final BingRule rule, final boolean isEnabled, final onBingRuleUpdateListener listener) {
        if (null != rule) {
            mApiClient.updateEnableRuleStatus(rule.kind, rule.ruleId, isEnabled, getUpdateCallback(listener));
        }
    }

    /**
     * Delete the rule.
     * The rules lists are refreshed when the listener is called.
     *
     * @param rule     the rule to delete.
     * @param listener the rule update listener.
     */
    public void deleteRule(final BingRule rule, final onBingRuleUpdateListener listener) {
        // null case
        if (null == rule) {
            if (listener != null) {
                try {
                    listener.onBingRuleUpdateSuccess();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## deleteRule : onBingRuleUpdateSuccess failed " + e.getMessage(), e);
                }
            }
            return;
        }

        mApiClient.deleteRule(rule.kind, rule.ruleId, getUpdateCallback(listener));
    }

    /**
     * Delete a rules list.
     * The rules lists are refreshed when the listener is called.
     *
     * @param rules    the rules to delete
     * @param listener the listener when the rules are deleted
     */
    public void deleteRules(final List<BingRule> rules, final onBingRuleUpdateListener listener) {
        deleteRules(rules, 0, listener);
    }

    /**
     * Recursive rules deletion method.
     *
     * @param rules    the rules to delete
     * @param index    the rule index
     * @param listener the listener when the rules are deleted
     */
    private void deleteRules(final List<BingRule> rules, final int index, final onBingRuleUpdateListener listener) {
        // sanity checks
        if ((null == rules) || (index >= rules.size())) {
            onBingRulesUpdate();
            if (null != listener) {
                try {
                    listener.onBingRuleUpdateSuccess();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## deleteRules() : onBingRuleUpdateSuccess failed " + e.getMessage(), e);
                }
            }

            return;
        }

        // delete the rule
        deleteRule(rules.get(index), new onBingRuleUpdateListener() {
            @Override
            public void onBingRuleUpdateSuccess() {
                deleteRules(rules, index + 1, listener);
            }

            @Override
            public void onBingRuleUpdateFailure(String errorMessage) {
                if (null != listener) {
                    try {
                        listener.onBingRuleUpdateFailure(errorMessage);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "## deleteRules() : onBingRuleUpdateFailure failed " + e.getMessage(), e);
                    }
                }
            }
        });
    }

    /**
     * Add a rule.
     * The rules lists are refreshed when the listener is called.
     *
     * @param rule     the rule to delete.
     * @param listener the rule update listener.
     */
    public void addRule(final BingRule rule, final onBingRuleUpdateListener listener) {
        // null case
        if (null == rule) {
            if (listener != null) {
                try {
                    listener.onBingRuleUpdateSuccess();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "## addRule : onBingRuleUpdateSuccess failed " + e.getMessage(), e);
                }
            }
            return;
        }

        mApiClient.addRule(rule, getUpdateCallback(listener));
    }

    /**
     * Update a bing rule.
     * The rules list are updated when the callback is called.
     *
     * @param source   the source
     * @param target   the target
     * @param listener the listener
     */
    public void updateRule(final BingRule source, final BingRule target, final onBingRuleUpdateListener listener) {
        if (null == source) {
            addRule(target, listener);
            return;
        }

        if (null == target) {
            deleteRule(source, listener);
            return;
        }

        if (source.isEnabled != target.isEnabled) {
            mApiClient.updateEnableRuleStatus(target.kind, target.ruleId, target.isEnabled, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void info) {
                    source.isEnabled = target.isEnabled;
                    updateRule(source, target, listener);
                }

                @Override
                public void onNetworkError(Exception e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }
            });

            return;
        }

        if (source.actions != target.actions) {
            Map<String, Object> map = new HashMap<>();
            List<Object> sortedActions = new ArrayList<>();

            // the webclient needs to have them sorted
            if (null != target.actions) {
                if (target.actions.contains(BingRule.ACTION_NOTIFY)) {
                    sortedActions.add(BingRule.ACTION_NOTIFY);
                }

                if (target.actions.contains(BingRule.ACTION_DONT_NOTIFY)) {
                    sortedActions.add(BingRule.ACTION_DONT_NOTIFY);
                }

                if (null != target.getActionMap(BingRule.ACTION_SET_TWEAK_SOUND_VALUE)) {
                    sortedActions.add(target.getActionMap(BingRule.ACTION_SET_TWEAK_SOUND_VALUE));
                }

                if (null != target.getActionMap(BingRule.ACTION_SET_TWEAK_HIGHLIGHT_VALUE)) {
                    sortedActions.add(target.getActionMap(BingRule.ACTION_SET_TWEAK_HIGHLIGHT_VALUE));
                }
            }

            map.put("actions", sortedActions);

            mApiClient.updateRuleActions(target.kind, target.ruleId, map, new SimpleApiCallback<Void>() {
                @Override
                public void onSuccess(Void info) {
                    source.actions = target.actions;
                    updateRule(source, target, listener);
                }

                @Override
                public void onNetworkError(Exception e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }

                @Override
                public void onMatrixError(MatrixError e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }

                @Override
                public void onUnexpectedError(Exception e) {
                    forceRulesRefresh(e.getLocalizedMessage(), listener);
                }
            });

            return;
        }

        // the update succeeds
        forceRulesRefresh(null, listener);
    }

    /**
     * Search the push rules for the room id
     *
     * @param roomId the room id
     * @return the room rules list
     */
    private List<BingRule> getPushRulesForRoomId(String roomId) {
        List<BingRule> rules = new ArrayList<>();

        // sanity checks
        if (!TextUtils.isEmpty(roomId) && (null != mRulesSet)) {
            // the webclient defines two ways to set a room rule
            // mention only : the user won't have any push for the room except if a content rule is fulfilled
            // mute : no notification for this room

            // mute rules are defined in override groups
            if (null != mRulesSet.override) {
                for (BingRule roomRule : mRulesSet.override) {
                    if (TextUtils.equals(roomRule.ruleId, roomId)) {
                        rules.add(roomRule);
                    }
                }
            }

            // mention only are defined in room group
            if (null != mRulesSet.room) {
                for (BingRule roomRule : mRulesSet.room) {
                    if (TextUtils.equals(roomRule.ruleId, roomId)) {
                        rules.add(roomRule);
                    }
                }
            }
        }

        return rules;
    }

    /**
     * Provide the room notification state
     *
     * @param roomId the room
     * @return the room notification state
     */
    public RoomNotificationState getRoomNotificationState(String roomId) {
        if (TextUtils.isEmpty(roomId)) {
            return RoomNotificationState.ALL_MESSAGES;
        }

        if (mRoomNotificationStateByRoomId.containsKey(roomId)) {
            return mRoomNotificationStateByRoomId.get(roomId);
        }

        RoomNotificationState result = RoomNotificationState.ALL_MESSAGES;
        List<BingRule> bingRules = getPushRulesForRoomId(roomId);

        for (BingRule rule : bingRules) {
            if (rule.isEnabled) {
                if (rule.shouldNotNotify()) {
                    result = TextUtils.equals(rule.kind, BingRule.KIND_OVERRIDE) ? RoomNotificationState.MUTE : RoomNotificationState.MENTIONS_ONLY;
                    break;
                } else if (rule.shouldNotify()) {
                    result = (null != rule.getNotificationSound()) ? RoomNotificationState.ALL_MESSAGES_NOISY : RoomNotificationState.ALL_MESSAGES;
                }
            }
        }

        mRoomNotificationStateByRoomId.put(roomId, result);
        return result;
    }

    /**
     * Update the notification state of a dedicated room
     *
     * @param roomId   the room id
     * @param state    the new state
     * @param listener the asynchronous callback
     */
    public void updateRoomNotificationState(final String roomId, final RoomNotificationState state, final onBingRuleUpdateListener listener) {
        List<BingRule> bingRules = getPushRulesForRoomId(roomId);

        deleteRules(bingRules, new onBingRuleUpdateListener() {
            @Override
            public void onBingRuleUpdateSuccess() {
                if (state == RoomNotificationState.ALL_MESSAGES) {
                    forceRulesRefresh(null, listener);
                } else {
                    BingRule rule;

                    if (state == RoomNotificationState.ALL_MESSAGES_NOISY) {
                        rule = new BingRule(BingRule.KIND_ROOM, roomId, true, false, true);
                    } else {
                        rule = new BingRule((state == RoomNotificationState.MENTIONS_ONLY) ?
                                BingRule.KIND_ROOM : BingRule.KIND_OVERRIDE, roomId, false, null, false);

                        EventMatchCondition condition = new EventMatchCondition();
                        condition.key = "room_id";
                        condition.pattern = roomId;
                        rule.addCondition(condition);

                    }

                    addRule(rule, listener);
                }
            }

            @Override
            public void onBingRuleUpdateFailure(String errorMessage) {
                listener.onBingRuleUpdateFailure(errorMessage);
            }
        });
    }

    /**
     * Tell whether the regular notifications are disabled for the room.
     *
     * @param roomId the room id
     * @return true if the regular notifications are disabled (mention only)
     */
    public boolean isRoomMentionOnly(String roomId) {
        return RoomNotificationState.MENTIONS_ONLY == getRoomNotificationState(roomId);
    }

    /**
     * Test if the room has a dedicated rule which disables notification.
     *
     * @param roomId the roomId
     * @return true if there is a rule to disable notifications.
     */
    public boolean isRoomNotificationsDisabled(String roomId) {
        RoomNotificationState state = getRoomNotificationState(roomId);
        return (RoomNotificationState.MENTIONS_ONLY == state) || (RoomNotificationState.MUTE == state);
    }
}
