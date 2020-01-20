Changes in RiotX 0.14.0 (2020-XX-XX)
===================================================

Features ✨:
 - Enable encryption in unencrypted rooms, from the room settings (#212)

Improvements 🙌:
 -

Other changes:
 -

Bugfix 🐛:
 -

Translations 🗣:
 -

Build 🧱:
 -

Changes in RiotX 0.13.0 (2020-01-17)
===================================================

Features ✨:
 - Send and render typing events (#564)
 - Create Room Profile screen (#54)
 - Create Room Member Profile screen (#59)

Improvements 🙌:
 - Render events m.room.encryption and m.room.guest_access in the timeline

Bugfix 🐛:
 - Fix broken background sync in F-Droid version
 - Fix issue with downloaded file on encrypted rooms. The file was not properly decrypted

Build 🧱:
 - Change the way versionCode is computed (#827)

Changes in RiotX 0.12.0 (2020-01-09)
===================================================

Improvements 🙌:
 - The initial sync is now handled by a foreground service
 - Render aliases and canonical alias change in the timeline
 - Introduce developer mode in the settings (#745, #796)
 - Improve devices list screen
 - Add settings for rageshake sensibility
 - Fix autocompletion issues and add support for rooms, groups, and emoji (#780)
 - Show skip to bottom FAB while scrolling down (#752)
 - Enable encryption on a room, SDK part (#212)

Other changes:
 - Change the way RiotX identifies a session to allow the SDK to support several sessions with the same user (#800)
 - Exclude play-services-oss-licenses library from F-Droid build (#814)
 - Email domain can be limited on some homeservers, i18n of the displayed error (#754)

Bugfix 🐛:
 - Fix crash when opening room creation screen from the room filtering screen
 - Fix avatar image disappearing (#777)
 - Fix read marker banner when permalink
 - Fix joining upgraded rooms (#697)
 - Fix matrix.org room directory not being browsable (#807)
 - Hide non working settings (#751)

Changes in RiotX 0.11.0 (2019-12-19)
===================================================

Features ✨:
 - Implement soft logout (#281)

Improvements 🙌:
 - Handle navigation to room via room alias (#201)
 - Open matrix.to link in RiotX (#57)
 - Limit sticker size in the timeline

Other changes:
 - Use same default room colors than Riot-Web

Bugfix 🐛:
 - Scroll breadcrumbs to top when opened
 - Render default room name when it starts with an emoji (#477)
 - Do not display " (IRC)" in display names https://github.com/vector-im/riot-android/issues/444
 - Fix rendering issue with HTML formatted body
 - Disable click on Stickers (#703)

Build 🧱:
 - Include diff-match-patch sources as dependency

Changes in RiotX 0.10.0 (2019-12-10)
===================================================

Features ✨:
 - Breadcrumbs: switch from one room to another quickly (#571)

Improvements 🙌:
 - Support entering a RiotWeb client URL instead of the homeserver URL during connection (#744)

Other changes:
 - Add reason for all membership events (https://github.com/matrix-org/matrix-doc/pull/2367)

Bugfix 🐛:
 - When automardown is ON, pills are sent as MD in body (#739)
 - "ban" event are not rendered correctly (#716)
 - Fix crash when rotating screen in Room timeline

Changes in RiotX 0.9.1 (2019-12-05)
===================================================

Bugfix 🐛:
 - Fix an issue with DB transaction (#740)

Changes in RiotX 0.9.0 (2019-12-05)
===================================================

Features ✨:
 - Account creation. It's now possible to create account on any homeserver with RiotX (#34)
 - Iteration of the login flow (#613)

Improvements 🙌:
 - Send mention Pills from composer
 - Links in message preview in the bottom sheet are now active. 
 - Rework the read marker to make it more usable

Other changes:
 - Fix a small grammatical error when an empty room list is shown.

Bugfix 🐛:
 - Do not show long click help if only invitation are displayed
 - Fix emoji filtering not working
 - Fix issue of closing Realm in another thread (#725)
 - Attempt to properly cancel the crypto module when user signs out (#724)

Changes in RiotX 0.8.0 (2019-11-19)
===================================================

Features ✨:
 - Handle long click on room in the room list (#395)
 - Ignore/UnIgnore users, and display list of ignored users (#542, #617)

Improvements 🙌:
 - Search reaction by name or keyword in emoji picker
 - Handle code tags (#567)
 - Support spoiler messages
 - Support m.sticker and m.room.join_rules events in timeline

Other changes:
 - Markdown set to off by default (#412)
 - Accessibility improvements to the attachment file type chooser

Bugfix 🐛:
 - Fix issues with some member events rendering (#498)
 - Passphrase does not match (Export room keys) (#644)
 - Ask for permission to write external storage when uri comes from the keyboard (#658)
 - Fix issue with english US/GB translation (#671)

Changes in RiotX 0.7.0 (2019-10-24)
===================================================

Features:
 - Share elements from other app to RiotX (#58)
 - Read marker (#84)
 - Add ability to report content (#515)

Improvements:
 - Persist active tab between sessions (#503)
 - Do not upload file too big for the homeserver (#587)
 - Attachments: start using system pickers (#52)
 - Mark all messages as read (#396)


Other changes:
 - Accessibility improvements to read receipts in the room timeline and reactions emoji chooser

Bugfix:
 - Fix issue on upload error in loop (#587)
 - Fix opening a permalink: the targeted event is displayed twice (#556)
 - Fix opening a permalink paginates all the history up to the last event (#282)
 - after login, the icon in the top left is a green 'A' for (all communities) rather than my avatar (#267)
 - Picture uploads are unreliable, pictures are shown in wrong aspect ratio on desktop client (#517)
 - Invitation notifications are not dismissed automatically if room is joined from another client (#347)
 - Opening links from RiotX reuses browser tab (#599)

Changes in RiotX 0.6.1 (2019-09-24)
===================================================

Bugfix:
 - Fix crash: MergedHeaderItem was missing dimensionConverter

Changes in RiotX 0.6.0 (2019-09-24)
===================================================

Features:
 - Save draft of a message when exiting a room with non empty composer (#329)

Improvements:
 - Add unread indent on room list (#485)
 - Message Editing: Update notifications (#128)
 - Remove any notification of a redacted event (#563)

Other changes:
 - Fix a few accessibility issues

Bugfix:
 - Fix characters erased from the Search field when the result are coming (#545)
 - "No connection" banner was displayed by mistake
 - Leaving community (from another client) has no effect on RiotX (#497)
 - Push rules was not retrieved after a clear cache
 - m.notice messages trigger push notifications (#238)
 - Embiggen messages with multiple emojis also for edited messages (#458)

Build:
 - Fix (again) issue with bad versionCode generated by Buildkite (#553)

Changes in RiotX 0.5.0 (2019-09-17)
===================================================

Features:
 - Implementation of login to homeserver with SSO (#557)
 - Handle M_CONSENT_NOT_GIVEN error (#64)
 - Auto configure homeserver and identity server URLs of LoginActivity with a magic link

Improvements:
 - Reduce default release build log level, and lab option to enable more logs.
 - Display a no network indicator when there is no network (#559)

Bugfix:
 - Fix crash due to missing informationData (#535)
 - Progress in initial sync dialog is decreasing for a step and should not (#532)
 - Fix rendering issue of accepted third party invitation event
 - All current notifications were dismissed by mistake when the app is launched from the launcher

Build:
 - Fix issue with version name (#533)
 - Fix issue with bad versionCode generated by Buildkite (#553)

Changes in RiotX 0.4.0 (2019-08-30)
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


Changes in RiotX 0.0.0 (2020-XX-XX)
===================================================

Features ✨:
 -

Improvements 🙌:
 -

Other changes:
 -

Bugfix 🐛:
 -

Translations 🗣:
 -

Build 🧱:
 -

