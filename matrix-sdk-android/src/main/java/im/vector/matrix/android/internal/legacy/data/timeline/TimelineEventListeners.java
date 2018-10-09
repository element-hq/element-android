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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import im.vector.matrix.android.internal.legacy.data.RoomState;
import im.vector.matrix.android.internal.legacy.rest.model.Event;
import im.vector.matrix.android.internal.legacy.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Handle the timeline event listeners
 * Is responsible for dispatching events
 */
class TimelineEventListeners {

    private static final String LOG_TAG = TimelineEventListeners.class.getSimpleName();

    // The inner listeners
    private final List<EventTimeline.Listener> mListeners = new ArrayList<>();

    /**
     * Add an events listener.
     *
     * @param listener the listener to add.
     */
    public void add(@Nullable final EventTimeline.Listener listener) {
        if (listener != null) {
            synchronized (this) {
                if (!mListeners.contains(listener)) {
                    mListeners.add(listener);
                }
            }
        }
    }

    /**
     * Remove an events listener.
     *
     * @param listener the listener to remove.
     */
    public void remove(@Nullable final EventTimeline.Listener listener) {
        if (null != listener) {
            synchronized (this) {
                mListeners.remove(listener);
            }
        }
    }

    /**
     * Dispatch the onEvent callback.
     *
     * @param event     the event.
     * @param direction the direction.
     * @param roomState the roomState.
     */
    public void onEvent(@NonNull final Event event,
                        @NonNull final EventTimeline.Direction direction,
                        @NonNull final RoomState roomState) {
        // ensure that the listeners are called in the UI thread
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            final List<EventTimeline.Listener> listeners;
            synchronized (this) {
                listeners = new ArrayList<>(mListeners);
            }
            for (EventTimeline.Listener listener : listeners) {
                try {
                    listener.onEvent(event, direction, roomState);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "EventTimeline.onEvent " + listener + " crashes " + e.getMessage(), e);
                }
            }
        } else {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    onEvent(event, direction, roomState);
                }
            });
        }
    }


}
