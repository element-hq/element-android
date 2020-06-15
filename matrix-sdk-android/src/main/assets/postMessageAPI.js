var android_widget_events = {};

var sendObjectMessageToRiotAndroid = function(parameters) {
    Android.onWidgetEvent(JSON.stringify(parameters));
};

var onWidgetMessageToRiotAndroid = function(event) {
    /* Use an internal "_id" field for matching onMessage events and requests
       _id was originally used by the Modular API. Keep it */
    if (!event.data._id) {
        /* The Matrix Widget API v2 spec says:
           "The requestId field should be unique and included in all requests" */
        event.data._id = event.data.requestId;
    }
    /* Make sure to have one id */
    if (!event.data._id) {
        event.data._id = Date.now() + "-" + Math.random().toString(36);
    }

    console.log("onWidgetMessageToRiotAndroid " + event.data._id);

    if (android_widget_events[event.data._id]) {
        console.log("onWidgetMessageToRiotAndroid : already managed");
        return;
    }

    if (!event.origin) {
        event.origin = event.originalEvent.origin;
    }

    android_widget_events[event.data._id] = event;

    console.log("onWidgetMessageToRiotAndroid : manage " + event.data);
    sendObjectMessageToRiotAndroid({'event.data': event.data});
};

var sendResponseFromRiotAndroid = function(eventId, res) {
    var event = android_widget_events[eventId];

    console.log("sendResponseFromRiotAndroid to " + event.data.action + " for "+ eventId + ": " + JSON.stringify(res));

    var data = JSON.parse(JSON.stringify(event.data));

    data.response = res;

    console.log("sendResponseFromRiotAndroid  ---> " + data);

    event.source.postMessage(data, event.origin);
    android_widget_events[eventId] = true;

    console.log("sendResponseFromRiotAndroid to done");
};

window.addEventListener('message', onWidgetMessageToRiotAndroid, false);
