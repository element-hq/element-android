Changes in RiotX 0.5.0 (2019-XX-XX)
===================================================

Features:
 - Save draft of a message when exiting a room with non empty composer (#329)

Improvements:
 - Reduce default release build log level, and lab option to enable more logs.

Other changes:
 -

Bugfix:
 - Fix crash due to missing informationData (#535)
 - Progress in initial sync dialog is decreasing for a step and should not (#532)

Translations:
 -

Build:
 - Fix issue with version name (#533)
 - Fix rendering issue of accepted third party invitation event

Changes in RiotX 0.4.0 (2019-XX-XX)
===================================================

Features:
 - Display read receipts in timeline (#81)

Improvements:
 - Reactions: Reinstate the ability to react with non-unicode keys (#307)

Bugfix:
 - Fix text diff linebreak display (#441)
 - Date change message repeats for each redaction until a normal message (#358)
 - Slide-in reply icon is distorted (#423)
 - Regression / e2e replies not encrypted
 - Some video won't play
 - Privacy: remove log of notifiable event (#519)
 - Fix crash with EmojiCompat (#530)

Changes in RiotX 0.3.0 (2019-08-08)
===================================================

Features:
 - Create Direct Room flow
 - Handle `/markdown` command

Improvements:
 - UI for pending edits (#193)
 - UX image preview screen transition (#393)
 - Basic support for resending failed messages (retry/remove)
 - Enable proper cancellation of suspending functions (including db transaction)
 - Enhances network connectivity checks in SDK
 - Add "View Edit History" item in the message bottom sheet (#401)
 - Cancel sync request on pause and timeout to 0 after pause (#404)

Other changes:
 - Show sync progress also in room detail screen (#403)

Bugfix:
 - Edited message: link confusion when (edited) appears in body (#398)
 - Close detail room screen when the room is left with another client (#256)
 - Clear notification for a room left on another client
 - Fix messages with empty `in_reply_to` not rendering (#447)
 - Fix clear cache (#408) and Logout (#205)
 - Fix `(edited)` link can be copied to clipboard (#402)

Build:
 - Split APK: generate one APK per arch, to reduce APK size of about 30%


Changes in RiotX 0.2.0 (2019-07-18)
===================================================

Features:
 - Message Editing: View edit history (#121)
 - Rooms filtering (#304)
 - Edit in encrypted room

Improvements:
 - Handle click on redacted events: view source and create permalink
 - Improve long tap menu: reply on top, more compact (#368)
 - Quick reply in timeline with swipe gesture (#167)
 - Improve edit of replies
 - Improve performance on Room Members and Users management (#381)

Other changes:
 - migrate from rxbinding 2 to rxbinding 3

Bugfix:
 - Fix regression on permalink click
 - Fix crash reported by the PlayStore (#341)
 - Fix Chat composer separator color in dark/black theme
 - Fix bad layout for room directory filter (#349)
 - Fix Copying link from a message shouldn't open context menu (#364)

Changes in RiotX 0.1.0 (2019-07-11)
===================================================

First release!

Mode details here: https://medium.com/@RiotChat/introducing-the-riotx-beta-for-android-b17952e8f771


=======================================================
+        TEMPLATE WHEN PREPARING A NEW RELEASE        +
=======================================================


Changes in RiotX 0.0.0 (2019-XX-XX)
===================================================

Features:
 -

Improvements:
 -

Other changes:
 -

Bugfix:
 -

Translations:
 -

Build:
 -

