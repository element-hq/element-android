# Sign up to a homeserver

This document describes the flow of registration to a homeserver. Examples come from the matrix.org homeserver, and the logs come from Riot-Android.

Note that it contains bugs:
 - "password" and "initial_device_display_name" values are sent a bit too much
 - the first received "sessionId" is not reused
 - The order of stages returned by the homeserver is not strictly followed

Ref: https://matrix.org/docs/spec/client_server/latest#account-registration-and-management

## Sign up flows

### First step

Client request the sign-up flows, once the homeserver is chosen by the user and its url is knwon (in the example it's https://matrix.org)

> curl -X POST --data $'{"initial_device_display_name":"Mobile device","x_show_msisdn":true}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "initial_device_display_name": "Mobile device",
  "x_show_msisdn": true
}
```

401

```json
{
  "session": "vwehdKMtkRedactedAMwgCACZ",
  "flows": [
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.dummy"
      ]
    },
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "params": {
    "m.login.recaptcha": {
      "public_key": "6LcgI54UAAAAAoREDACTEDoDdOocFpYVdjYBRe4zb"
    },
    "m.login.terms": {
      "policies": {
        "privacy_policy": {
          "version": "1.0",
          "en": {
            "name": "Terms and Conditions",
            "url": "https:\/\/matrix.org\/_matrix\/consent?v=1.0"
          }
        }
      }
    }
  }
}
```

### Step 1: entering user name and password

The app is displaying a form with login and password. Only the login is sent for the first request

> curl -X POST --data $'{"initial_device_display_name":"Mobile device","username":"alice"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "initial_device_display_name": "Mobile device",
  "username": "alice"
}
```

401

```json
{
  "session": "xptUYoREDACTEDogOWAGVnbJQ",
  "flows": [
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.dummy"
      ]
    },
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "params": {
    "m.login.recaptcha": {
      "public_key": "6LcgI54UAAAAAoREDACTEDoDdOocFpYVdjYBRe4zb"
    },
    "m.login.terms": {
      "policies": {
        "privacy_policy": {
          "version": "1.0",
          "en": {
            "name": "Terms and Conditions",
            "url": "https:\/\/matrix.org\/_matrix\/consent?v=1.0"
          }
        }
      }
    }
  }
}
```

#### If username already exists

```json
{
  "errcode": "M_USER_IN_USE",
  "error": "User ID already taken."
}
```

### Step 2: entering email

User is proposed to enter an email. We skip this step.

> curl -X POST --data $'{"auth":{"session":"xptUYoREDACTEDogOWAGVnbJQ","type":"m.login.dummy"},"initial_device_display_name":"Mobile device","password":"azerty","username":"alice"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "auth": {
    "session": "xptUYoREDACTEDogOWAGVnbJQ",
    "type": "m.login.dummy"
  },
  "initial_device_display_name": "Mobile device",
  "password": "password_REDACTED",
  "username":"alice"
}
```

401

```json
{
  "session": "xptUYoREDACTEDogOWAGVnbJQ",
  "flows": [
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.dummy"
      ]
    },
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "params": {
    "m.login.recaptcha": {
      "public_key": "6LcgI54UAAAAAoREDACTEDoDdOocFpYVdjYBRe4zb"
    },
    "m.login.terms": {
      "policies": {
        "privacy_policy": {
          "version": "1.0",
          "en": {
            "name": "Terms and Conditions",
            "url": "https:\/\/matrix.org\/_matrix\/consent?v=1.0"
          }
        }
      }
    }
  },
  "completed": [
    "m.login.dummy"
  ]
}
```

### Step 2 bis: we enter an email

> curl -X POST --data $'{"client_secret":"53e679ea-oRED-ACTED-92b8-3012c49c6cfa","email":"alice@yopmail.com","send_attempt":0}' 'https://matrix.org/_matrix/client/r0/register/email/requestToken'

```json
{
  "client_secret": "53e679ea-oRED-ACTED-92b8-3012c49c6cfa",
  "email": "alice@yopmail.com",
  "send_attempt": 0
}
```

200

```json
{
  "sid": "qlBCREDACTEDEtgxD"
}
```

And

> curl -X POST --data $'{"auth":{"threepid_creds":{"client_secret":"53e679ea-oRED-ACTED-92b8-3012c49c6cfa","sid":"qlBCREDACTEDEtgxD"},"session":"xptUYoREDACTEDogOWAGVnbJQ","type":"m.login.email.identity"},"initial_device_display_name":"Mobile device","password":"password_REDACTED","username":"alice"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "auth": {
    "threepid_creds": {
      "client_secret": "53e679ea-oRED-ACTED-92b8-3012c49c6cfa",
      "sid": "qlBCREDACTEDEtgxD"
    },
    "session": "xptUYoREDACTEDogOWAGVnbJQ",
    "type": "m.login.email.identity"
  },
  "initial_device_display_name": "Mobile device",
  "password": "password_REDACTED",
  "username": "alice"
}
```

