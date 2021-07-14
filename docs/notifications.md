This document aims to describe how Element android displays notifications to the end user. It also clarifies notifications and background settings in the app.

# Table of Contents
1. [Prerequisites Knowledge](#prerequisites-knowledge)
    * [How does a matrix client get a message from a homeserver?](#how-does-a-matrix-client-get-a-message-from-a-homeserver)
    * [How does a mobile app receives push notification?](#how-does-a-mobile-app-receives-push-notification)
    * [Push VS Notification](#push-vs-notification)
    * [Push in the matrix federated world](#push-in-the-matrix-federated-world)
    * [How does the homeserver know when to notify a client?](#how-does-the-homeserver-know-when-to-notify-a-client)
    * [Push vs privacy, and mitigation](#push-vs-privacy-and-mitigation)
    * [Background processing limitations](#background-processing-limitations)
2. [Element Notification implementations](#element-notification-implementations)
    * [Requirements](#requirements) 
    * [Foreground sync mode (Gplay & F-Droid)](#foreground-sync-mode-gplay-f-droid)
    * [Push (FCM) received in background](#push-fcm-received-in-background)
    * [FCM Fallback mode](#fcm-fallback-mode)
    * [F-Droid background Mode](#f-droid-background-mode)
3. [Application Settings](#application-settings)


First let's start with some prerequisite knowledge

# Prerequisites Knowledge

## How does a matrix client get a message from a homeserver?

In order to get messages from a homeserver, a matrix client need to perform a ``sync`` operation.

`To read events, the intended flow of operation is for clients to first call the /sync API without a since parameter. This returns the most recent message events for each room, as well as the state of the room at the start of the returned timeline. `

The client need to call the `sync`API periodically in order to get incremental updates of the server state (new messages).
This mechanism is known as **HTTP long Polling**.

Using the **HTTP Long Polling** mechanism  a client polls a server requesting new information.
The server *holds the request open until new data is available*.
Once available, the server responds and sends the new information.
When the client receives the new information, it immediately sends another request, and the operation is repeated.
This effectively emulates a server push feature.

The HTTP long Polling can be fine tuned in the **SDK** using two parameters:
* timeout (Sync request timeout)
* delay (Delay between each sync)

**timeout** is a server parameter, defined by:
```
The maximum time to wait, in milliseconds, before returning this request.`
If no events (or other data) become available before this time elapses, the server will return a response with empty fields.
By default, this is 0, so the server will return immediately even if the response is empty.
```

**delay** is a client preference. When the server responds to a sync request, the client waits for `delay`before calling a new sync.

When the Element Android app is open (i.e in foreground state), the default timeout is 30 seconds, and delay is 0.

## How does a mobile app receives push notification

Push notification is used as a way to wake up a mobile application when some important information is available and should be processed.

Typically in order to get push notification, an application relies on a **Push Notification Service** or **Push Provider**.

For example iOS uses APNS (Apple Push Notification Service).
Most of android devices relies on Google's Firebase Cloud Messaging (FCM).
 > FCM has replaced Google Cloud Messaging (GCM - deprecated April 10 2018)

FCM will only work on android devices that have Google plays services installed
(In simple terms, Google Play Services is a background service that runs on Android, which in turn helps in integrating Google’s advanced functionalities to other applications)

De-Googlified devices need to rely on something else in order to stay up to date with a server.
There some cases when devices with google services cannot use FCM (network infrastructure limitations -firewalls- ,
 privacy and or independency requirement, source code licence)

## Push VS Notification

This need some disambiguation, because it is the source of common confusion:


*The fact that you see a notification on your screen does not mean that you have successfully configured your PUSH plateform.*

  Technically there is a difference between a push and a notification. A notification is what you see on screen and/or in the notification Menu/Drawer (in the top bar of the phone).

  Notifications are not always triggered by a push (One can display a notification locally triggered by an alarm)


## Push in the matrix federated world

In order to send a push to a mobile, App developers need to have a server that will use the FCM APIs, and these APIs requires authentication!
This server is called a **Push Gateway** in the matrix world

That means that Element Android, a matrix client created by New Vector, is using a **Push Gateway** with the needed credentials (FCM API secret Key) in order to send push to the New Vector client.

If you create your own matrix client, you will also need to deploy an instance of a **Push Gateway** with the credentials needed to use FCM for your app.

On registration, a matrix client must tell its homeserver what Push Gateway to use.

See [Sygnal](https://github.com/matrix-org/sygnal/) for a reference implementation.
```

                                  +--------------------+  +-------------------+
                 Matrix HTTP      |                    |  |                   |
            Notification Protocol |   App Developer    |  |   Device Vendor   |
                                  |                    |  |                   |
          +-------------------+   | +----------------+ |  | +---------------+ |
          |                   |   | |                | |  | |               | |
          | Matrix homeserver +----->  Push Gateway  +------> Push Provider | |
          |                   |   | |                | |  | |               | |
          +-^-----------------+   | +----------------+ |  | +----+----------+ |
            |                     |                    |  |      |            |
   Matrix   |                     |                    |  |      |            |
Client/Server API  +              |                    |  |      |            |
            |      |              +--------------------+  +-------------------+
            |   +--+-+                                           |
            |   |    <-------------------------------------------+
            +---+    |
                |    |          Provider Push Protocol
                +----+

        Mobile Device or Client
```

Recommended reading:
 * https://thomask.sdf.org/blog/2016/12/11/riots-magical-push-notifications-in-ios.html
* https://matrix.org/docs/spec/client_server/r0.4.0.html#id128


## How does the homeserver know when to notify a client?

This is defined by [**push rules**](https://matrix.org/docs/spec/client_server/r0.4.0.html#push-rules-).

`A push rule is a single rule that states under what conditions an event should be passed onto a push gateway and how the notification should be presented (sound / importance).`

A homeserver can be configured with default rules (for Direct messages, group messages, mentions, etc.. ).

There are different kind of push rules, it can be per room (each new message on this room should be notified), it can also define a pattern that a message should match (when you are mentioned, or key word based).

Notifications have 2 'levels' (`highlighted = true/false sound = default/custom`). In Element these notifications level are reflected as Noisy/Silent. 

**What about encrypted messages?**

Of course, content patterns matching cannot be used for encrypted messages server side (as the content is encrypted).

That is why clients are able to **process the push rules client side** to decide what kind of notification should be presented for a given event.

## Push vs privacy, and mitigation

As seen previously, App developers don't directly send a push to the end user's device, they use a Push Provider as intermediary. So technically this intermediary is able to read the content of what is sent.

App developers usually mitigate this by sending a `silent notification`, that is a notification with no identifiable data, or with an encrypted payload. When the push is received the app can then synchronise to it's server in order to generate a local notification.


## Background processing limitations

A mobile applications process live in a managed word, meaning that its process can be limited (e.g no network access), stopped or killed at almost anytime by the Operating System.

In order to improve the battery life of their devices some constructors started to implement mechanism to drastically limit background execution of applications (e.g MIUI/Xiaomi restrictions, Sony stamina mode). 
Then starting android M, android has also put more focus on improving device performances, introducing several IDLE modes, App-Standby, Light Doze, Doze.

In a nutshell, apps can't do much in background now.

If the devices is not plugged and stays IDLE for a certain amount of time, radio (mobile connectivity) and CPU can/will be turned off.

For an application like Element, where users can receive important information at anytime, the best option is to rely on a push system (Google's Firebase Message a.k.a FCM). FCM high priority push can wake up the device and whitelist an application to perform background task (for a limited but unspecified amount of time). 

Notice that this is still evolving, and in future versions application that has been 'background restricted' by users won't be able to wake up even when a high priority push is received. Also high priority notifications could be rate limited (not defined anywhere)

It's getting a lot more complicated when you cannot rely on FCM (because: closed sources, network/firewall restrictions, privacy concerns). 
The documentation on this subject is vague, and as per our experiments not always exact, also device's behaviour is fragmented. 

It is getting more and more complex to have reliable notifications when FCM is not used.

# Element Notification implementations

## Requirements

Element Android must work with and without FCM.
* The Element android app published on F-Droid do not rely on FCM (all related dependencies are not present)
* The Element android app published on google play rely on FCM, with a fallback mode when FCM registration has failed (e.g outdated or missing Google Play Services)

## Foreground sync mode (Gplay & F-Droid)

When in foreground, Element performs sync continuously with a timeout value set to 10 seconds (see HttpPooling).

As this mode does not need to live beyond the scope of the application, and as per Google recommendation, Element uses the internal app resources (Thread and Timers) to perform the syncs. 

This mode is turned on when the app enters foreground, and off when enters background.

In background, and depending on wether push is available or not, Element will use different methods to perform the syncs (Workers / Alarms / Service)

## Push (FCM) received in background 

In order to enable Push, Element must first get a push token from the firebase SDK, then register a pusher with this token on the homeserver.

When a message should be notified to a user,  the user's homeserver notifies the registered `push gateway` for Element, that is [sygnal](https://github.com/matrix-org/sygnal) _- The reference implementation for push gateways -_ hosted by matrix.org. 

This sygnal instance is configured with the required FCM API authentication token, and will then use the FCM API in order to notify the user's device running Element.

```
Homeserver ----> Sygnal (configured for Element) ----> FCM ----> Element
```

The push gateway is configured to only send  `(eventId,roomId)` in the push payload (for better [privacy](#push-vs-privacy-and-mitigation)).

Element needs then to synchronise with the user's homeserver, in order to resolve the event and create a notification.

As per [Google recommendation](https://android-developers.googleblog.com/2018/09/notifying-your-users-with-fcm.html), Element will then use the WorkManager API in order to trigger a background sync. 

**Google recommendations:** 
>  We recommend using FCM messages in combination with the WorkManager 1 or JobScheduler API

>  Avoid background services. One common pitfall is using a background service to fetch data in the FCM message handler, since background service will be stopped by the system per recent changes to Google Play Policy

```
Homeserver ----> Sygnal ----> FCM ----> Element
                                        (Sync) ----> Homeserver
                                               <----
                                        Display notification
```

**Possible outcomes**

Upon reception of the FCM push, Element will perform a sync call to the homeserver, during this process it is possible that:
  * Happy path, the sync is performed, the message resolved and displayed in the notification drawer
  * The notified message is not in the sync. Can happen if a lot of things did happen since the push (`gappy sync`)
  * The sync generates additional notifications (e.g an encrypted message where the user is mentioned detected locally)
  * The sync takes too long and the process is killed before completion, or network is not reliable and the sync fails.

Element implements several strategies in these cases (TODO document)

## FCM Fallback mode

It is possible that Element is not able to get a FCM push token.
Common errors (amoung several others) that can cause that:
* Google Play Services is outdated
* Google Play Service fails in someways with FCM servers (infamous `SERVICE_NOT_AVAILABLE`)

If Element is able to detect one of this cases, it will notifies it to the users and when possible help him fix it via a dedicated troubleshoot screen.

Meanwhile, in order to offer a minimal service, and as per Google's recommendation for background activities, Element will launch periodic background sync in order to stays in sync with servers.

The fallback mode is impacted by all the battery life saving mechanism implemented by android. Meaning that if the app is not used for a certain amount of time (`App-Standby`), or the device stays still and unplugged (`Light Doze`) , the sync will become less frequent.

And if the device stays unplugged and still for too long (`Doze Mode`), no background sync will be perform at all (the system's `Ignore Battery Optimization option` has no effect on that).

 Also the time interval between sync is elastic, controlled by the system to group other apps background sync request and start radio/cpu only once for all.

Usually in this mode, what happen is when you take back your phone in your hand, you suddenly receive notifications.

The fallback mode is supposed to be a temporary state waiting for the user to fix issues for FCM, or for App Developers that has done a fork to correctly configure their FCM settings.

## F-Droid background Mode

The F-Droid Element flavor has no dependencies to FCM, therefore cannot relies on Push.

Also Google's recommended background processing method cannot be applied. This is because all of these methods are affected by IDLE modes, and will result on the user not being notified at all when the app is in a Doze mode (only in maintenance windows that could happens only after hours).

Only solution left is to use `AlarmManager`, that offers new API to allow launching some process even if the App is in IDLE modes.

Notice that these alarms, due to their potential impact on battery life, can still be restricted by the system. Documentation says that they will not be triggered more than every minutes under normal system operation, and when in low power mode about every 15 mn.

These restrictions can be relaxed by requirering the app to be white listed from battery optimization.

F-Droid version will schedule alarms that will then trigger a Broadcast Receiver, that in turn will launch a Service (in the classic android way), and the reschedule an alarm for next time.

Depending on the system status (or device make),  it is still possible that the app is not given enough time to launch the service, or that the radio is still turned off thus preventing the sync to success (that's why Alarms are not recommended for network related tasks).

That is why on Element F-Droid, the broadcast receiver will acquire a temporary WAKE_LOCK for several seconds (thus securing cpu/network), and launch the service in foreground. The service performs the sync.

Note that foreground services require to put a notification informing the user that the app is doing something even if not launched).



# Application Settings

**Notifications > Enable notifications for this account**

Configure Sygnal to send or not notifications to all user devices.

**Notifications > Enable notifications for this device**

Disable notifications locally. The push server will continue to send notifications to the device but this one will ignore them.


