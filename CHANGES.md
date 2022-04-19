Changes in Element v1.4.11 (2022-04-07)
=======================================

Bugfixes 🐛
----------
 - Choosing "leave all rooms and spaces" while leaving Space won't cause leaving DMs in this Space anymore ([#5609](https://github.com/vector-im/element-android/issues/5609))


Changes in Element v1.4.10 (2022-04-05)
=======================================

Features ✨
----------
 - Allow scrolling position of Voice Message playback ([#5426](https://github.com/vector-im/element-android/issues/5426))
 - Users will be able to provide feedback for threads ([#5647](https://github.com/vector-im/element-android/issues/5647))
 - Update Jitsi lib from 3.10.0 to 5.0.2 ([#5654](https://github.com/vector-im/element-android/issues/5654))

Bugfixes 🐛
----------
 - Replace "open settings" button by "disable" action in RageShake dialog if there is no session ([#4445](https://github.com/vector-im/element-android/issues/4445))
 - Fixes room summaries showing encrypted content after verifying device ([#4867](https://github.com/vector-im/element-android/issues/4867))
 - Fixes polls being votable after being ended ([#5473](https://github.com/vector-im/element-android/issues/5473))
 - [Subscribing] Blank display name ([#5497](https://github.com/vector-im/element-android/issues/5497))
 - Fixes voice call button disappearing in DM rooms with more than 2 members ([#5548](https://github.com/vector-im/element-android/issues/5548))
 - Add loader in thread list ([#5562](https://github.com/vector-im/element-android/issues/5562))
 - Fixed key export when overwriting existing files ([#5663](https://github.com/vector-im/element-android/issues/5663))

In development 🚧
----------------
 - Adding combined account creation and server selection screen as part of the new FTUE ([#5277](https://github.com/vector-im/element-android/issues/5277))
 - Finalising FTUE onboarding account creation personalization steps but keeping feature disabled until other parts are complete ([#5519](https://github.com/vector-im/element-android/issues/5519))
 - Live Location Sharing - Foreground Service and Notification ([#5595](https://github.com/vector-im/element-android/issues/5595))
 - Send beacon info state event when live location sharing started ([#5651](https://github.com/vector-im/element-android/issues/5651))
 - Show a banner in timeline while location sharing service is running ([#5660](https://github.com/vector-im/element-android/issues/5660))
 - Location sharing: adding possibility to choose duration of live sharing ([#5667](https://github.com/vector-im/element-android/issues/5667))

Other changes
-------------
 - Improve main timeline thread summary rendering ([#5151](https://github.com/vector-im/element-android/issues/5151))
 - "Add space" copy is replaced with "create space" in left sliding panel ([#5516](https://github.com/vector-im/element-android/issues/5516))
 - Flattening the asynchronous onboarding state and passing all errors through the same pipeline ([#5517](https://github.com/vector-im/element-android/issues/5517))
 - Changed items order in space menu. Changed capitalization for "space" in those items ([#5524](https://github.com/vector-im/element-android/issues/5524))
 - Permalinks to root thread messages will now navigate you within the thread timeline ([#5567](https://github.com/vector-im/element-android/issues/5567))
 - Live location sharing: adding way to override feature activation in debug ([#5581](https://github.com/vector-im/element-android/issues/5581))
 - Adds unit tests around the login with matrix id flow ([#5628](https://github.com/vector-im/element-android/issues/5628))
 - Setup the plugin org.owasp.dependencycheck ([#5654](https://github.com/vector-im/element-android/issues/5654))
 - Implement threads beta opt-in mechanism to notify users about threads ([#5692](https://github.com/vector-im/element-android/issues/5692))


Changes in Element v1.4.8 (2022-03-28)
======================================

Other changes
-------------
 - Moving live location sharing permission to debug only builds whilst it is WIP ([#5636](https://github.com/vector-im/element-android/issues/5636))


Changes in Element v1.4.7 (2022-03-24)
======================================

Bugfixes 🐛
----------
 - Fix inconsistencies between the arrow visibility and the collapse action on the room sections ([#5616](https://github.com/vector-im/element-android/issues/5616))
 - Fix room list header count flickering

Changes in Element v1.4.6 (2022-03-23)
======================================

Features ✨
----------
 - Thread timeline is now live and much faster especially for large or old threads ([#5230](https://github.com/vector-im/element-android/issues/5230))
 - View all threads per room screen is now live when the home server supports threads ([#5232](https://github.com/vector-im/element-android/issues/5232))
 - Add a custom view to display a picker for share location options ([#5395](https://github.com/vector-im/element-android/issues/5395))
 - Add ability to pin a location on map for sharing ([#5417](https://github.com/vector-im/element-android/issues/5417))
 - Poll Integration Tests ([#5522](https://github.com/vector-im/element-android/issues/5522))
 - Live location sharing: adding build config field and show permission dialog ([#5536](https://github.com/vector-im/element-android/issues/5536))
 - Live location sharing: Adding indicator view when enabled ([#5571](https://github.com/vector-im/element-android/issues/5571))

Bugfixes 🐛
----------
 - Poll system notifications on Android are not user friendly ([#4780](https://github.com/vector-im/element-android/issues/4780))
 - Add colors for shield vector drawable ([#4860](https://github.com/vector-im/element-android/issues/4860))
 - Support both stable and unstable prefixes for Events about Polls and Location ([#5340](https://github.com/vector-im/element-android/issues/5340))
 - Fix missing messages when loading messages forwards ([#5448](https://github.com/vector-im/element-android/issues/5448))
 - Fix presence indicator being aligned to the center of the room image ([#5489](https://github.com/vector-im/element-android/issues/5489))
 - Read receipt in wrong order ([#5514](https://github.com/vector-im/element-android/issues/5514))
 - Fix mentions using matrix.to rather than client defined permalink base url ([#5521](https://github.com/vector-im/element-android/issues/5521))
 - Fixes crash when tapping the timeline verification surround box instead of the buttons ([#5540](https://github.com/vector-im/element-android/issues/5540))
 - [Notification mode] Wrong mode is displayed when the mention only is selected on the web client ([#5547](https://github.com/vector-im/element-android/issues/5547))
 - Fix local echos not being shown when re-opening rooms ([#5551](https://github.com/vector-im/element-android/issues/5551))
 - Fix crash when closing a room while decrypting timeline events ([#5552](https://github.com/vector-im/element-android/issues/5552))
 - Fix sometimes read marker not properly updating ([#5564](https://github.com/vector-im/element-android/issues/5564))

In development 🚧
----------------
 - Dynamically showing/hiding onboarding personalisation screens based on the users homeserver capabilities ([#5375](https://github.com/vector-im/element-android/issues/5375))
 - Introduces FTUE personalisation complete screen along with confetti celebration ([#5389](https://github.com/vector-im/element-android/issues/5389))

SDK API changes ⚠️
------------------
 - Adds support for MSC3440, additional threads homeserver capabilities ([#5271](https://github.com/vector-im/element-android/issues/5271))

Other changes
-------------
 - Refactoring for safer olm and megolm session usage ([#5380](https://github.com/vector-im/element-android/issues/5380))
 - Improve headers UI in Rooms/Messages lists ([#4533](https://github.com/vector-im/element-android/issues/4533))
 - Number of unread messages on space badge now include number of unread DMs ([#5260](https://github.com/vector-im/element-android/issues/5260))
 - Amend spaces menu to be consistent with iOS version ([#5270](https://github.com/vector-im/element-android/issues/5270))
 - Selected space highlight changed in left panel ([#5346](https://github.com/vector-im/element-android/issues/5346))
 - [Rooms list] Do not suggest collapse the unique section ([#5347](https://github.com/vector-im/element-android/issues/5347))
 - Add analytics support for threads ([#5378](https://github.com/vector-im/element-android/issues/5378))
 - Add top margin before our first message ([#5384](https://github.com/vector-im/element-android/issues/5384))
 - Improved onboarding registration unit test coverage ([#5408](https://github.com/vector-im/element-android/issues/5408))
 - Adds stable room hierarchy endpoint with a fallback to the unstable one ([#5443](https://github.com/vector-im/element-android/issues/5443))
 - Use ColorPrimary for attachmentGalleryButton tint ([#5501](https://github.com/vector-im/element-android/issues/5501))
 - Added online presence indicator attribute online to match offline styling ([#5513](https://github.com/vector-im/element-android/issues/5513))
 - Add a presence sync enabling build config ([#5563](https://github.com/vector-im/element-android/issues/5563))
 - Show stickers on click ([#5572](https://github.com/vector-im/element-android/issues/5572))


Changes in Element v1.4.4 (2022-03-09)
======================================

Features ✨
----------
 - Adds animated typing indicator to the bottom of the timeline ([#3296](https://github.com/vector-im/element-android/issues/3296))
 - Removes the topic and typing information from the room's top bar ([#4642](https://github.com/vector-im/element-android/issues/4642))
 - Add possibility to save media from Gallery + reorder choices in message context menu ([#5005](https://github.com/vector-im/element-android/issues/5005))
 - Improves settings error dialog messaging when changing avatar or display name fails ([#5418](https://github.com/vector-im/element-android/issues/5418))
 
Bugfixes 🐛
----------
 - Open direct message screen when clicking on DM button in the space members list ([#4319](https://github.com/vector-im/element-android/issues/4319))
 - Fix incorrect media cache size in settings ([#5394](https://github.com/vector-im/element-android/issues/5394))
 - Setting an avatar when creating a room had no effect ([#5402](https://github.com/vector-im/element-android/issues/5402))
 - Fix reactions summary crash when reopening a room ([#5463](https://github.com/vector-im/element-android/issues/5463))
 - Fixing room titles overlapping the room image in the room toolbar ([#5468](https://github.com/vector-im/element-android/issues/5468))

In development 🚧
----------------
 - Starts the FTUE account personalisation flow by adding an account created screen behind a feature flag ([#5158](https://github.com/vector-im/element-android/issues/5158))

SDK API changes ⚠️
------------------
 - Change name of getTimeLineEvent and getTimeLineEventLive methods to getTimelineEvent and getTimelineEventLive. ([#5330](https://github.com/vector-im/element-android/issues/5330))

Other changes
-------------
 - Improve Bubble layouts rendering ([#5303](https://github.com/vector-im/element-android/issues/5303))
 - Continue improving realm usage (potentially helping with storage and RAM usage) ([#5330](https://github.com/vector-im/element-android/issues/5330))
 - Update reaction button layout. ([#5313](https://github.com/vector-im/element-android/issues/5313))
 - Adds forceLoginFallback feature flag and usages to FTUE login and registration ([#5325](https://github.com/vector-im/element-android/issues/5325))
 - Override task affinity to prevent unknown activities running in our app tasks. ([#4498](https://github.com/vector-im/element-android/issues/4498))
 - Tentatively fixing the UI sanity test being unable to click on the space menu items ([#5269](https://github.com/vector-im/element-android/issues/5269))
 - Moves attachment-viewer, diff-match-patch, and multipicker modules to subfolders under library ([#5309](https://github.com/vector-im/element-android/issues/5309))
 - Log the `since` token used and `next_batch` token returned when doing an incremental sync. ([#5312](https://github.com/vector-im/element-android/issues/5312), [#5318](https://github.com/vector-im/element-android/issues/5318))
 - Upgrades material dependency version from 1.4.0 to 1.5.0 ([#5392](https://github.com/vector-im/element-android/issues/5392))
 - Using app name instead of hardcoded "Element" for exported keys filename ([#5326](https://github.com/vector-im/element-android/issues/5326))
 - Upgrade the plugin which generate strings with template from 1.2.2 to 2.0.0 ([#5348](https://github.com/vector-im/element-android/issues/5348))
 - Remove about 700 unused strings and their translations ([#5352](https://github.com/vector-im/element-android/issues/5352))
 - Creates dedicated VectorOverrides for forcing behaviour for local testing/development ([#5361](https://github.com/vector-im/element-android/issues/5361))
 - Cleanup unused threads build configurations ([#5379](https://github.com/vector-im/element-android/issues/5379))
 - Notify element-android channel each time a nightly build completes. ([#5314](https://github.com/vector-im/element-android/issues/5314))
 - Iterate on badge / unread indicator color ([#5456](https://github.com/vector-im/element-android/issues/5456))


Changes in Element v1.4.2 (2022-02-22 Palindrome Day!)
======================================================

Features ✨
----------
 - Open the room when user accepts an invite from the room list ([#3771](https://github.com/vector-im/element-android/issues/3771))
 - Add completion for @room to notify everyone in a room ([#5123](https://github.com/vector-im/element-android/issues/5123))
 - Improve UI of reactions in timeline, including quick add reaction. ([#5204](https://github.com/vector-im/element-android/issues/5204))
 - Support creating disclosed polls ([#5290](https://github.com/vector-im/element-android/issues/5290))

Bugfixes 🐛
----------
 - Remove redundant highlight on add poll option button ([#5178](https://github.com/vector-im/element-android/issues/5178))
 - Reliably display crash report prompt ([#5195](https://github.com/vector-im/element-android/issues/5195))
 - Fix for rooms with virtual rooms not showing call status events in the timeline. ([#5198](https://github.com/vector-im/element-android/issues/5198))
 - Fix for call transfer with consult failing to make outgoing consultation call. ([#5201](https://github.com/vector-im/element-android/issues/5201))
 - Fix crash during account registration when redirecting to Web View ([#5218](https://github.com/vector-im/element-android/issues/5218))
 - Analytics: Fixes missing use case identity values from within the onboarding flow ([#5234](https://github.com/vector-im/element-android/issues/5234))
 - Fixing crash when adding room by QR code after accepting the camera permission for the first time ([#5295](https://github.com/vector-im/element-android/issues/5295))

SDK API changes ⚠️
------------------
 - `join` and `leave` methods moved from MembershipService to RoomService and SpaceService to split logic for rooms and spaces ([#5183](https://github.com/vector-im/element-android/issues/5183))
 - Deprecates Matrix.initialize and Matrix.getInstance in favour of the client providing its own singleton instance via Matrix.createInstance ([#5185](https://github.com/vector-im/element-android/issues/5185))
 - Adds support for MSC3283, additional homeserver capabilities ([#5207](https://github.com/vector-im/element-android/issues/5207))

Other changes
-------------
 - Right align the notifications badge in the rooms list (and DMs) so that it's always in a consistent place on the screen. ([#4640](https://github.com/vector-im/element-android/issues/4640))
 - Collapse successive ACLs events in room timeline ([#2782](https://github.com/vector-im/element-android/issues/2782))
 - Home screen: Replacing search icon by filter icon in the top right menu ([#4643](https://github.com/vector-im/element-android/issues/4643))
 - Make Space creation screens more consistent ([#5104](https://github.com/vector-im/element-android/issues/5104))
 - Defensive coding to ensure encryption when room was once e2e ([#5136](https://github.com/vector-im/element-android/issues/5136))
 - Reduce verbosity of debug logging, ([#5209](https://github.com/vector-im/element-android/issues/5209))
 - Standardise emulator versions of GHA integration tests. ([#5210](https://github.com/vector-im/element-android/issues/5210))
 - Replacing color "vctr_unread_room_badge" by "vctr_content_secondary" ([#5225](https://github.com/vector-im/element-android/issues/5225))
 - Change preferred jitsi domain from `jitsi.riot.im` to `meet.element.io` ([#5254](https://github.com/vector-im/element-android/issues/5254))
 - Analytics screen events are now tracked on screen enter instead of screen leave ([#5256](https://github.com/vector-im/element-android/issues/5256))
 - Improves bitmap memory usage by caching the shortcut images ([#5276](https://github.com/vector-im/element-android/issues/5276))
 - Changes unread marker in room list from green to grey ([#5294](https://github.com/vector-im/element-android/issues/5294))
 - Improve some internal realm usages. ([#5297](https://github.com/vector-im/element-android/issues/5297))

Translations 🗣
--------------
 - Improved Japanese translations (special thanks to Suguru Hirahara!)


Changes in Element v1.4.0 (2022-02-09)
======================================

Features ✨
----------
 - Initial implementation of thread messages ([#4746](https://github.com/vector-im/element-android/issues/4746))
 - Support message bubbles in timeline. ([#4937](https://github.com/vector-im/element-android/issues/4937))
 - Support generic location pin ([#5146](https://github.com/vector-im/element-android/issues/5146))
 - Retrieve map style url from .well-known ([#5175](https://github.com/vector-im/element-android/issues/5175))

Bugfixes 🐛
----------
 - Fixes non sans-serif font weights being ignored ([#3907](https://github.com/vector-im/element-android/issues/3907))
 - Fixing missing/intermittent notifications on the google play variant when wifi is enabled ([#5038](https://github.com/vector-im/element-android/issues/5038))
 - Fixes call statuses in the timeline for missed/rejected calls and connected calls. ([#5088](https://github.com/vector-im/element-android/issues/5088))
 - Fix fallback permalink when threads are disabled ([#5128](https://github.com/vector-im/element-android/issues/5128))
 - Analytics: aligns use case identifying with iOS implementation ([#5142](https://github.com/vector-im/element-android/issues/5142))
 - Fix location rendering in timeline if map cannot be loaded ([#5143](https://github.com/vector-im/element-android/issues/5143))

Other changes
-------------
 - "Invite users to space" dialog now closed when user choose invite method ([#4295](https://github.com/vector-im/element-android/issues/4295))
 - Changed layout for space card and room card used at "explore room" screen and space/room invite dialogs ([#4304](https://github.com/vector-im/element-android/issues/4304))
 - Removed spaces restricted search hint dialogs ([#4315](https://github.com/vector-im/element-android/issues/4315))
 - Remove Search from room options if not available ([#4641](https://github.com/vector-im/element-android/issues/4641))
 - Qr code scanning fragments merged into one ([#4873](https://github.com/vector-im/element-android/issues/4873))
 - Fix CI/CD errors after merges for quality and integration tests ([#5118](https://github.com/vector-im/element-android/issues/5118))
 - Added automation for the Z-FTUE label to add issues to the FTUE Project Board ([#5120](https://github.com/vector-im/element-android/issues/5120))
 - Added automation for WTF labels to move to WTF project board ([#5148](https://github.com/vector-im/element-android/issues/5148))
 - Update WTF automation to fix it ([#5173](https://github.com/vector-im/element-android/issues/5173))


Changes in Element v1.3.18 (2022-02-03)
=======================================

Bugfixes 🐛
----------
 - Avoid deleting root event of CurrentState on gappy sync. In order to restore lost Events an initial sync may be triggered. ([#5137](https://github.com/vector-im/element-android/issues/5137))


Changes in Element v1.3.17 (2022-01-31)
=======================================

Bugfixes 🐛
----------
 - Display static map images in the timeline and improve Location sharing feature ([#5084](https://github.com/vector-im/element-android/issues/5084))
 - Show the legal mention of mapbox when sharing location ([#5062](https://github.com/vector-im/element-android/issues/5062))
 - Poll cannot end in some unencrypted rooms ([#5067](https://github.com/vector-im/element-android/issues/5067))
 - Selecting Transfer in a call should immediately put the other person on hold until the call connects or the Transfer is cancelled. ([#5081](https://github.com/vector-im/element-android/issues/5081))
 - Fixing crashes when quickly scrolling or restoring the room timeline ([#5091](https://github.com/vector-im/element-android/issues/5091))


Changes in Element 1.3.16 (2022-01-25)
======================================

Features ✨
----------
 - Static location sharing and rendering ([#2210](https://github.com/vector-im/element-android/issues/2210))
 - Enables the FTUE splash carousel ([#4584](https://github.com/vector-im/element-android/issues/4584))
 - Allow editing polls ([#5036](https://github.com/vector-im/element-android/issues/5036))

Bugfixes 🐛
----------
 - Fixing missing notifications in FDroid variants using `optimised for battery` background sync mode ([#5003](https://github.com/vector-im/element-android/issues/5003))
 - Fix for stuck local event messages at the bottom of the screen ([#516](https://github.com/vector-im/element-android/issues/516))
 - Notification does not take me to the room when another space was last viewed ([#3839](https://github.com/vector-im/element-android/issues/3839))
 - Explore Rooms overflow menu - content update include "Create room" ([#3932](https://github.com/vector-im/element-android/issues/3932))
 - Fix sync timeout after returning from background ([#4669](https://github.com/vector-im/element-android/issues/4669))
 - Fix a wrong network error issue in the Legals screen ([#4935](https://github.com/vector-im/element-android/issues/4935))
 - Prevent Alerts to be displayed in the automatically displayed analytics opt-in screen ([#4948](https://github.com/vector-im/element-android/issues/4948))
 - EmojiPopupDismissListener not being triggered after dismissing the EmojiPopup ([#4991](https://github.com/vector-im/element-android/issues/4991))
 - Fix an error in string resource ([#4997](https://github.com/vector-im/element-android/issues/4997))
 - Big messages taking inappropriately long to evaluate .m.rule.roomnotif push rules ([#5008](https://github.com/vector-im/element-android/issues/5008))
 - Improve auto rageshake lab feature ([#5021](https://github.com/vector-im/element-android/issues/5021))

In development 🚧
----------------
 - Updates the onboarding carousel images, copy and improves the handling of different device sizes ([#4880](https://github.com/vector-im/element-android/issues/4880))
 - Disabling onboarding automatic carousel transitions on user interaction ([#4914](https://github.com/vector-im/element-android/issues/4914))
 - Locking phones to portrait during the FTUE onboarding ([#4918](https://github.com/vector-im/element-android/issues/4918))
 - Adds a messaging use case screen to the FTUE onboarding ([#4927](https://github.com/vector-im/element-android/issues/4927))
 - Updating the FTUE use case icons ([#5025](https://github.com/vector-im/element-android/issues/5025))
 - Support undisclosed polls ([#5037](https://github.com/vector-im/element-android/issues/5037))

Other changes
-------------
 - Enabling native support for window resizing ([#4811](https://github.com/vector-im/element-android/issues/4811))
 - Analytics: send more Events ([#4734](https://github.com/vector-im/element-android/issues/4734))
 - Fix integration tests and add a comment with results (still not perfect due to github actions resource limitations) ([#4842](https://github.com/vector-im/element-android/issues/4842))
 - "/kick" command is replaced with "/remove". Also replaced all occurrences in string resources ([#4865](https://github.com/vector-im/element-android/issues/4865))
 - Toolbar management rework. Toolbar title's and subtitle's text appearance now controlled by theme without local overrides. Helper class introduced to
  help with toolbar configuration. Toolbar title, subtitle and navigation button widgets are removed where it is possible and replaced with built-in
  toolbar widgets. ([#4884](https://github.com/vector-im/element-android/issues/4884))
 - Add signing config for the release buildType. No secret added ([#4926](https://github.com/vector-im/element-android/issues/4926))
 - Remove unused module matrix-sdk-android-rx and do some cleanup ([#4942](https://github.com/vector-im/element-android/issues/4942))
 - Sync issue automation with element-web ([#4949](https://github.com/vector-im/element-android/issues/4949))
 - Improves local echo blinking when non room events received ([#4960](https://github.com/vector-im/element-android/issues/4960))
 - Including onboarding server options in the all screen sanity test suite ([#4975](https://github.com/vector-im/element-android/issues/4975))
 - Exclude dependabot upgrade for @github-script@v3 ([#4988](https://github.com/vector-im/element-android/issues/4988))
 - Small iteration on command parser and unit test it. ([#4998](https://github.com/vector-im/element-android/issues/4998))

SDK API changes ⚠️
------------------
 - `StateService.sendStateEvent()` now takes a non-nullable String for the parameter `stateKey`. If null was used, just now use an empty string. ([#4895](https://github.com/vector-im/element-android/issues/4895))
 - 429 are not automatically retried anymore in case of too long retry delay ([#4995](https://github.com/vector-im/element-android/issues/4995))


Changes in Element v1.3.15 (2022-01-18)
=======================================

Bugfixes 🐛
----------
 - Fix crash when viewing source which contains an emoji ([#4796](https://github.com/vector-im/element-android/issues/4796))
 - Prevent crash in Timeline and add more logs. ([#4959](https://github.com/vector-im/element-android/issues/4959))
 - Fix crash on API <24 and make sure this error will not occur again. ([#4962](https://github.com/vector-im/element-android/issues/4962))
 - Fixes sign in/up crash when selecting ems and other server types which use SSO ([#4969](https://github.com/vector-im/element-android/issues/4969))


Changes in Element v1.3.14 (2022-01-12)
=======================================

Bugfixes 🐛
----------
 - Fix sending events in encrypted rooms broken, and incremental sync broken in 1.3.13 ([#4924](https://github.com/vector-im/element-android/issues/4924))


Changes in Element v1.3.13 (2022-01-11)
=======================================

Features ✨
----------
 - Updates onboarding splash screen to have a dedicated sign in button and removes the dual purpose sign in/up stage ([#4382](https://github.com/vector-im/element-android/issues/4382))
 - Display Analytics opt-in screen at first start-up of the app ([#4892](https://github.com/vector-im/element-android/issues/4892))
 - New attachment picker UI ([#3444](https://github.com/vector-im/element-android/issues/3444))
 - Add labs support for rendering LaTeX maths (MSC2191) ([#2133](https://github.com/vector-im/element-android/issues/2133))
 - Allow changing nick colors from the member detail screen ([#2614](https://github.com/vector-im/element-android/issues/2614))
 - Analytics: Track Errors ([#4719](https://github.com/vector-im/element-android/issues/4719))
 - Change internal timeline management. ([#4405](https://github.com/vector-im/element-android/issues/4405))
 - Translate the error observed when the user is not allowed to join a room ([#4847](https://github.com/vector-im/element-android/issues/4847))

Bugfixes 🐛
----------
 - Stop using CharSequence as EpoxyAttribute because it can lead to crash if the CharSequence mutates during rendering. ([#4837](https://github.com/vector-im/element-android/issues/4837))
 - Better handling of misconfigured room encryption ([#4711](https://github.com/vector-im/element-android/issues/4711))
 - Fix message replies/quotes to respect newlines. ([#4540](https://github.com/vector-im/element-android/issues/4540))
 - Polls: unable to create a poll with more than 10 answers ([#4735](https://github.com/vector-im/element-android/issues/4735))
 - Fix for broken unread message indicator on the room list when there are no messages in the room. ([#4749](https://github.com/vector-im/element-android/issues/4749))
 - Fixes newer emojis rendering strangely when inserting from the system keyboard ([#4756](https://github.com/vector-im/element-android/issues/4756))
 - Fixing unable to change change avatar in some scenarios ([#4767](https://github.com/vector-im/element-android/issues/4767))
 - Tentative fix for the speaker being used instead of earpiece for the outgoing call ringtone on lineage os ([#4781](https://github.com/vector-im/element-android/issues/4781))
 - Fixing crashes when quickly scrolling or restoring the room timeline ([#4789](https://github.com/vector-im/element-android/issues/4789))
 - Fixing encrypted non message events showing up as notification messages (eg when a participant joins, mutes or leaves a voice call) ([#4804](https://github.com/vector-im/element-android/issues/4804))

SDK API changes ⚠️
------------------
 - Introduce method onStateUpdated on Timeline.Callback ([#4405](https://github.com/vector-im/element-android/issues/4405))
 - Support tagged events in Room Account Data (MSC2437) ([#4753](https://github.com/vector-im/element-android/issues/4753))

Other changes
-------------
 - Workaround to fetch all the pending toDevice events from a Synapse homeserver ([#4612](https://github.com/vector-im/element-android/issues/4612))
 - Toolbar is added to a views with QR code scan ([#4644](https://github.com/vector-im/element-android/issues/4644))
 - Open share UI provides by the system when sharing media or text. ([#4745](https://github.com/vector-im/element-android/issues/4745))
 - Cleaning rendering of state events in timeline ([#4747](https://github.com/vector-im/element-android/issues/4747))
 - Enabling new FTUE Auth onboarding base, includes the "I already have an account" button in the splash ([#4872](https://github.com/vector-im/element-android/issues/4872))
 - Olm lib is now hosted in MavenCentral - upgrade to 3.2.10 ([#4882](https://github.com/vector-im/element-android/issues/4882))
 - Remove deprecated experimental restricted space lab option ([#4889](https://github.com/vector-im/element-android/issues/4889))
 - Add ktlint results on github as a comment only on fail ([#4888](https://github.com/vector-im/element-android/issues/4888))
 - Fix github actions ktlint reports and publish results on PR as comment ([#4864](https://github.com/vector-im/element-android/issues/4864))


Changes in Element v1.3.12 (2021-12-20)
=======================================

Bugfixes 🐛
----------
 - Fixing emoji related crashes on android 8.1.1 and below ([#4769](https://github.com/vector-im/element-android/issues/4769))


Changes in Element v1.3.11 (2021-12-17)
=======================================

Bugfixes 🐛
----------
 - Fixing proximity sensor still being active after a call ([#2467](https://github.com/vector-im/element-android/issues/2467))
 - Fix name and shield are truncated in the room detail screen ([#4700](https://github.com/vector-im/element-android/issues/4700))
 - Call banner: center text vertically ([#4710](https://github.com/vector-im/element-android/issues/4710))
 - Fixes unable to render messages by allowing them to render whilst the emoji library is initialising ([#4733](https://github.com/vector-im/element-android/issues/4733))
 - Fix app crash uppon long press on a reply event ([#4742](https://github.com/vector-im/element-android/issues/4742))
 - Fixes crash when launching rooms which contain emojis in the emote content on android 12+ ([#4743](https://github.com/vector-im/element-android/issues/4743))

Other changes
-------------
 - Avoids leaking the activity windows when loading dialogs are displaying ([#4713](https://github.com/vector-im/element-android/issues/4713))


Changes in Element v1.3.10 (2021-12-14)
=======================================

Features ✨
----------
 - Poll Feature - Render in timeline ([#4653](https://github.com/vector-im/element-android/issues/4653))
 - Updates URL previews to match latest designs ([#4278](https://github.com/vector-im/element-android/issues/4278))
 - Setup Analytics framework using PostHog. Analytics are disabled by default. Opt-in screen not automatically displayed yet. ([#4559](https://github.com/vector-im/element-android/issues/4559))
 - Create a legal screen in the setting to group all the different policies. ([#4660](https://github.com/vector-im/element-android/issues/4660))
 - Add a help section in the settings. ([#4638](https://github.com/vector-im/element-android/issues/4638))
 - MSC2732: Olm fallback keys ([#3473](https://github.com/vector-im/element-android/issues/3473))

Bugfixes 🐛
----------
 - Fixes message menu showing when copying message urls ([#4324](https://github.com/vector-im/element-android/issues/4324))
 - Fix lots of integration tests by introducing TestMatrix class and MatrixWorkerFactory. ([#4546](https://github.com/vector-im/element-android/issues/4546))
 - Fix empty Dev Tools screen issue. ([#4592](https://github.com/vector-im/element-android/issues/4592))
 - Fix for outgoing voip call via sip bridge failing after 1 minute. ([#4621](https://github.com/vector-im/element-android/issues/4621))
 - Update log warning for call selection during voip calls. ([#4636](https://github.com/vector-im/element-android/issues/4636))
 - Fix possible crash when having identical subspaces in multiple root spaces ([#4693](https://github.com/vector-im/element-android/issues/4693))
 - Fix a crash in the timeline with some Emojis. Also migrate to androidx.emoji2 ([#4698](https://github.com/vector-im/element-android/issues/4698))
 - At the very first room search after opening the app sometimes no results are displayed ([#4600](https://github.com/vector-im/element-android/issues/4600))

Other changes
-------------
 - Upgrade OLM to v3.2.7 and get it from our maven repository. ([#4647](https://github.com/vector-im/element-android/issues/4647))
 - Add explicit dependency location, regarding the several maven repository. Also update some libraries (flexbox and alerter), and do some cleanup. ([#4670](https://github.com/vector-im/element-android/issues/4670))
 - Introducing feature flagging to the login and notification settings flows ([#4626](https://github.com/vector-im/element-android/issues/4626))
 - There is no need to call job.cancel() when we are using viewModelScope() ([#4602](https://github.com/vector-im/element-android/issues/4602))
 - Debounce some clicks ([#4645](https://github.com/vector-im/element-android/issues/4645))
 - Improve issue automation workflows ([#4617](https://github.com/vector-im/element-android/issues/4617))
 - Add automation to move message bubbles issues to message bubbles board. ([#4666](https://github.com/vector-im/element-android/issues/4666))
 - Fix graphql warning in issue workflow automation ([#4671](https://github.com/vector-im/element-android/issues/4671))
 - Cleanup the layout files ([#4604](https://github.com/vector-im/element-android/issues/4604))
 - Cleanup id ref. Use type views instead ([#4650](https://github.com/vector-im/element-android/issues/4650))


Changes in Element v1.3.9 (2021-12-01)
======================================

Features ✨
----------
 - Voice messages: Persist drafts of voice messages when navigating between rooms ([#3922](https://github.com/vector-im/element-android/issues/3922))
 - Make Element Android Thread aware ([#4246](https://github.com/vector-im/element-android/issues/4246))
 - Iterate on the consent dialog of the identity server. ([#4577](https://github.com/vector-im/element-android/issues/4577))

Bugfixes 🐛
----------
 - Fixes left over text when inserting emojis via the ':' menu and replaces the last typed ':' rather than the one at the end of the message ([#3449](https://github.com/vector-im/element-android/issues/3449))
 - Fixing queued voice message failing to send or retry ([#3833](https://github.com/vector-im/element-android/issues/3833))
 - Keeping device screen on whilst recording and playing back voice messages ([#4022](https://github.com/vector-im/element-android/issues/4022))
 - Allow voice messages to continue recording during device rotation ([#4067](https://github.com/vector-im/element-android/issues/4067))
 - Allowing users to hang up VOIP calls during the initialisation phase (avoids getting stuck in the call screen if something goes wrong) ([#4144](https://github.com/vector-im/element-android/issues/4144))
 - Make the verification shields the same in Element Web and Element Android ([#4338](https://github.com/vector-im/element-android/issues/4338))
 - Fix a display issue in the composer when the replied message is changed. ([#4343](https://github.com/vector-im/element-android/issues/4343))
 - Dismissing the Fdroid variant Listening for notifications on sign out, fixes crash when tapping the notification when signed out ([#4488](https://github.com/vector-im/element-android/issues/4488))
 - Fix a crash when displaying the bootstrap bottom sheet ([#4520](https://github.com/vector-im/element-android/issues/4520))
 - Remove duplicated settings declaration ([#4539](https://github.com/vector-im/element-android/issues/4539))
 - Fixes .ogg files failing to upload to rooms ([#4552](https://github.com/vector-im/element-android/issues/4552))
 - Add robustness when getting data from cursors ([#4605](https://github.com/vector-im/element-android/issues/4605))

Other changes
-------------
 - Upgrade Jitsi lib (and so webrtc) from Jitsi android-sdk-3.1.0 to android-sdk-3.10.0 ([#4504](https://github.com/vector-im/element-android/issues/4504))
 - Improve crypto logs to help debug decryption failures ([#4507](https://github.com/vector-im/element-android/issues/4507))
 - Voice recording mic button refactor with small animation tweaks in preparation for voice drafts ([#4515](https://github.com/vector-im/element-android/issues/4515))
 - Remove requestModelBuild() from epoxy Controllers init{} block ([#4591](https://github.com/vector-im/element-android/issues/4591))


Changes in Element v1.3.8 (2021-11-17)
======================================

Features ✨
----------
 - Android 12 support ([#4433](https://github.com/vector-im/element-android/issues/4433))
 - Make notification text spoiler aware ([#3477](https://github.com/vector-im/element-android/issues/3477))
 - Poll Feature - Create Poll Screen (Disabled for now) ([#4367](https://github.com/vector-im/element-android/issues/4367))
 - Adds support for images inside message notifications ([#4402](https://github.com/vector-im/element-android/issues/4402))

Bugfixes 🐛
----------
 - Render markdown in room list ([#452](https://github.com/vector-im/element-android/issues/452))
 - Fix incorrect cropping of conversation icons ([#4424](https://github.com/vector-im/element-android/issues/4424))
 - Fix potential NullPointerException crashes in Room and User account data sources ([#4428](https://github.com/vector-im/element-android/issues/4428))
 - Unable to establish Olm outbound session from fallback key ([#4446](https://github.com/vector-im/element-android/issues/4446))
 - Fixes intermittent crash on sign out due to the session being incorrectly recreated whilst being closed ([#4480](https://github.com/vector-im/element-android/issues/4480))

SDK API changes ⚠️
------------------
 - Add content scanner API from MSC1453
  API documentation : https://github.com/matrix-org/matrix-content-scanner#api ([#4392](https://github.com/vector-im/element-android/issues/4392))
 - Breaking SDK API change to PushRuleListener, the separated callbacks have been merged into one with a data class which includes all the previously separated push information ([#4401](https://github.com/vector-im/element-android/issues/4401))

Other changes
-------------
 - Finish migration from RxJava to Flow ([#4219](https://github.com/vector-im/element-android/issues/4219))
 - Remove redundant text in feature request issue form ([#4257](https://github.com/vector-im/element-android/issues/4257))
 - Add and improve issue triage workflows ([#4435](https://github.com/vector-im/element-android/issues/4435))
 - Update issue template to bring in line with element-web ([#4452](https://github.com/vector-im/element-android/issues/4452))


Changes in Element v1.3.7 (2021-11-04)
======================================

Features ✨
----------
 - Adding the room name to the invitation notification (if the room summary is available) ([#582](https://github.com/vector-im/element-android/issues/582))
 - Updating single sign on providers ordering to match priority/popularity ([#4277](https://github.com/vector-im/element-android/issues/4277))

Bugfixes 🐛
----------
 - Stops showing a dedicated redacted event notification, the message notifications will update accordingly ([#1491](https://github.com/vector-im/element-android/issues/1491))
 - Fixes marking individual notifications as read causing other notifications to be dismissed ([#3395](https://github.com/vector-im/element-android/issues/3395))
 - Fixing missing send button in light mode dev tools - send * event ([#3674](https://github.com/vector-im/element-android/issues/3674))
 - Fixing room search needing exact casing for non latin-1 character named rooms ([#3968](https://github.com/vector-im/element-android/issues/3968))
 - Fixing call ringtones only playing once when the ringtone doesn't contain looping metadata (android 9.0 and above) ([#4047](https://github.com/vector-im/element-android/issues/4047))
 - Tentatively fixing the doubled notifications by updating the group summary at specific points in the notification rendering cycle ([#4152](https://github.com/vector-im/element-android/issues/4152))
 - Do not show shortcuts if a PIN code is set ([#4170](https://github.com/vector-im/element-android/issues/4170))
 - Fixes being unable to join rooms by name ([#4255](https://github.com/vector-im/element-android/issues/4255))
 - Fixing missing F-Droid notifications when in background due to background syncs not triggering ([#4298](https://github.com/vector-im/element-android/issues/4298))
 - Fix video compression before upload ([#4353](https://github.com/vector-im/element-android/issues/4353))
 - Fixing QR code crashes caused by a known issue in the zxing library for older versions of android by downgrading to 3.3.3 ([#4361](https://github.com/vector-im/element-android/issues/4361))
 - Fixing timeline crash when rotating with the emoji window open ([#4365](https://github.com/vector-im/element-android/issues/4365))
 - Fix handling of links coming from web instance reported as malformed by mistake ([#4369](https://github.com/vector-im/element-android/issues/4369))

SDK API changes ⚠️
------------------
 - Add API `LoginWizard.loginCustom(data: JsonDict): Session` to be able to login to a homeserver using arbitrary request content ([#4266](https://github.com/vector-im/element-android/issues/4266))
 - Add optional deviceId to the login API ([#4334](https://github.com/vector-im/element-android/issues/4334))

Other changes
-------------
 - Migrate app DI framework to Hilt ([#3888](https://github.com/vector-im/element-android/issues/3888))
 - Limit supported TLS versions and cipher suites ([#4192](https://github.com/vector-im/element-android/issues/4192))
 - Fixed capitalisation of text on initial sync screen ([#4292](https://github.com/vector-im/element-android/issues/4292))


Changes in Element v1.3.6 (2021-10-26)
======================================

Bugfixes 🐛
----------
 - Correctly handle url of type https://mobile.element.io/?hs_url=…&is_url=…
  Skip the choose server screen when such URL are open when Element ([#2684](https://github.com/vector-im/element-android/issues/2684))


Changes in Element v1.3.5 (2021-10-25)
======================================

Bugfixes 🐛
----------
 - Fixing malformed link pop up when tapping on notifications ([#4267](https://github.com/vector-im/element-android/issues/4267))
 - Fix Broken EditText when using FromEditTextItem ([#4276](https://github.com/vector-im/element-android/issues/4276))
 - Fix crash when clicking on ViewEvent source actions ([#4279](https://github.com/vector-im/element-android/issues/4279))
 - Fix voice message record button wrong visibility ([#4283](https://github.com/vector-im/element-android/issues/4283))
 - Fix unread marker not showing ([#4313](https://github.com/vector-im/element-android/issues/4313))


Changes in Element v1.3.4 (2021-10-20)
======================================

Features ✨
----------
 - Implement /part command, with or without parameter ([#2909](https://github.com/vector-im/element-android/issues/2909))
 - Handle Presence support, for Direct Message room ([#4090](https://github.com/vector-im/element-android/issues/4090))
 - Priority conversations for Android 11+ ([#3313](https://github.com/vector-im/element-android/issues/3313))

Bugfixes 🐛
----------
 - Issue #908 Adding trailing space " " or ": " if the user started a sentence by mentioning someone, ([#908](https://github.com/vector-im/element-android/issues/908))
 - Fixes reappearing notifications when dismissing notifications from slow homeservers or delayed /sync responses ([#3437](https://github.com/vector-im/element-android/issues/3437))
 - Catching event decryption crash and logging when attempting to markOlmSessionForUnwedging fails ([#3608](https://github.com/vector-im/element-android/issues/3608))
 - Fixing notification sounds being triggered for every message, now they only trigger for the first, consistent with the vibrations ([#3774](https://github.com/vector-im/element-android/issues/3774))
 - Voice Message not sendable if recorded while flight mode was on ([#4006](https://github.com/vector-im/element-android/issues/4006))
 - Fixes push notification emails list not refreshing the first time seeing the notifications page.
  Also improves the error handling in the email notification toggling by using synchronous flows instead of the WorkManager ([#4106](https://github.com/vector-im/element-android/issues/4106))
 - Make MegolmBackupAuthData.signatures optional for robustness ([#4162](https://github.com/vector-im/element-android/issues/4162))
 - Fixing push notifications starting the looping background sync when the push notification causes the application to be created. ([#4167](https://github.com/vector-im/element-android/issues/4167))
 - Fix random crash when user logs out just after the log in. ([#4193](https://github.com/vector-im/element-android/issues/4193))
 - Make the font size selection dialog scrollable ([#4201](https://github.com/vector-im/element-android/issues/4201))
 - Fix conversation notification for sent messages ([#4221](https://github.com/vector-im/element-android/issues/4221))
 - Fixes the developer sync options being displayed in the home menu when developer mode is disabled ([#4234](https://github.com/vector-im/element-android/issues/4234))
 - Restore support for Android Auto as sent messages are no longer read aloud ([#4247](https://github.com/vector-im/element-android/issues/4247))
 - Fix crash on slash commands Exceptions ([#4261](https://github.com/vector-im/element-android/issues/4261))

Other changes
-------------
 - Scrub user sensitive data like gps location from images when sending on original quality ([#465](https://github.com/vector-im/element-android/issues/465))
 - Migrate to MvRx2 (Mavericks) ([#3890](https://github.com/vector-im/element-android/issues/3890))
 - Implement a new github action workflow to generate two PRs for emoji and sas string sync ([#4216](https://github.com/vector-im/element-android/issues/4216))
 - Improve wording around rageshakes in the defect issue template. ([#4226](https://github.com/vector-im/element-android/issues/4226))
 - Add automation to move incoming issues and X-Needs-Info into the right places on the issue triage board. ([#4250](https://github.com/vector-im/element-android/issues/4250))
 - Uppon sharing image compression fails, return the original image ([#4264](https://github.com/vector-im/element-android/issues/4264))


Changes in Element v1.3.3 (2021-10-11)
======================================

Bugfixes 🐛
----------
 - Disable Android Auto supports ([#4205](https://github.com/vector-im/element-android/issues/4205))


Changes in Element v1.3.2 (2021-10-08)
======================================

Features ✨
----------
 - Android Auto notification support ([#240](https://github.com/vector-im/element-android/issues/240))
 - Add a fallback for user displayName when this one is null or empty ([#3732](https://github.com/vector-im/element-android/issues/3732))
 - Add client base url config to customize permalinks ([#4027](https://github.com/vector-im/element-android/issues/4027))
 - Check if DM exists before creating a new one ([#4157](https://github.com/vector-im/element-android/issues/4157))
 - Handle 8 new slash commands: `/ignore`, `/unignore`, `/roomname`, `/myroomnick`, `/roomavatar`, `/myroomavatar`, `/lenny`, `/whois`. ([#4158](https://github.com/vector-im/element-android/issues/4158))
 - Display identity server policies in the Discovery screen ([#4184](https://github.com/vector-im/element-android/issues/4184))

Bugfixes 🐛
----------
 - Ensure initial sync progress dialog is hidden when the initial sync is over ([#983](https://github.com/vector-im/element-android/issues/983))
 - Avoid resending notifications that are already shown ([#1673](https://github.com/vector-im/element-android/issues/1673))
 - Room filter no results bad CTA in space mode when a space selected ([#3048](https://github.com/vector-im/element-android/issues/3048))
 - Fixes notifications not dismissing when reading messages on other devices ([#3347](https://github.com/vector-im/element-android/issues/3347))
 - Fixes the passphrase screen being incorrectly shown when pressing back on the key verification screen.
  When the user doesn't have a passphrase set we don't show the passphrase screen. ([#3898](https://github.com/vector-im/element-android/issues/3898))
 - App doesn't take you to a Space after choosing to Join it ([#3933](https://github.com/vector-im/element-android/issues/3933))
 - Validate public space addresses and room aliases length ([#3934](https://github.com/vector-im/element-android/issues/3934))
 - Save button for adding rooms to a space is hidden when scrolling through list of rooms ([#3935](https://github.com/vector-im/element-android/issues/3935))
 - Align new room encryption default to Web ([#4045](https://github.com/vector-im/element-android/issues/4045))
 - Fix Reply/Edit mode animation is broken when sending ([#4077](https://github.com/vector-im/element-android/issues/4077))
 - Added changes that will make SearchView in search bar focused by default on opening reaction picker.

  When tapping close icon of SearchView, the SearchView did not collapse therefore added the on close listener
  which will collapse the SearchView on close. ([#4092](https://github.com/vector-im/element-android/issues/4092))
 - Troubleshoot notification: Fix button not clickable ([#4109](https://github.com/vector-im/element-android/issues/4109))
 - Harmonize wording in the message bottom sheet and move up the View Reactions item ([#4155](https://github.com/vector-im/element-android/issues/4155))
 - Remove unused SendRelationWorker and related API call (3588) ([#4156](https://github.com/vector-im/element-android/issues/4156))
 - SIP user to native user mapping is wrong ([#4176](https://github.com/vector-im/element-android/issues/4176))

SDK API changes ⚠️
------------------
 - Create extension `String.isMxcUrl()` ([#4158](https://github.com/vector-im/element-android/issues/4158))

Other changes
-------------
 - Use ktlint plugin. See [the documentation](https://github.com/vector-im/element-android/blob/develop/CONTRIBUTING.md#ktlint) for more detail. ([#3957](https://github.com/vector-im/element-android/issues/3957))
 - Minimize the use of exported="true" in android Manifest (link: https://github.com/matrix-org/matrix-dinsic/issues/618) ([#4018](https://github.com/vector-im/element-android/issues/4018))
 - Fix redundancy in heading in the bug report issue form ([#4076](https://github.com/vector-im/element-android/issues/4076))
 - Fix release label in the release issue template ([#4113](https://github.com/vector-im/element-android/issues/4113))


Changes in Element v1.3.1 (2021-09-29)
======================================

Bugfixes 🐛
----------
 - Verifying exported E2E keys to provide user feedback when the output is malformed ([#4082](https://github.com/vector-im/element-android/issues/4082))
 - Fix settings crash when accelerometer not available ([#4103](https://github.com/vector-im/element-android/issues/4103))
 - Crash while rendering failed message warning ([#4110](https://github.com/vector-im/element-android/issues/4110))


Changes in Element v1.3.0 (2021-09-27)
======================================

Features ✨
----------
 - Spaces!
 - Adds email notification registration to Settings ([#2243](https://github.com/vector-im/element-android/issues/2243))
 - Spaces | M3.23 Invite by email in create private space flow ([#3678](https://github.com/vector-im/element-android/issues/3678))
 - Improve space invite bottom sheet ([#4057](https://github.com/vector-im/element-android/issues/4057))
 - Allow to also leave rooms when leaving a space ([#3692](https://github.com/vector-im/element-android/issues/3692))
 - Better expose adding spaces as Subspaces ([#3752](https://github.com/vector-im/element-android/issues/3752))
 - Push and syncs: add debug info on room list and on room detail screen and improves the log format. ([#4046](https://github.com/vector-im/element-android/issues/4046))

Bugfixes 🐛
----------
 - Remove the "Teammate spaces aren't quite ready" bottom sheet ([#3945](https://github.com/vector-im/element-android/issues/3945))
 - Restricted Room previews aren't working ([#3946](https://github.com/vector-im/element-android/issues/3946))
 - A removed room from a space can't be re-added as it won't be shown in add-room ([#3947](https://github.com/vector-im/element-android/issues/3947))
 - "Non-Admin" user able to invite others to Private Space (by default) ([#3951](https://github.com/vector-im/element-android/issues/3951))
 - Kick user dialog for spaces talks about rooms ([#3956](https://github.com/vector-im/element-android/issues/3956))
 - Messages are displayed as unable to decrypt then decrypted a few seconds later ([#4011](https://github.com/vector-im/element-android/issues/4011))
 - Fix DTMF not working ([#4015](https://github.com/vector-im/element-android/issues/4015))
 - Fix sticky end call notification ([#4019](https://github.com/vector-im/element-android/issues/4019))
 - Fix call screen stuck with some hanging up scenarios ([#4026](https://github.com/vector-im/element-android/issues/4026))
 - Fix other call not always refreshed when ended ([#4028](https://github.com/vector-im/element-android/issues/4028))
 - Private space invite bottomsheet only offering inviting by username not by email ([#4042](https://github.com/vector-im/element-android/issues/4042))
 - Spaces invitation system notifications don't take me to the join space toast ([#4043](https://github.com/vector-im/element-android/issues/4043))
 - Space Invites are not lighting up the drawer menu ([#4059](https://github.com/vector-im/element-android/issues/4059))
 - MessageActionsBottomSheet not being shown on local echos ([#4068](https://github.com/vector-im/element-android/issues/4068))

SDK API changes ⚠️
------------------
 - InitialSyncProgressService has been renamed to SyncStatusService and its function getInitialSyncProgressStatus() has been renamed to getSyncStatusLive() ([#4046](https://github.com/vector-im/element-android/issues/4046))

Other changes
-------------
 - Better support for Sdk2 version. Also slight change in the default user agent: `MatrixAndroidSDK_X` is replaced by `MatrixAndroidSdk2` ([#3994](https://github.com/vector-im/element-android/issues/3994))
 - Introduces ConferenceEvent to abstract usage of Jitsi BroadcastEvent class. ([#4014](https://github.com/vector-im/element-android/issues/4014))
 - Improve performances on RoomDetail screen ([#4065](https://github.com/vector-im/element-android/issues/4065))


Changes in Element v1.2.2 (2021-09-13)
======================================

Bugfixes 🐛
----------

- Fix a security issue with message key sharing. See https://matrix.org/blog/2021/09/13/vulnerability-disclosure-key-sharing for details.


Changes in Element v1.2.1 (2021-09-08)
======================================

Features ✨
----------
 - Support Android 11 Conversation features ([#1809](https://github.com/vector-im/element-android/issues/1809))
 - Introduces AutoAcceptInvites which can be enabled at compile time. ([#3531](https://github.com/vector-im/element-android/issues/3531))
 - New call designs ([#3599](https://github.com/vector-im/element-android/issues/3599))
 - Restricted Join Rule | Inform admins of new option ([#3631](https://github.com/vector-im/element-android/issues/3631))
 - Mention and Keyword Notification Settings: Turn on/off keyword notifications and edit keywords. ([#3650](https://github.com/vector-im/element-android/issues/3650))
 - Support accept 3pid invite when email is not bound to account ([#3691](https://github.com/vector-im/element-android/issues/3691))
 - Space summary pagination ([#3693](https://github.com/vector-im/element-android/issues/3693))
 - Update Email invite to be aware of spaces ([#3695](https://github.com/vector-im/element-android/issues/3695))
 - M11.12 Spaces | Default to 'Home' in settings ([#3754](https://github.com/vector-im/element-android/issues/3754))
 - Call: show dialog for some ended reasons. ([#3853](https://github.com/vector-im/element-android/issues/3853))
 - Add expired account error code in the matrix SDK ([#3900](https://github.com/vector-im/element-android/issues/3900))
 - Add password errors in the matrix SDK ([#3927](https://github.com/vector-im/element-android/issues/3927))
 - Upgrade AGP to 7.0.2.
  When compiling using command line, make sure to use the JDK 11 by adding for instance `-Dorg.gradle.java.home=/Applications/Android\ Studio\ Preview.app/Contents/jre/Contents/Home` or by setting JAVA_HOME. ([#3954](https://github.com/vector-im/element-android/issues/3954))
 - Check power level before displaying actions in the room details' timeline ([#3959](https://github.com/vector-im/element-android/issues/3959))

Bugfixes 🐛
----------
 - Add mxid to autocomplete suggestion if more than one user in a room has the same displayname ([#1823](https://github.com/vector-im/element-android/issues/1823))
 - Use WebView cache for widgets to avoid excessive data use ([#2648](https://github.com/vector-im/element-android/issues/2648))
 - Jitsi-hosted jitsi conferences not loading ([#2846](https://github.com/vector-im/element-android/issues/2846))
 - Space Explore Rooms no feedback on failed to join ([#3207](https://github.com/vector-im/element-android/issues/3207))
 - Notifications - Fix missing sound on notifications. ([#3243](https://github.com/vector-im/element-android/issues/3243))
 - the element-based domain permalinks (e.g. https://app.element.io/#/user/@chagai95:matrix.org) don't have the mxid in the first param (like matrix.to does - https://matrix.to/#/@chagai95:matrix.org) but rather in the second after /user/ so /user/mxid ([#3735](https://github.com/vector-im/element-android/issues/3735))
 - Update the AccountData with the users' matrix Id instead of their email for those invited by email in a direct chat ([#3743](https://github.com/vector-im/element-android/issues/3743))
 - Send an empty body for POST rooms/{roomId}/receipt/{receiptType}/{eventId} ([#3789](https://github.com/vector-im/element-android/issues/3789))
 - Fix order in which the items of the attachment menu appear ([#3793](https://github.com/vector-im/element-android/issues/3793))
 - Authenticated Jitsi not working in release ([#3841](https://github.com/vector-im/element-android/issues/3841))
 - Home: Dial pad lost entry when config changes ([#3845](https://github.com/vector-im/element-android/issues/3845))
 - Message edition is not rendered in e2e rooms after pagination ([#3887](https://github.com/vector-im/element-android/issues/3887))
 - Crash on opening a room on Android 5.0 and 5.1 - Regression with Voice message ([#3897](https://github.com/vector-im/element-android/issues/3897))
 - Fix a crash at start-up if translated string is empty ([#3910](https://github.com/vector-im/element-android/issues/3910))
 - PushRule enabling request is not following the spec ([#3911](https://github.com/vector-im/element-android/issues/3911))
 - Enable image preview in Android's share sheet (Android 11+) ([#3965](https://github.com/vector-im/element-android/issues/3965))
 - Voice Message - Cannot render voice message if the waveform data is corrupted ([#3983](https://github.com/vector-im/element-android/issues/3983))
 - Fix memory leak on RoomDetailFragment (ValueAnimator) ([#3990](https://github.com/vector-im/element-android/issues/3990))

Other changes
-------------
 - VoIP: Merge virtual room timeline in corresponding native room (call events only). ([#3520](https://github.com/vector-im/element-android/issues/3520))
 - Issue templates: modernise and sync with element-web ([#3883](https://github.com/vector-im/element-android/issues/3883))
 - Issue templates: modernise SDK and release checklists, and add homeserver question for bugs ([#3889](https://github.com/vector-im/element-android/issues/3889))
 - Issue templates: merge expected and actual results ([#3960](https://github.com/vector-im/element-android/issues/3960))


Changes in Element v1.2.0 (2021-08-12)
======================================

Features ✨
----------
 - Reorganise Advanced Notifications in to Default Notifications, Keywords and Mentions, Other (This feature is hidden in the release ui until a future release date.) ([#3646](https://github.com/vector-im/element-android/issues/3646))
 - Voice Message - Enable by default, remove from labs ([#3817](https://github.com/vector-im/element-android/issues/3817))

Bugfixes 🐛
----------
 - Voice Message - UI Improvements ([#3798](https://github.com/vector-im/element-android/issues/3798))
 - Stop VMs playing in the timeline if a new VM recording is started ([#3802](https://github.com/vector-im/element-android/issues/3802))


Changes in Element v1.1.16 (2021-08-09)
=======================================

Features ✨
----------
 - Spaces - Support Restricted Room via room capabilities API ([#3509](https://github.com/vector-im/element-android/issues/3509))
 - Spaces | Support restricted room access in room settings ([#3665](https://github.com/vector-im/element-android/issues/3665))

Bugfixes 🐛
----------
 - Fix crash when opening Troubleshoot Notifications ([#3778](https://github.com/vector-im/element-android/issues/3778))
 - Fix error when sending encrypted message if someone in the room logs out. ([#3792](https://github.com/vector-im/element-android/issues/3792))
 - Voice Message - Amplitude update java.util.ConcurrentModificationException ([#3796](https://github.com/vector-im/element-android/issues/3796))


Changes in Element v1.1.15 (2021-07-30)
=======================================

Features ✨
----------
 - Voice Message implementation (Currently under Labs Settings and disabled by default). ([#3598](https://github.com/vector-im/element-android/issues/3598))

SDK API changes ⚠️
------------------
 - updatePushRuleActions signature has been updated to more explicitly enabled/disable the rule and update the actions. It's behaviour has also been changed to match the web with the enable/disable requests being sent on every invocation and actions sent when needed(not null). ([#3681](https://github.com/vector-im/element-android/issues/3681))


Changes in Element 1.1.14 (2021-07-23)
======================================

Features ✨
----------
 - Add low priority section in DM tab ([#3463](https://github.com/vector-im/element-android/issues/3463))
 - Show missed call notification. ([#3710](https://github.com/vector-im/element-android/issues/3710))

Bugfixes 🐛
----------
 - Don't use the transaction ID of the verification for the request ([#3589](https://github.com/vector-im/element-android/issues/3589))
 - Avoid incomplete downloads in cache ([#3656](https://github.com/vector-im/element-android/issues/3656))
 - Fix a crash which can happen when user signs out ([#3720](https://github.com/vector-im/element-android/issues/3720))
 - Ensure OTKs are uploaded when the session is created ([#3724](https://github.com/vector-im/element-android/issues/3724))

SDK API changes ⚠️
------------------
 - Add initialState support to CreateRoomParams (#3713) ([#3713](https://github.com/vector-im/element-android/issues/3713))

Other changes
-------------
 - Apply grammatical fixes to the Server ACL timeline messages. ([#3721](https://github.com/vector-im/element-android/issues/3721))
 - Add tags in the log, especially for VoIP, but can be used for other features in the future ([#3723](https://github.com/vector-im/element-android/issues/3723))


Changes in Element v1.1.13 (2021-07-19)
=======================================

Features ✨
----------
 - Remove redundant mimetype (vector-im/element-web#2547) ([#3273](https://github.com/vector-im/element-android/issues/3273))
 - Room version capabilities and room upgrade support, better error feedback ([#3551](https://github.com/vector-im/element-android/issues/3551))
 - Add retry support in room addresses screen ([#3635](https://github.com/vector-im/element-android/issues/3635))
 - Better management of permission requests ([#3667](https://github.com/vector-im/element-android/issues/3667))

Bugfixes 🐛
----------
 - Standardise spelling and casing of homeserver, identity server, and integration manager. ([#491](https://github.com/vector-im/element-android/issues/491))
 - Perform .well-known request first, even if the entered URL is a valid homeserver base url ([#2843](https://github.com/vector-im/element-android/issues/2843))
 - Use different copy for self verification. ([#3624](https://github.com/vector-im/element-android/issues/3624))
 - Crash when opening room addresses screen with no internet connection ([#3634](https://github.com/vector-im/element-android/issues/3634))
 - Fix unread messages marker being hidden in collapsed membership item ([#3655](https://github.com/vector-im/element-android/issues/3655))
 - Ensure reaction emoji picker tabs look fine on small displays ([#3661](https://github.com/vector-im/element-android/issues/3661))

SDK API changes ⚠️
------------------
 - RawService.getWellknown() now takes a domain instead of a matrixId as parameter ([#3572](https://github.com/vector-im/element-android/issues/3572))


Changes in Element 1.1.12 (2021-07-05)
======================================

Features ✨
----------
 - Reveal password: use facility from com.google.android.material.textfield.TextInputLayout instead of manual handling. ([#3545](https://github.com/vector-im/element-android/issues/3545))
 - Implements new design for Jump to unread and quick fix visibility issues. ([#3547](https://github.com/vector-im/element-android/issues/3547))

Bugfixes 🐛
----------
 - Fix some issues with timeline cache invalidation and visibility. ([#3542](https://github.com/vector-im/element-android/issues/3542))
 - Fix call invite processed after call is ended because of fastlane mode. ([#3564](https://github.com/vector-im/element-android/issues/3564))
 - Fix crash after video call. ([#3577](https://github.com/vector-im/element-android/issues/3577))
 - Fix crash out of memory ([#3583](https://github.com/vector-im/element-android/issues/3583))
 - CryptoStore migration has to be object to avoid crash ([#3605](https://github.com/vector-im/element-android/issues/3605))


Changes in Element v1.1.11 (2021-06-22)
=======================================

Bugfixes 🐛
----------
 - Send button does not show up half of the time ([#3535](https://github.com/vector-im/element-android/issues/3535))
 - Fix crash on signout: release session at the end of clean up. ([#3538](https://github.com/vector-im/element-android/issues/3538))


Changes in Element v1.1.10 (2021-06-18)
=======================================

Features ✨
----------
 - Migrate DefaultTypingService, KeysImporter and KeysExporter to coroutines ([#2449](https://github.com/vector-im/element-android/issues/2449))
 - Update Message Composer design ([#3182](https://github.com/vector-im/element-android/issues/3182))
 - Cleanup Epoxy items, and debounce all the clicks ([#3435](https://github.com/vector-im/element-android/issues/3435))
 - Adds support for receiving MSC3086 Asserted Identity events. ([#3451](https://github.com/vector-im/element-android/issues/3451))
 - Migrate to new colors and cleanup the style and theme. Now exported in module :library:ui-styles
  Ref: https://material.io/blog/migrate-android-material-components ([#3459](https://github.com/vector-im/element-android/issues/3459))
 - Add option to set aliases for public spaces ([#3483](https://github.com/vector-im/element-android/issues/3483))
 - Add beta warning to private space creation flow ([#3485](https://github.com/vector-im/element-android/issues/3485))
 - User defined top level spaces ordering ([#3501](https://github.com/vector-im/element-android/issues/3501))

Bugfixes 🐛
----------
 - Fix new DMs not always marked as such ([#3333](https://github.com/vector-im/element-android/issues/3333))

SDK API changes ⚠️
------------------
 - Splits SessionAccountDataService and RoomAccountDataService and offers to query RoomAccountDataEvent at the session level. ([#3479](https://github.com/vector-im/element-android/issues/3479))

Other changes
-------------
 - Move the ability to start a call from dialpad directly to a dedicated tab in the home screen. ([#3457](https://github.com/vector-im/element-android/issues/3457))
 - VoIP: Change hold direction to send-only. ([#3467](https://github.com/vector-im/element-android/issues/3467))
 - Some improvements on DialPad (cursor edition, paste number, small fixes). ([#3516](https://github.com/vector-im/element-android/issues/3516))


Changes in Element v1.1.9 (2021-06-02)
======================================

Features ✨:
 - Upgrade Olm dependency to 3.2.4
 - Allow user to add custom "network" in room search (#1458)
 - Add Gitter.im as a default in the Change Network menu (#3196)
 - VoIP: support for virtual rooms (#3355)
 - Compress thumbnail: change Jpeg quality from 100 to 80 (#3396)
 - Inconsistent usage of the term homeserver in Settings (#3404)
 - VoIP: support attended transfer (#3420)
 - /snow -> /snowfall and update wording (iso Element Web) (#3430)

Bugfixes 🐛:
 - Fix | On Android it seems to be impossible to view the complete description of a Space (without dev tools) (#3401)
 - Fix | Suggest Rooms, Show a detailed view of the room on click (#3406)
 - Fix app crashing when signing out (#3424)
 - Switch to stable endpoint/fields for MSC2858 (#3442)

Changes in Element 1.1.8 (2021-05-25)
===================================================

Improvements 🙌:
 - Support Jitsi authentication (#3379)

Bugfix 🐛:
 - Space Invite by link not always displayed for public space (#3345)
 - Wrong copy in share space bottom sheet (#3346)
 - Fix a problem with database migration on nightly builds (#3335)
 - Implement a workaround to render &lt;del&gt; and &lt;u&gt; in the timeline (#1817)
 - Make sure the SDK can retrieve the secret storage if the system is upgraded (#3304)
 - Spaces | Explore room list: the RoomId is displayed instead of name (#3371)
 - Spaces | Personal spaces add DM - Web Parity (#3271)
 - Spaces | Improve 'Leave Space' UX/UI (#3359)
 - Don't create private spaces with encryption enabled (#3363)
 - #+ button on lower right when looking at an empty space goes to an empty 'Explore rooms' (#3327)

Build 🧱:
 - Compile with Kotlin 1.5.10.
 - Upgrade some dependencies: gradle wrapper, third party lib, etc.
 - Sign APK with build tools 30.0.3

Other changes:
 - Add documentation on LoginWizard and RegistrationWizard (#3303)
 - Setup towncrier tool (#3293)

 Security:
  - Element Android shares name of E2EE files with homeserver (#3387)

Changes in Element 1.1.7 (2021-05-12)
===================================================

Features ✨:
 - Spaces beta

Improvements 🙌:
 - Add ability to install APK from directly from Element (#2381)
 - Delete and react to stickers (#3250)
 - Compress video before sending (#442)
 - Improve file too big error detection (#3245)
 - User can now select video when selecting Gallery to send attachments to a room
 - Add option to record a video from the camera
 - Add the public icon on the rooms in the room list (#3292)

Bugfix 🐛:
 - Message states cosmetic changes (#3007)
 - Fix exception in rxSingle (#3180)
 - Do not invite the current user when creating a room (#3123)
 - Fix color issues when the system theme is changed (#2738)
 - Fix issues on Android 11 (#3067)
 - Fix issue when opening encrypted files (#3186)
 - Fix wording issue (#3242)
 - Fix missing sender information after edits (#3184)
 - Fix read marker not updating automatically (#3267)
 - Sent video does not contains duration (#3272)
 - Properly clean the back stack if the user cancel registration when waiting for email validation
 - Fix read marker visibility/position when filtering some events 
 - Fix user invitation in case of restricted profile api (#3306)

SDK API changes ⚠️:
 - RegistrationWizard.createAccount() parameters are now all optional, following Matrix spec (#3205)

Build 🧱:
 - Upgrade to gradle 7
 - https://github.com/Piasy/BigImageViewer is now hosted on mavenCentral()
 - Upgrade Realm to version 10.4.0

Other changes:
 - New store descriptions
 - `master` branch has been renamed to `main`. To apply change to your dev environment, run:
```sh
git branch -m master main
git fetch origin
git branch -u origin/main main
# And optionally
git remote prune origin
```
 - Allow cleartext (non-SSL) connections to Matrix servers on LAN hosts (#3166)

Changes in Element 1.1.6 (2021-04-16)
===================================================

Bugfix 🐛:
 - Fix crash on the timeline
 - App crashes on "troubleshoot notifications" button (#3187)

Changes in Element 1.1.5 (2021-04-15)
===================================================

Bugfix 🐛:
 - Fix crash during Realm migration
 - Fix crash when playing video (#3179)

Changes in Element 1.1.4 (2021-04-09)
===================================================

Improvements 🙌:
 - Split network request `/keys/query` into smaller requests (250 users max) (#2925)
 - Crypto improvement | Bulk send NO_OLM withheld code
 - Display the room shield in all room setting screens
 - Improve message with Emoji only detection (#3017)
 - Picture preview when replying. Also add the image preview in the message detail bottomsheet (#2916)
 - Api interceptor to allow app developers peek responses (#2986)
 - Update reactions to Unicode 13.1 (#2998)
 - Be more robust when parsing some enums
 - Improve timeline filtering (dissociate membership and profile events, display hidden events when highlighted, fix hidden item/read receipts behavior)
 - Add better support for empty room name fallback (#3106)
 - Room list improvements (paging)
 - Fix quick click action (#3127)
 - Get Event after a Push for a faster notification display in some conditions
 - Always try to retry Http requests in case of 429 (#1300)
 - registration availability endpoint added to matrix-sdk

Bugfix 🐛:
 - Fix bad theme change for the MainActivity
 - Handle encrypted reactions (#2509)
 - Disable URL preview for some domains (#2995)
 - Fix avatar rendering for DMs, after initial sync (#2693)
 - Fix mandatory parameter in API (#3065)
 - If signout request fails, do not start LoginActivity, but restart the app (#3099)
 - Retain keyword order in emoji import script, and update the generated file (#3147)

SDK API changes ⚠️:
 - Several Services have been migrated to coroutines (#2449)
 - Removes filtering options on Timeline.

Build 🧱:
 - Properly exclude gms dependencies in fdroid build flavour which were pulled in through the jitsi SDK (#3125)

Other changes:
 - Add version details on the login screen, in debug or developer mode
 - Migrate Retrofit interface to coroutine calls

Changes in Element 1.1.3 (2021-03-18)
===================================================

Bugfix 🐛:
 - Fix regression in UpdateTrustWorker (introduced in 1.1.2)
 - Timeline : Fix ripple effect on text item and fix background color of separators.

Changes in Element 1.1.2 (2021-03-16) (was not published tp GPlay prod)
===================================================

Improvements 🙌:
 - Lazy storage of ReadReceipts
 - Do not load room members in e2e after init sync

Bugfix 🐛:
 - Add option to cancel stuck messages at bottom of timeline see #516
 - Ensure message are decrypted in the room list after a clear cache
 - Regression: Video will not play upon tap, but only after swipe #2928
 - Cross signing now works with servers with an explicit port in the servername

Other changes:
 - Change formatting on issue templates to proper headings.

Changes in Element 1.1.1 (2021-03-10) (was not published tp GPlay prod)
===================================================

Improvements 🙌:
 - Allow non-HTTPS connections to homeservers on Tor (#2941)
 - Fetch homeserver type and version and display in a new setting screen and add info in rageshakes (#2831)
 - Improve initial sync performance - split into 2 transactions (#983)
 - PIP support for Jitsi call (#2418)
 - Add tooltip for room quick actions
 - Pre-share session keys when opening a room or start typing (#2771)
 - Sending is now queuing by room and not uniquely to the session
 - Improve Snackbar duration (#2929)
 - Improve sending message state (#2937)

Bugfix 🐛:
 - Try to fix crash about UrlPreview (#2640)
 - Be robust if Event.type is missing (#2946)
 - Snappier message send status
 - Fix MainActivity display (#2927)

Translations 🗣:
 - All string resources and translations have been moved to the application module. Weblate project for the SDK will be removed.

Build 🧱:
 - Update a lot of dependencies, with the help of dependabot.
 - Add a script to download and install APK from the CI

Other changes:
 - Rework edition of event management

Changes in Element 1.1.0 (2021-02-19)
===================================================

Features ✨:
 - VoIP : support for VoIP V1 protocol, transfer call and dial-pad
                                
Improvements 🙌:
 - VoIP : new tiles in timeline
 - Improve room profile UX
 - Upgrade Jitsi library from 2.9.3 to 3.1.0
 - a11y improvements

Bugfix 🐛:
 - VoIP : fix audio devices output
 - Fix crash after initial sync on Dendrite
 - Fix crash reported by PlayStore (#2707)
 - Ignore url override from credential if it is not valid (#2822)
 - Fix crash when deactivating an account

SDK API changes ⚠️:
 - Migrate AuthenticationService API to coroutines (#2449)

Other changes:
 - New Dev Tools panel for developers
 - Fix typos in CHANGES.md (#2811)
 - Colors rework: first step: merge file `colors_riot.xml` to file `colors_riotx.xml` and rename the file to `colors.xml`

Changes in Element 1.0.17 (2021-02-09)
===================================================

Improvements 🙌:
 - Create a WidgetItemFactory and use it for better rendering of Jitsi widget change (video conference)
 - Open image from URL Preview (#2705)

Bugfix 🐛:
 - Bug in WidgetContent.computeURL() (#2767)
 - Duplicate thumbs | Mobile reactions for 👍 and 👎 are not the same as web (#2776)
 - Join room by alias other federation error (#2778)
 - HTML unescaping for URL preview (#2766)
 - URL preview on reply fallback (#2756)
 - RTL: some arrows should be rotated in RTL (#2757)
 - Properly delete objects from Realm DB (#2765)

Build 🧱:
 - Upgrade build tools

Other changes:
 - Change app name from "Element (Riot.im)" to "Element"

Changes in Element 1.0.16 (2021-02-04)
===================================================

Bugfix 🐛:
 - Fix crash on API < 30 and light theme (#2774)

Changes in Element 1.0.15 (2021-02-03)
===================================================

Features ✨:
 - Social Login support

Improvements 🙌:
 - SSO support for cross signing (#1062)
 - Deactivate account when logged in with SSO (#1264)
 - SSO UIA doesn't work (#2754)

Bugfix 🐛:
 - Fix clear cache issue: sometimes, after a clear cache, there is still a token, so the init sync service is not started.
 - Sidebar too large in horizontal orientation or tablets (#475)
 - UrlPreview should be updated when the url is edited and changed (#2678)
 - When receiving a new pepper from identity server, use it on the next hash lookup (#2708)
 - Crashes reported by PlayStore (new in 1.0.14) (#2707)
 - Widgets: Support $matrix_widget_id parameter (#2748)
 - Data for Worker overload (#2721)
 - Fix multiple tasks
 - Object deletion in database is not complete (#2759)

SDK API changes ⚠️:
 - Increase targetSdkVersion to 30 (#2600)

Build 🧱:
 - Compile with Android SDK 30 (Android 11)

Other changes:
 - Update Dagger to 2.31 version so we can use the embedded AssistedInject feature 

Changes in Element 1.0.14 (2021-01-15)
===================================================

Features ✨:
 - Enable url previews for notices (#2562)
 - Edit room permissions (#2471)

Improvements 🙌:
 - Add System theme option and set as default (#904, #2387)
 - Store megolm outbound session to improve send time of first message after app launch.
 - Warn user when they are leaving a not public room (#1460)
 - Option to disable emoji keyboard (#2563)

Bugfix 🐛:
 - Unspecced msgType field in m.sticker (#2580)
 - Wait for all room members to be known before sending a message to a e2e room (#2518)
 - Url previews sometimes attached to wrong message (#2561)
 - Room Topic not displayed correctly after visiting a link (#2551)
 - Hiding membership events works the exact opposite (#2603)
 - Tapping drawer having more than 1 room in notifications gives "malformed link" error (#2605)
 - Sent image not displayed when opened immediately after sending (#409)
 - Initial sync is not retried correctly when there is some network error. (#2632)
 - Fix switch theme issue, and white field issue (#2599, #2528)
 - Fix request too large Uri error when joining a room

Translations 🗣:
 - New language supported: Hebrew

Build 🧱:
 - Remove dependency to org.greenrobot.eventbus library

Other changes:
 - Migrate to ViewBindings (#1072)

Changes in Element 1.0.13 (2020-12-18)
===================================================

Bugfix 🐛:
 - Fix MSC2858 implementation details (#2540)

Changes in Element 1.0.12 (2020-12-15)
===================================================

Features ✨:
 - Add room aliases management, and room directory visibility management in a dedicated screen (#1579, #2428)
 - Room setting: update join rules and guest access (#2442)
 - Url preview (#481)
 - Store encrypted file in cache and cleanup decrypted file at each app start (#2512)
 - Emoji Keyboard (#2520)
 - Social login (#2452)
 - Support for chat effects in timeline (confetti, snow) (#2535)

Improvements 🙌:
 - Add Setting Item to Change PIN (#2462)
 - Improve room history visibility setting UX (#1579)
 - Matrix.to deeplink custom scheme support
 - Homeserver history (#1933)

Bugfix 🐛:
 - Fix cancellation of sending event (#2438)
 - Double bottomsheet effect after verify with passphrase
 - EditText cursor jumps to the start while typing fast (#2469)
 - UTD for events before invitation if member state events are hidden (#2486)
 - No known servers error is given when joining rooms on new Gitter bridge (#2516)
 - Show preview when sending attachment from the keyboard (#2440)
 - Do not compress GIFs (#1616, #1254)

SDK API changes ⚠️:
 - StateService now exposes suspendable function instead of using MatrixCallback.
 - RawCacheStrategy has been moved and renamed to CacheStrategy
 - FileService: remove useless FileService.DownloadMode

Build 🧱:
 - Upgrade some dependencies and Kotlin version
 - Use fragment-ktx and preference-ktx dependencies (fix lint issue KtxExtensionAvailable)
 - Upgrade Realm dependency to 10.1.2

Other changes:
 - Remove "Status.im" theme #2424
 - Log HTTP requests and responses in production (level BASIC, i.e. without any private data)

Changes in Element 1.0.11 (2020-11-27)
===================================================

Features ✨:
 - Create DMs with users by scanning their QR code (#2025)
 - Add Invite friends quick invite actions (#2348)
 - Add friend by scanning QR code, show your code to friends (#2025)

Improvements 🙌:
 - New room creation tile with quick action (#2346)
 - Open an existing DM instead of creating a new one (#2319)
 - Use RoomMember instead of User in the context of a Room.
 - Ask for explicit user consent to send their contact details to the identity server (#2375)
 - Handle events of type "m.room.server_acl" (#890)
 - Room creation form: add advanced section to disable federation (#1314)
 - Move "Enable Encryption" from room setting screen to room profile screen (#2394)
 - Home empty screens quick design update (#2347)
 - Improve Invite user screen (seamless search for matrix ID)

Bugfix 🐛:
 - Fix crash on AttachmentViewer (#2365)
 - Exclude yourself when decorating rooms which are direct or don't have more than 2 users (#2370)
 - F-Droid version: ensure timeout of sync request can be more than 60 seconds (#2169)
 - Fix issue when restoring draft after sharing (#2287)
 - Fix issue when updating the avatar of a room (new avatar vanishing)
 - Discard change dialog displayed by mistake when avatar has been updated
 - Try to fix cropped image in timeline (#2126)
 - Registration: annoying error message scares every new user when they add an email (#2391)
 - Fix jitsi integration for those with non-vanilla dialler frameworks
 - Update profile has no effect if user is in zero rooms
 - Fix issues with matrix.to deep linking (#2349)

SDK API changes ⚠️:
 - AccountService now exposes suspendable function instead of using MatrixCallback (#2354).
   Note: We will incrementally migrate all the SDK API in a near future (#2449)

Test:
 - Add `allScreensTest` to cover all screens of the app

Other changes:
 - Upgrade Realm dependency to 10.0.0

Changes in Element 1.0.10 (2020-11-04)
===================================================

Improvements 🙌:
 - Rework sending Event management (#154)
 - New room creation screen: set topic and avatar in the room creation form (#2078)
 - Toggle Low priority tag (#1490)
 - Add option to send with enter (#1195)
 - Use Hardware keyboard enter to send message (use shift-enter for new line) (#1881, #1440)
 - Edit and remove icons are now visible on image attachment preview screen (#2294)
 - Room profile: BigImageViewerActivity now only display the image. Use the room setting to change or delete the room Avatar
 - Better visibility of text reactions in dark theme (#1118)
 - Room member profile: Add action to create (or open) a DM (#2310)
 - Prepare changelog for F-Droid (#2296)
 - Add graphic resources for F-Droid (#812, #2220)
 - Highlight text in the body of the displayed result (#2200)
 - Considerably faster QR-code bitmap generation (#2331)

Bugfix 🐛:
 - Fixed ringtone handling (#2100 & #2246)
 - Messages encrypted with no way to decrypt after SDK update from 0.18 to 1.0.0 (#2252)
 - Incoming call continues to ring if call is answered on another device (#1921)
 - Search Result | scroll jumps after pagination (#2238)
 - Badly formatted mentions in body (#1506)
 - KeysBackup: Avoid using `!!` (#2262)
 - Two elements in the task switcher (#2299)

Changes in Element 1.0.9 (2020-10-16)
===================================================

Features ✨:
 - Search messages in a room - phase 1 (#2110)
 - Hide encrypted history (before user is invited). Can be shown if wanted in developer settings
 - Changed rainbow algorithm

Improvements 🙌:
 - Wording differentiation for direct rooms (#2176)
 - PIN code: request PIN code if phone has been locked
 - Small optimisation of scrolling experience in timeline (#2114)
 - Allow user to reset cross signing if he has no way to recover (#2052)
 - Ability to share text
 - Create home shortcut for any room (#1525)
 - Can't confirm email due to killing by Android (#2021)
 - Add a menu item to open the setting in room list and in room (#2171)
 - Add a menu item in the timeline as a shortcut to invite user (#2171)
 - Drawer: move settings access and add sign out action (#2171)
 - Filter room member (and banned users) by name (#2184)
 - Implement "Jump to read receipt" and "Mention" actions on the room member profile screen
 - Direct share (#2029)
 - Add FAB to room members list (#2226)
 - Add Sygnal API implementation to test is Push are correctly received
 - Add PushGateway API implementation to test if Push are correctly received
 - Cross signing: shouldn't offer to verify with other session when there is not. (#2227)

Bugfix 🐛:
 - Improve support for image/audio/video/file selection with intent changes (#1376)
 - Fix Splash layout on small screens
 - Invalid popup when pressing back (#1635)
 - Simplifies draft management and should fix bunch of draft issues (#952, #683)
 - Very long topic cannot be fully visible (#1957)
 - Properly detect cross signing keys reset
 - Don't set presence when handling a push notification or polling (#2156)
 - Be robust against `StrandHogg` task injection
 - Clear alerts if user sign out
 - Fix rows are hidden in Textinput (#2234)
 - Uploading a file to a room caused it to have a info.size of -1 (#2141)

Translations 🗣:
 - Move store data to `/fastlane/metadata/android` (#812)
 - Weblate is now hosted at https://translate.element.io

SDK API changes ⚠️:
 - Search messages in a room by using Session.searchService() or Room.search()

Build 🧱:
 - Use Update Gradle Wrapper Action
 - Updates Gradle Wrapper from 5.6.4 to 6.6.1. (#2193)
 - Upgrade kotlin version from `1.3.72` to `1.4.10` and kotlin coroutines version from `1.3.8` to `1.3.9`
 - Upgrade build tools from `3.5.3` to `4.0.1`, then to `4.1.0`
 - Upgrade com.google.gms:google-services from `4.3.2` to `4.3.4`
 - Upgrade Moshi to `1.11.0`, Dagger to `2.29.1`, Epoxy to `4.1.0`

Other changes:
 - Added registration/verification automated UI tests
 - Create a script to help getting public information form any homeserver

Changes in Element 1.0.8 (2020-09-25)
===================================================

Improvements 🙌:
 - Add "show password" in import Megolm keys dialog
 - Visually disable call buttons in menu and prohibit calling when permissions are insufficient (#2112)
 - Better management of requested permissions (#2048)
 - Add a setting to show timestamp for all messages (#2123)
 - Use cache for user color
 - Allow using an outdated homeserver, at user's risk (#1972)
 - Restore small logo on login screens and fix scrolling issue on those screens
 - PIN Code Improvements: Add more settings: biometrics, grace period, notification content (#1985)

Bugfix 🐛:
 - Long message cannot be sent/takes infinite time & blocks other messages (#1397)
 - Fix crash when wellknown are malformed, or redirect to some HTML content (reported by rageshakes)
 - User Verification in DM not working
 - Manual import of Megolm keys does back up the imported keys
 - Auto scrolling to the latest message when sending (#2094)
 - Fix incorrect permission check when creating widgets (#2137)
 - Pin code: user has to enter pin code twice (#2005)

SDK API changes ⚠️:
 - Rename `tryThis` to `tryOrNull`

Other changes:
 - Add an advanced action to reset an account data entry

Changes in Element 1.0.7 (2020-09-17)
===================================================

Improvements 🙌:
 - Handle date formatting properly (show time am/pm if needed, display year when needed)
 - Improve F-Droid Notification (#2055)

Bugfix 🐛:
 - Clear the notification when the event is read elsewhere (#1822)
 - Speakerphone is not used for ringback tone (#1644, #1645)
 - Back camera preview is not mirrored anymore (#1776)
 - Various report of people that cannot play video (#2107)
 - Rooms incorrectly marked as unread (#588)
 - Allow users to show/hide room member state events (#1231) 
 - Fix stuck on loader when launching home

SDK API changes ⚠️:
 - Create a new RawService to get plain data from the server.

Other changes:
 - Performance: share Realm instance used on UI thread and improve SharedPreferences reading time.

Changes in Element 1.0.6 (2020-09-08)
===================================================

Features ✨:
 - List phone numbers and emails added to the Matrix account, and add emails and phone numbers to account (#44, #45)

Improvements 🙌:
 - You can now join room through permalink and within room directory search
 - Add long click gesture to copy userId, user display name, room name, room topic and room alias (#1774)
 - Fix several issues when uploading big files (#1889)
 - Do not propose to verify session if there is only one session and 4S is not configured (#1901)
 - Call screen does not use proximity sensor (#1735)

Bugfix 🐛:
 - Display name not shown under Settings/General (#1926)
 - Editing message forgets line breaks and markdown (#1939)
 - Words containing my name should not trigger notifications (#1781)
 - Fix changing language issue
 - Fix FontSize issue (#1483, #1787)
 - Fix bad color for settings icon on Android < 24 (#1786)
 - Change user or room avatar: when selecting Gallery, I'm not proposed to crop the selected image (#1590)
 - Loudspeaker is always used (#1685)
 - Fix uploads still don't work with room v6 (#1879)
 - Can't handle ongoing call events in background (#1992)
 - Handle room, user and group links by the Element app (#1795)
 - Update associated site domain (#1833)
 - Crash / Attachment viewer: Cannot draw a recycled Bitmap #2034
 - Login with Matrix-Id | Autodiscovery fails if identity server is invalid and Homeserver ok (#2027)
 - Support for image compression on Android 10
 - Verification popup won't show
 - Android 6: App crash when read Contact permission is granted (#2064)
 - JSON for verification events leaks in to the room list (#1246)
 - Replies to poll appears in timeline as unsupported events during sending (#1004)

Translations 🗣:
 - The SDK is now using SAS string translations from [Weblate Matrix-doc project](https://translate.element.io/projects/matrix-doc/) (#1909)
 - New translation to kabyle

Build 🧱:
 - Some dependencies have been upgraded (coroutine, recyclerView, appCompat, core-ktx, firebase-messaging)
 - Buildkite:
    New pipeline location: https://github.com/matrix-org/pipelines/blob/master/element-android/pipeline.yml
    New build location: https://buildkite.com/matrix-dot-org/element-android

Other changes:
 - Use File extension functions to make code more concise (#1996)
 - Create a script to import SAS strings (#1909)
 - Support `data-mx-[bg-]color` attributes on `<font>` tags.

Changes in Element 1.0.5 (2020-08-21)
===================================================

Features ✨:
 - Protect access to the app by a pin code (#1700)
 - Conference with Jitsi support (#43)

Improvements 🙌:
 - Share button in rooms gives room ID link without via parameters (#1927)
 - Give user the possibility to prevent accidental call (#1869)
 - Display device information (name, id and key) in Cryptography setting screen (#1784)
 - Ensure users do not accidentally ignore other users (#1890)
 - Better handling DM creation when invitees cannot be inviting due to denied federation
 - Support new config.json format and config.domain.json files (#1682)
 - Increase Font size on Calling screen (#1643)
 - Make the user's Avatar live in the general settings

Bugfix 🐛:
 - Fix incorrect date format for some Asian languages (#1928)
 - Fix invisible toolbar (Status.im theme) (#1746)
 - Fix relative date time formatting (#822)
 - Fix crash reported by RageShake
 - Fix refreshing of sessions list when another session is logged out
 - Fix IllegalArgumentException: Receiver not registered: NetworkInfoReceiver (#1960)
 - Failed to build unique file (#1954)
 - Highlighted Event when opening a permalink from another room (#1033)
 - A Kick appears has "someone has made no change" (#1959)
 - Avoid NetworkOnMainThreadException when setting a user avatar
 - Renew turnserver credentials when ttl runs out

Translations 🗣:
 - Add PlayStore description resources in the Triple-T format, to let Weblate handle them

SDK API changes ⚠️:
 - Rename package `im.vector.matrix.android` to `org.matrix.android.sdk`
 - Rename package `im.vector.matrix.rx` to `org.matrix.android.sdk.rx`

Build 🧱:
 - Fix RtlHardcoded issues (use `Start` and `End` instead of `Left` and `Right` layout attributes)

Other changes:
 - Use `Context#getSystemService` extension function provided by `core-ktx` (#1702)
 - Hide Flair settings, this is not implemented yet.
 - Rename package `im.vector.riotx.attachmentviewer` to `im.vector.lib.attachmentviewer`
 - Rename package `im.vector.riotx.multipicker` to `im.vector.lib.multipicker`
 - Rename package `im.vector.riotx` to `im.vector.app`
 - Remove old code that was used on devices with api level <21
 - Add Official Gradle Wrapper Validation Action

Changes in Element 1.0.4 (2020-08-03)
===================================================

Bugfix 🐛:
 - Fix Crash when opening invite to room user screen

Changes in Element 1.0.3 (2020-07-31)
===================================================

Features ✨:
 - Support server admin option to disable E2EE for DMs / private rooms [users can still enable] (#1794)

Bugfix 🐛:
 - Crash reported on playstore for HomeActivity launch (151 reports)

Changes in Element 1.0.2 (2020-07-29)
===================================================

Improvements 🙌:
 - Added Session Database migration to avoid unneeded initial syncs

Changes in Element 1.0.1 (2020-07-28)
===================================================

Improvements 🙌:
 - Sending events is now retried only 3 times, so we avoid blocking the sending queue too long.
 - Display warning when fail to send events in room list
 - Improve UI of edit role action in member profile
 - Moderation | New screen to display list of banned users in room settings, with unban action

Bugfix 🐛:
 - Fix theme issue on Room directory screen (#1613)
 - Fix notification not dismissing when entering a room
 - Fix uploads don't work with Room v6 (#1558)
 - Fix Requesting avatar thumbnails in Element uses wrong http "user-agent" string (#1725)
 - Fix 404 on EMS (#1761)
 - Fix Infinite loop at startup when migrating account from Riot (#1699)
 - Fix Element crashes in loop after initial sync (#1709)
 - Remove inner mx-reply tags before replying
 - Fix timeline items not loading when there are only filtered events
 - Fix "Voice & Video" grayed out in Settings (#1733)
 - Fix Allow VOIP call in all rooms with 2 participants (even if not DM)
 - Migration from old client does not enable notifications (#1723)

Other changes:
 - i18n deactivated account error

Changes in Element 1.0.0 (2020-07-15)
===================================================

Features ✨:
 - Re-branding: The app is now called Element. New name, new themes, new icons, etc. More details here: https://element.io/blog/welcome-to-element/ (#1691)

Bugfix 🐛:
 - Video calls are shown as a voice ones in the timeline (#1676)
 - Fix regression: not able to create a room without IS configured (#1679)

Changes in Riot.imX 0.91.5 (2020-07-11)
===================================================

Features ✨:
 - 3pid invite: it is now possible to invite people by email. An Identity Server has to be configured (#548)

Improvements 🙌:
 - Cleaning chunks with lots of events as long as a threshold has been exceeded (35_000 events in DB) (#1634)
 - Creating and listening to EventInsertEntity. (#1634)
 - Handling (almost) properly the groups fetching (#1634)
 - Improve fullscreen media display (#327)
 - Setup server recovery banner (#1648)
 - Set up SSSS from security settings (#1567)
 - New lab setting to add 'unread notifications' tab to main screen
 - Render third party invite event (#548)
 - Display three pid invites in the room members list (#548)

Bugfix 🐛:
 - Integration Manager: Wrong URL to review terms if URL in config contains path (#1606)
 - Regression Composer does not grow, crops out text (#1650)
 - Bug / Unwanted draft (#698)
 - All users seems to be able to see the enable encryption option in room settings (#1341)
 - Leave room only leaves the current version (#1656)
 - Regression |  Share action menu do not work (#1647)
 - verification issues on transition (#1555)
 - Fix issue when restoring keys backup using recovery key

SDK API changes ⚠️:
 - CreateRoomParams has been updated

Build 🧱:
 - Upgrade some dependencies
 - Revert to build-tools 3.5.3

Other changes:
 - Use Intent.ACTION_CREATE_DOCUMENT to save megolm key or recovery key in a txt file
 - Use `Context#withStyledAttributes` extension function (#1546)

Changes in Riot.imX 0.91.4 (2020-07-06)
===================================================

Features ✨:
 - Re-activate Wellknown support with updated UI (#1614)

Improvements 🙌:
 - Upload device keys only once to the homeserver and fix crash when no network (#1629)

Bugfix 🐛:
 - Fix crash when coming from a notification (#1601)
 - Fix Exception when importing keys (#1576)
 - File isn't downloaded when another file with the same name already exists (#1578)
 - saved images don't show up in gallery (#1324)
 - Fix reply fallback leaking sender locale (#429)

Build 🧱:
 - Fix lint false-positive about WorkManager (#1012)
 - Upgrade build-tools from 3.5.3 to 3.6.3
 - Upgrade gradle from 5.4.1 to 5.6.4

Changes in Riot.imX 0.91.3 (2020-07-01)
===================================================

Notes:
 - This version is the third beta version of RiotX codebase published as Riot-Android on the PlayStore.
 - Changelog below includes changes of v0.91.0, v0.91.1, and v0.91.2, because the first beta versions have been tagged and
 published from the branch feature/migration_from_legacy.
 - This version uses temporary name `Riot.imX`, to distinguish the app with RiotX app.

Features ✨:
 - Call with WebRTC support (##611)
 - Add capability to change the display name (#1529)

Improvements 🙌:
 - "Add Matrix app" menu is now always visible (#1495)
 - Handle `/op`, `/deop`, and `/nick` commands (#12)
 - Prioritising Recovery key over Recovery passphrase (#1463)
 - Room Settings: Name, Topic, Photo, Aliases, History Visibility (#1455)
 - Update user avatar (#1054)
 - Allow self-signed certificate (#1564)
 - Improve file download and open in timeline
 - Catchup tab is removed temporarily (#1565)
 - Render room avatar change (#1319)

Bugfix 🐛:
 - Fix dark theme issue on login screen (#1097)
 - Incomplete predicate in RealmCryptoStore#getOutgoingRoomKeyRequest (#1519)
 - User could not redact message that they have sent (#1543)
 - Use vendor prefix for non merged MSC (#1537)
 - Compress images before sending (#1333)
 - Searching by displayname is case sensitive (#1468)
 - Fix layout overlap issue (#1407)

Build 🧱:
 - Enable code optimization (Proguard)
 - SDK is now API level 21 minimum, and so RiotX (#405)

Other changes:
 - Use `SharedPreferences#edit` extension function consistently (#1545)
 - Use `retrofit2.Call.awaitResponse` extension provided by Retrofit 2. (#1526)
 - Fix minor typo in contribution guide (#1512)
 - Fix self-assignment of callback in `DefaultRoomPushRuleService#setRoomNotificationState` (#1520)
 - Random housekeeping clean-ups indicated by Lint (#1520, #1541)
 - Keys Backup API now use the unstable prefix (#1503)
 - Remove deviceId from /keys/upload/{deviceId} as not spec-compliant (#1502)

Changes in RiotX 0.22.0 (2020-06-15)
===================================================

Features ✨:
 - Integration Manager and Widget support (#48)
 - Send stickers (#51)

Improvements 🙌:
 - New wording for notice when current user is the sender
 - Hide "X made no changes" event by default in timeline (#1430)
 - Hide left rooms in breadcrumbs (#766)
 - Handle PowerLevel properly (#627)
 - Correctly handle SSO login redirection
 - SSO login is now performed in the default browser, or in Chrome Custom tab if available (#1400)
 - Improve checking of homeserver version support (#1442)
 - Add capability to add and remove a room from the favorites (#1217)

Bugfix 🐛:
 - Switch theme is not fully taken into account without restarting the app
 - Temporary fix to show error when user is creating an account on matrix.org with userId containing only digits (#1410)
 - Reply composer overlay stays on screen too long after send (#1169)
 - Fix navigation bar icon contrast on API in [21,27[ (#1342)
 - Fix status bar icon contrast on API in [21,23[
 - Wrong /query request (#1444)
 - Make Credentials.homeServer optional because it is deprecated (#1443)
 - Fix issue on dark themes, after alert popup dismiss

Other changes:
 - Send plain text in the body of events containing formatted body, as per https://matrix.org/docs/spec/client_server/latest#m-room-message-msgtypes
 - Update link to Modular url from "https://modular.im/" to "https://modular.im/services/matrix-hosting-riot" and open it using ChromeCustomTab

Changes in RiotX 0.21.0 (2020-05-28)
===================================================

Features ✨:
 - Identity server support (#607)
 - Switch language support (#41)
 - Display list of attachments of a room (#860)

Improvements 🙌:
 - Better connectivity lost indicator when airplane mode is on
 - Add a setting to hide redacted events (#951)
 - Render formatted_body for m.notice and m.emote (#1196)
 - Change icon to magnifying-glass to filter room (#1384)

Bugfix 🐛:
 - After jump to unread, newer messages are never loaded (#1008)
 - Fix issues with FontScale switch (#69, #645)
 - "Seen by" uses 12h time (#1378)
 - Enable markdown (if active) when sending emote (#734)
 - Screenshots for Rageshake now includes Dialogs such as BottomSheet (#1349)

SDK API changes ⚠️:
 - initialize with proxy configuration

Other changes:
 - support new key agreement method for SAS (#1374)

Changes in RiotX 0.20.0 (2020-05-15)
===================================================

Features ✨:
 - Add Direct Shortcuts (#652)

Improvements 🙌:
 - Invite member(s) to an existing room (#1276)
 - Improve notification accessibility with ticker text (#1226)
 - Support homeserver discovery from MXID (DISABLED: waiting for design) (#476)

Bugfix 🐛:
 - Fix | Verify Manually by Text crashes if private SSK not known (#1337)
 - Sometimes the same device appears twice in the list of devices of a user (#1329)
 - Random Crashes while doing sth with cross signing keys (#1364)
 - Crash | crash while restoring key backup (#1366)

SDK API changes ⚠️:
 - excludedUserIds parameter added to the UserService.getPagedUsersLive() function

Changes in RiotX 0.19.0 (2020-05-04)
===================================================

Features ✨:
 - Change password (#528)
 - Cross-Signing | Support SSSS secret sharing (#944)
 - Cross-Signing | Verify new session from existing session (#1134)
 - Cross-Signing | Bootstraping cross signing with 4S from mobile (#985)
 - Save media files to Gallery (#973)
 - Account deactivation (with password only) (#35)

Improvements 🙌:
 - Verification DM / Handle concurrent .start after .ready (#794)
 - Reimplementation of multiple attachment picker
 - Cross-Signing | Update Shield Logic for DM (#963)
 - Cross-Signing | Complete security new session design update (#1135)
 - Cross-Signing | Setup key backup as part of SSSS bootstrapping (#1201)
 - Cross-Signing | Gossip key backup recovery key (#1200)
 - Show room encryption status as a bubble tile (#1078)
 - UX/UI | Add indicator to home tab on invite (#957)
 - Cross-Signing | Restore history after recover from passphrase (#1214)
 - Cross-Sign | QR code scan confirmation screens design update (#1187)
 - Emoji Verification | It's not the same butterfly! (#1220)
 - Cross-Signing | Composer decoration: shields (#1077)
 - Cross-Signing | Migrate existing keybackup to cross signing with 4S from mobile (#1197)
 - Show a warning dialog if the text of the clicked link does not match the link target (#922)
 - Cross-Signing | Consider not using a spinner on the 'complete security' prompt (#1271)
 - Restart broken Olm sessions ([MSC1719](https://github.com/matrix-org/matrix-doc/pull/1719))
 - Cross-Signing | Hide Use recovery key when 4S is not setup (#1007)
 - Cross-Signing | Trust account xSigning keys by entering Recovery Key (select file or copy) #1199
 - E2E timeline decoration (#1279)
 - Manage Session Settings / Cross Signing update (#1295)
 - Cross-Signing | Review sessions toast update old vs new (#1293, #1306)

Bugfix 🐛:
 - Fix summary notification staying after "mark as read"
 - Missing avatar/displayname after verification request message (#841)
 - Crypto | RiotX sometimes rotate the current device keys (#1170)
 - RiotX can't restore cross signing keys saved by web in SSSS (#1174)
 - Cross- Signing | After signin in new session, verification paper trail in DM is off (#1191)
 - Failed to encrypt message in room (message stays in red), [thanks to pwr22] (#925)
 - Cross-Signing | web <-> riotX After QR code scan, gossiping fails (#1210)
 - Fix crash when trying to download file without internet connection (#1229)
 - Local echo are not updated in timeline (for failed & encrypted states)
 - Render image event even if thumbnail_info does not have mimetype defined (#1209)
 - RiotX now uses as many threads as it needs to do work and send messages (#1221)
 - Fix issue with media path (#1227)
 - Add user to direct chat by user id (#1065)
 - Use correct URL for SSO connection (#1178)
 - Emoji completion :tada: does not completes to 🎉 like on web (#1285)
 - Fix bad Shield Logic for DM (#963)

Translations 🗣:
 - Weblate now create PR directly to RiotX GitHub project

SDK API changes ⚠️:
 - Increase targetSdkVersion to 29

Build 🧱:
 - Compile with Android SDK 29 (Android Q)

Other changes:
 - Add a setting to prevent screenshots of the application, disabled by default (#1027)
 - Increase File Logger capacities ( + use dev log preferences)

Changes in RiotX 0.18.1 (2020-03-17)
===================================================

Improvements 🙌:
 - Implementation of /join command

Bugfix 🐛:
 - Message transitions in encrypted rooms are jarring #518
 - Images that failed to send are waiting to be sent forever #1145
 - Fix / Crashed when trying to send a gif from the Gboard #1136
 - Fix / Cannot click on key backup banner when new keys are available


Changes in RiotX 0.18.0 (2020-03-11)
===================================================

Improvements 🙌:
 - Share image and other media from e2e rooms (#677)
 - Add support for `/plain` command (#12)
 - Detect spaces in password if user fail to login (#1038)
 - FTUE: do not display a different color when encrypting message when not in developer mode.
 - Open room member profile from avatar of the room member state event (#935)
 - Restore the push rules configuration in the settings

Bugfix 🐛:
 - Fix crash on attachment preview screen (#1088)
 - "Share" option is not appearing in encrypted rooms for images (#1031)
 - Set "image/jpeg" as MIME type of images instead of "image/jpg" (#1075)
 - Self verification via QR code is failing (#1130)

SDK API changes ⚠️:
 - PushRuleService.getPushRules() now returns a RuleSet. Use getAllRules() on this object to get all the rules.

Build 🧱:
 - Upgrade ktlint to version 0.36.0
 - Pipeline file for Buildkite is now hosted on another Github repository: https://github.com/matrix-org/pipelines/blob/master/riotx-android/pipeline.yml

Other changes:
 - Restore availability to Chromebooks (#932)
 - Add a [documentation](./docs/integration_tests.md) to run integration tests

Changes in RiotX 0.17.0 (2020-02-27)
===================================================

Features ✨:
 - Secured Shared Storage Support (#984, #936)
 - It's now possible to select several rooms (with a possible mix of clear/encrypted rooms) when sharing elements to RiotX (#1010)
 - Media preview: media are previewed before being sent to a room (#1010)
 - Image edition: it's now possible to edit image before sending: crop, rotate, and delete actions are supported (#1010)
 - Sending image: image are sent to rooms with a reduced size. It's still possible to send original image file (#1010)

Improvements 🙌:
 - Migrate to binary QR code verification (#994)
 - Share action is added to room profile and room member profile (#858)
 - Display avatar in fullscreen (#861)
 - Fix some performance issues with crypto

Bugfix 🐛:
 - Account creation: wrongly hints that an email can be used to create an account (#941)
 - Fix crash in the room directory, when public room has no name (#1023)
 - Fix restoring keys backup with passphrase (#526)
 - Fix rotation of full-size image (#647)
 - Fix joining rooms from directory via federation isn't working. (#808)
 - Leaving a room creates a stuck "leaving room" loading screen. (#1041)
 - Fix some invitation handling issues (#1013)
 - New direct chat: selecting a participant sometimes results in two breadcrumbs (#1022)
 - New direct chat: selecting several participants was not adding the room to the direct chats list
 - Room overview shows deleted messages as “Encrypted message” (#758)

SDK API changes ⚠️:
 - Get crypto methods through Session.cryptoService()
 - ProgressListener.onProgress() function will be invoked on the background thread instead of UI thread
 - Improve CreateRoomParams API (#1070)

Changes in RiotX 0.16.0 (2020-02-14)
===================================================

Features ✨:
 - Polls and Bot Buttons (MSC 2192 matrix-org/matrix-doc#2192)

Improvements 🙌:
 - Show confirmation dialog before deleting a message (#967, #1003)
 - Open room member profile from reactions list and read receipts list (#875)

Bugfix 🐛:
 - Fix crash by removing all notifications after clearing cache (#878)
 - Fix issue with verification when other client declares it can only show QR code (#988)
 - Fix too errors in the code (1941862499c9ec5268cc80882512ced379cafcfd, a250a895fe0a4acf08c671e03434edcd29ccd84f)

SDK API changes ⚠️:
 - Javadoc improved for PushersService
 - PushersService.pushers() has been renamed to PushersService.getPushers()

Changes in RiotX 0.15.0 (2020-02-10)
===================================================

Improvements 🙌:
 - Improve navigation to the timeline (#789, #862)
 - Improve network detection. It is now based on the sync request status (#873, #882)

Other changes:
 - Support SSO login with Firefox account (#606)

Bugfix 🐛:
 - Ask for permission before opening the camera (#934)
 - Encrypt for invited users by default, if the room state allows it (#803)

Changes in RiotX 0.14.3 (2020-02-03)
===================================================

Bugfix 🐛:
 - Fix Exception in DeviceListManager

Changes in RiotX 0.14.2 (2020-02-02)
===================================================

Bugfix 🐛:
 - Fix RiotX not starting issue

Changes in RiotX 0.14.1 (2020-02-02)
===================================================

Bugfix 🐛:
 - Cross-signing: fix UX issue when closing the bottom sheet verification (#813)
 - Room and room member profile: fix issues on dark and black themes

Changes in RiotX 0.14.0 (2020-02-01)
===================================================

Features ✨:
 - First implementation of Cross-signing
 - Enable encryption in unencrypted rooms, from the room settings (#212)
 - Negotiate E2E by default for DMs (#907)

Improvements 🙌:
 - Sharing things to RiotX: sort list by recent room first (#771)
 - Hide the algorithm when turning on e2e (#897)
 - Sort room members by display names

Other changes:
 - Add support for /rainbow and /rainbowme commands (#879)

Build 🧱:
 - Ensure builds are reproducible (#842)
 - F-Droid: fix the "-dev" issue in version name (#815)

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
 - [SDK] MSC2241 / verification in DMs (#707)

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
 - KeyBackup / SSSS | Should get the key from SSSS instead of asking recovery Key (#1163)

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