401

```json
{
  "errcode": "M_UNAUTHORIZED",
  "error": ""
}
```

The app is now polling on 

> curl -X POST --data $'{"auth":{"threepid_creds":{"client_secret":"53e679ea-oRED-ACTED-92b8-3012c49c6cfa","sid":"qlBCREDACTEDEtgxD"},"session":"xptUYoREDACTEDogOWAGVnbJQ","type":"m.login.email.identity"},"initial_device_display_name":"Mobile device","password":"password_REDACTED","username":"alice"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "auth": {
    "threepid_creds": {
      "client_secret": "53e679ea-oRED-ACTED-92b8-3012c49c6cfa",
      "sid": "qlBCREDACTEDEtgxD"
    },
    "session": "xptUYoREDACTEDogOWAGVnbJQ",
    "type": "m.login.email.identity"
  },
  "initial_device_display_name": "Mobile device",
  "password": "password_REDACTED",
  "username": "alice"
}
```

We click on the link received by email https://matrix.org/_matrix/client/unstable/registration/email/submit_token?token=vtQjQIZfwdoREDACTEDozrmKYSWlCXsJ&client_secret=53e679ea-oRED-ACTED-92b8-3012c49c6cfa&sid=qlBCREDACTEDEtgxD which contains:
- A token vtQjQIZfwdoREDACTEDozrmKYSWlCXsJ
- a client secret: 53e679ea-oRED-ACTED-92b8-3012c49c6cfa
- A sid: qlBCREDACTEDEtgxD

Once the link is clicked, the registration request (polling) returns a 401 with the following content:

```json
{
  "session": "xptUYoREDACTEDogOWAGVnbJQ",
  "flows": [
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.dummy"
      ]
    },
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "params": {
    "m.login.recaptcha": {
      "public_key": "6LcgI54UAAAAAoREDACTEDoDdOocFpYVdjYBRe4zb"
    },
    "m.login.terms": {
      "policies": {
        "privacy_policy": {
          "version": "1.0",
          "en": {
            "name": "Terms and Conditions",
            "url": "https:\/\/matrix.org\/_matrix\/consent?v=1.0"
          }
        }
      }
    }
  },
  "completed": [
    "m.login.email.identity"
  ]
}
```

### Step 3: Accepting T&C

User is proposed to accept T&C and he accepts them

> curl -X POST --data $'{"auth":{"session":"xptUYoREDACTEDogOWAGVnbJQ","type":"m.login.terms"},"initial_device_display_name":"Mobile device"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "auth": {
    "session": "xptUYoREDACTEDogOWAGVnbJQ",
    "type": "m.login.terms"
  },
  "initial_device_display_name": "Mobile device"
}
```

401

```json
{
  "session": "xptUYoREDACTEDogOWAGVnbJQ",
  "flows": [
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.dummy"
      ]
    },
    {
      "stages": [
        "m.login.recaptcha",
        "m.login.terms",
        "m.login.email.identity"
      ]
    }
  ],
  "params": {
    "m.login.recaptcha": {
      "public_key": "6LcgI54UAAAAAoREDACTEDoDdOocFpYVdjYBRe4zb"
    },
    "m.login.terms": {
      "policies": {
        "privacy_policy": {
          "version": "1.0",
          "en": {
            "name": "Terms and Conditions",
            "url": "https:\/\/matrix.org\/_matrix\/consent?v=1.0"
          }
        }
      }
    }
  },
  "completed": [
    "m.login.dummy",
    "m.login.terms"
  ]
}
```

