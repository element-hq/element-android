/*
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

package im.vector.matrix.android.internal.legacy.data.timeline;

import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import im.vector.matrix.android.internal.legacy.MXDataHandler;
import im.vector.matrix.android.internal.legacy.call.MXCall;
import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.rest.model.bingrules.BingRule;
import im.vector.matrix.android.internal.legacy.util.BingRulesManager;
import im.vector.matrix.android.internal.legacy.util.Log;

/**
 * This class is responsible for handling push rules for an event
 */
class TimelinePushWorker {

    private static final String LOG_TAG = TimelinePushWorker.class.getSimpleName();

    private final MXDataHandler mDataHandler;

    TimelinePushWorker(@NonNull final MXDataHandler dataHandler) {
        mDataHandler = dataHandler;
    }

    /**
     * Trigger a push if there is a dedicated push rules which implies it.
     *
     * @param event the event
     */
    public void triggerPush(@NonNull final RoomState state,
                            @NonNull final Event event) {
        BingRule bingRule;
        boolean outOfTimeEvent = false;
        long maxLifetime = 0;
        long eventLifetime = 0;
        final JsonObject eventContent = event.getContentAsJsonObject();
        if (eventContent != null && eventContent.has("lifetime")) {
            maxLifetime = eventContent.get("lifetime").getAsLong();
            eventLifetime = System.currentTimeMillis() - event.getOriginServerTs();
            outOfTimeEvent = eventLifetime > maxLifetime;
        }
        final BingRulesManager bingRulesManager = mDataHandler.getBingRulesManager();
        // If the bing rules apply, bing
        if (!outOfTimeEvent
                && bingRulesManager != null
                && (bingRule = bingRulesManager.fulfilledBingRule(event)) != null) {

            if (bingRule.shouldNotify()) {
                // bing the call events only if they make sense
                if (Event.EVENT_TYPE_CALL_INVITE.equals(event.getType())) {
                    long lifeTime = event.getAge();
                    if (Long.MAX_VALUE == lifeTime) {
                        lifeTime = System.currentTimeMillis() - event.getOriginServerTs();
                    }
                    if (lifeTime > MXCall.CALL_TIMEOUT_MS) {
                        Log.d(LOG_TAG, "IGNORED onBingEvent rule id " + bingRule.ruleId + " event id " + event.eventId
                                + " in " + event.roomId);
                        return;
                    }
                }
                Log.d(LOG_TAG, "onBingEvent rule id " + bingRule.ruleId + " event id " + event.eventId + " in " + event.roomId);
                mDataHandler.onBingEvent(event, state, bingRule);
            } else {
                Log.d(LOG_TAG, "rule id " + bingRule.ruleId + " event id " + event.eventId
                        + " in " + event.roomId + " has a mute notify rule");
            }
        } else if (outOfTimeEvent) {
            Log.e(LOG_TAG, "outOfTimeEvent for " + event.eventId + " in " + event.roomId);
            Log.e(LOG_TAG, "outOfTimeEvent maxlifetime " + maxLifetime + " eventLifeTime " + eventLifetime);
        }
    }
}
