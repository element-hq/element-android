# Understand how your data is used

The Matrix protocol is designed with your privacy and data sovereignty in mind.
Because it is a decentralised, federated service with cryptographically-validated message integrity, there are a few important things to know before you use the Service.

This app can communicate with any matrix homeserver which supports the matrix specification by the homeserver.
The user is free to choose the homeserver and has to accept the privacy policy of this homeserver before using it.


#Federation

Services using the Matrix protocol rely on Matrix homeservers which share user data with the wider ecosystem over federation.

- When you send messages or files in a room, a copy of the data is sent to all participants in the room.
  If these participants are registered on remote homeservers, your username, display name, messages and files may be replicated across each participating homeserver.

- We will forget your copy of your data upon your request. We will also forward your request onto federated homeservers.
  However - these homeservers are outside our span of control, so we cannot guarantee they will forget your data.

- Federated homeservers can be located anywhere in the world, and are subject to local laws and regulations.


# Bridging

Some Matrix rooms are bridged to third-party services, such as IRC networks, twitter or email.
When a room has been bridged, your messages and media may be copied onto the bridged service.

- It may not be technically possible to support your management of your data once it has been copied onto a bridged service.

- Bridged services can be located anywhere in the world, and are subject to local laws and regulations.


# Integration Services (Bots and Widgets)

The homeserver the user is using may provide a range of integrations in the form of Widgets (web applications accessed as part of the Matrix Client webapp) and Bots (automated participants in rooms).
Bots and Widgets have access to the messages and files in rooms in which they participate.


# Forgetting your Data

You can request that we forget your data if you deactivate your account.
Each user in a Matrix conversation receives their own copy of all messages and files in that conversation (similar to email), so we ensure data is forgotten by ensuring that your data is not shared further and is not visible to future users.
Once all users’ copies have been forgotten the messages and files will be deleted from the homeserver database. For full details, please see the [full privacy notice](https://matrix.org/legal/privacy-notice).

If you remove (redact) a message, the message content will no longer be accessible to users.
Redactions only remove message content, your display name and avatar - your username will still be visible. Federated homeservers and some matrix clients may not honour the redaction request.


# Legal Basis for Processing

New Vector processes your data under Legitimate Interest.
This means that we process your data only as necessary to deliver the Service, and in a manner that you understand and expect.

The Legitimate Interest of our Service is the provision of decentralised, openly-federated and (optionally) end-to-end encrypted communication services.
The processing of user data we undertake is necessary to provide the Service.
The nature of the Service and its implementation results in some caveats concerning this processing, particularly in terms of GDPR Article 17 Right to Erasure (Right to be Forgotten).
We believe these caveats are in line with the broader societal interests served by providing the Service.
These caveats are discussed in detail in the full privacy notice, but the most important restriction is that your username will still be publicly associated with rooms in which you have participated even if you deactivate your account and ask us to forget your data.

In situations where the interests of the individual appear to be in conflict with the broader societal interests, we will seek to reconcile those differences in accordance with our policy.

If any of the above are unacceptable to you, **please do not use the Service**.

Please review the [full privacy notice](https://matrix.org/legal/privacy-notice) and [code of conduct](https://matrix.org/legal/code-of-conduct) before using this Service.

Please review the [terms and conditions](https://matrix.org/legal/terms-and-conditions) before using this Service.

You must be at least 16 years old to use this Service.