### Step 4: Captcha

User is proposed to prove he is not a robot and he does it:

> curl -X POST --data $'{"auth":{"response":"03AOLTBLSiGS9GhFDpAMblJ2nlXOmHXqAYJ5OvHCPUjiVLBef3k9snOYI_BDC32-t4D2jv-tpvkaiEI_uloobFd9RUTPpJ7con2hMddbKjSCYqXqcUQFhzhbcX6kw8uBnh2sbwBe80_ihrHGXEoACXQkL0ki1Q0uEtOeW20YBRjbNABsZPpLNZhGIWC0QVXnQ4FouAtZrl3gOAiyM-oG3cgP6M9pcANIAC_7T2P2amAHbtsTlSR9CsazNyS-rtDR9b5MywdtnWN9Aw8fTJb8cXQk_j7nvugMxzofPjSOrPKcr8h5OqPlpUCyxxnFtag6cuaPSUwh43D2L0E-ZX7djzaY2Yh_U2n6HegFNPOQ22CJmfrKwDlodmAfMPvAXyq77n3HpoREDACTEDo3830RHF4BfkGXUaZjctgg-A1mvC17hmQmQpkG7IhDqyw0onU-0vF_-ehCjq_CcQEDpS_O3uiHJaG5xGf-0rhLm57v_wA3deugbsZuO4uTuxZZycN_mKxZ97jlDVBetl9hc_5REPbhcT1w3uzTCSx7Q","session":"iLHmdwNlXZoREDACTEDoouwMi","type":"m.login.recaptcha"},"initial_device_display_name":"Mobile device"}' 'https://matrix.org/_matrix/client/r0/register'

```json
{
  "auth": {
    "response": "03AOLTBLSiGS9GhFDpAMblJ2nlXOmHXqAYJ5OvHCPUjiVLBef3k9snOYI_BDC32-t4D2jv-tpvkaiEI_uloobFd9RUTPpJ7con2hMddbKjSCYqXqcUQFhzhbcX6kw8uBnh2sbwBe80_ihrHGXEoACXQkL0ki1Q0uEtOeW20YBRjbNABsZPpLNZhGIWC0QVXnQ4FouAtZrl3gOAiyM-oG3cgP6M9pcANIAC_7T2P2amAHbtsTlSR9CsazNyS-rtDR9b5MywdtnWN9Aw8fTJb8cXQk_j7nvugMxzofPjSOrPKcr8h5OqPlpUCyxxnFtag6cuaPSUwh43D2L0E-ZX7djzaY2Yh_U2n6HegFNPOQ22CJmfrKwDlodmAfMPvAXyq77n3HpoREDACTEDo3830RHF4BfkGXUaZjctgg-A1mvC17hmQmQpkG7IhDqyw0onU-0vF_-ehCjq_CcQEDpS_O3uiHJaG5xGf-0rhLm57v_wA3deugbsZuO4uTuxZZycN_mKxZ97jlDVBetl9hc_5REPbhcT1w3uzTCSx7Q",
    "session": "iLHmdwNlXZoREDACTEDoouwMi",
    "type": "m.login.recaptcha"
  },
  "initial_device_display_name": "Mobile device"
}
```

200

```json
{
  "user_id": "@alice:matrix.org",
  "home_server": "matrix.org",
  "access_token": "MDAxOGxvY2F0aW9uIG1hdHJpeC5vcmcKMoREDACTEDo50aWZpZXIga2V5CjAwMTBjaWQgZ2VuID0gMQowMDI5Y2lkIHVzZXJfaWQgPSBAYmVub2l0eHh4eDptYXRoREDACTEDoCjAwMTZjaWQgdHlwZSA9IGFjY2VzcwowMDIxY2lkIG5vbmNlID0gNHVSVm00aVFDaWlKdoREDACTEDoJmc2lnbmF0dXJlIOmHnTLRfxiPjhrWhS-dThUX-qAzZktfRThzH1YyAsxaCg",
  "device_id": "FLBAREDAJZ"
}
```

The account is created!
