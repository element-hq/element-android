package im.vector.matrix.android.api.rooms.timeline;

import im.vector.matrix.android.api.events.Event;
import im.vector.matrix.android.api.rooms.RoomState;

/**
 * A `EventTimeline` instance represents a contiguous sequence of events in a room.
 * <p>
 * There are two kinds of timeline:
 * <p>
 * - live timelines: they receive live events from the events stream. You can paginate
 * backwards but not forwards.
 * <p>
 * - past timelines: they start in the past from an `initialEventId`. They are filled
 * with events on calls of [MXEventTimeline paginate] in backwards or forwards direction.
 */
public interface EventTimeline {

    /**
     * @return The state of the room at the top most recent event of the timeline.
     */
    RoomState getState();

    /**
     * The direction from which an incoming event is considered.
     */
    enum Direction {
        /**
         * Forwards when the event is added to the end of the timeline.
         * These events come from the /sync stream or from forwards pagination.
         */
        FORWARDS,

        /**
         * Backwards when the event is added to the start of the timeline.
         * These events come from a back pagination.
         */
        BACKWARDS
    }

    interface Listener {

        /**
         * Call when an event has been handled in the timeline.
         *
         * @param event     the event.
         * @param direction the direction.
         * @param roomState the room state
         */
        void onEvent(Event event, Direction direction, RoomState roomState);
    }
}
