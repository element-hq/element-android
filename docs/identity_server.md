# Identity server

Issue: #607
PR: #1354

## Introduction
Identity servers support contact discovery on Matrix by letting people look up Third Party Identifiers to see if the owner has publicly linked them with their Matrix ID.

## Implementation

The current implementation was Inspired by the code from Riot-Android.

Difference though (list not exhaustive):
- Only API v2 is supported (see https://matrix.org/docs/spec/identity_service/latest)
- Homeserver has to be up to date to support binding (Versions.isLoginAndRegistrationSupportedBySdk() has to return true)
- The SDK managed the session and client secret when binding ThreePid. Those data are not exposed to the client.
- The SDK supports incremental sendAttempt (this is not used by Element)
- The "Continue" button is now under the information, and not as the same place that the checkbox
- The app can cancel a binding. Current data are erased from DB.
- The API (IdentityService) is improved.
- A new DB to store data related to the identity server management.

Missing features (list not exhaustive):
- Invite by 3Pid (will be in a dedicated PR)
- Add email or phone to account (not P1, can be done on Element-Web)
- List email and phone of the account (could be done in a dedicated PR)
- Search contact (not P1)
- Logout from identity server when user sign out or deactivate his account.

## Related MSCs
The list can be found here: https://matrix.org/blog/2019/09/27/privacy-improvements-in-synapse-1-4-and-riot-1-4

## Steps and requirements

- Only one identity server by account can be set. The user's choice is stored in account data with key `m.identity_server`. But every clients will managed its own token to log in to the identity server
```json
{
  "type": "m.identity_server",
  "content": {
    "base_url": "https://matrix.org"
  }
}
```
- The accepted terms are stored in the account data:
```json
{
  "type": "m.accepted_terms",
  "content": {
    "accepted": [
      "https://vector.im/identity-server-privacy-notice-1"
    ]
  }
}
```

- Default identity server URL, from Wellknown data is proposed to the user.
- Identity server can be set
- Identity server can be changed on another user's device, so when the change is detected (thanks to account data sync) Element should properly disconnect from a previous identity server (I think it was not the case in Riot-Android, where we keep the token forever)
- Registration to the identity server is managed with an openId token
- Terms of service can be accepted when configuring the identity server.
- Terms of service can be accepted after, if they change.
- Identity server can be modified
- Identity server can be disconnected with a warning dialog, with special content if there are current bound 3pid on this identity server.
- Email can be bound
- Email can be unbound
- Phone can be bound
- Phone can be unbound
- Look up can be performed, to get matrixIds from local contact book (phone and email): Android permission correctly handled (not done yet)
- Look up pepper can be updated if it is rotated on the identity server
- Invitation using 3PID can be done (See #548) (not done yet)
- Homeserver access-token will never be sent to an identity server
- When user sign-out: logout from the identity server if any.
- When user deactivate account: logout from the identity server if any.

## Screens

### Settings

Identity server settings can be accessed from the internal setting of the application, both from "Discovery" section and from identity detail section.

### Discovery screen

This screen displays the identity server configuration and the binding of the user's ThreePid (email and msisdn). This is the main screen of the feature.

### Set identity server screen

This screen is a form to set a new identity server URL

## Ref:
- https://matrix.org/blog/2019/09/27/privacy-improvements-in-synapse-1-4-and-riot-1-4 is a good summary of the role of an identity server and the proper way to configure and use it in respect to the privacy and the consent of the user.
- API documentation: https://matrix.org/docs/spec/identity_service/latest
- vector.im TOS: https://vector.im/identity-server-privacy-notice
