# Features and changes compared to upstream

Here you can find some extra features and changes compared to Element Android (without guarantees on correctness or up-to-dateness).

- Branding (name, icon, links)
- Optional unified chat list instead of dividing group chats and direct messages
- Additional SchildiChat light, dark and black themes
- Possibility to select themes for both light and dark system mode individually
- Optional chat layouts with message bubbles
- Message count passed to the notification badge (visible next to the launcher icon on recent Android versions)
- More prominent unread counter for chats in the room overview (bigger, different placement, more noticeable color in SchildiChat designs)
- Remember across app restarts which categories in the chat overview are expanded or collapsed
- Setting for room previews: show all events, hide membership changes, hide membership changes and reactions (individual settings for direct chats and groups)
- Optional: let overview categories' unread counters also account for chats with disabled notifications (using a different color to indicate the reduced importance)
- Bigger stickers
- Don't always repeat sender name for multiple stickers by the same sender
- Chat options menu: add entry for member list (to skip the step of clicking on the room name, to enter room settings), and hide the less frequently used invite option (which is still available from the member list)
- Optional simplified mode: hide public room functionality and some encryption details
- Experimental possibility to mark rooms as unread (using [MSC2867](https://github.com/matrix-org/matrix-doc/pull/2867), not implemented by many other clients yet)
- Option to also show URL previews in e2e-encrypted chats (for users who trust their homeserver or who don't care about leaked message content in this case)
- Smaller compose area (as before Element 1.0.12)
- Compose area: emoji button on the left, attachments button on the right (flipped compared to Element, but what most other messengers do, thus more familiar to most users)

- Show a toast instead of a snackbar after copying text, in order to not block the input area right after copying
- Change some default settings (e.g., hide deleted messages by default)
- Disable bug reporting to Element
- Login screen: more prominent login via Matrix-ID, removed login via EMS
- Hide "help" text that tells users to long-press a room, which users reported to be rather confusing than helpful
- Hide trailing newlines in text messages
- More lines of the room topic shown by default (without the need to expand) in the room information screen
- Re-arrange room profile
- Some different icons
- Emoji-only messages with increased size: also for messages that have spaces between emojis
- Also fallback to other user's avatar for 2-person-rooms not marked as DM
- ...
- Sometimes bug fixes for issues in Element, when found during internal testing
- Sometimes additional bugs ;)
