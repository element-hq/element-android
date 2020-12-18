# Adding and removing ThreePids to an account

## Add email

### User enter the email

> POST https://homeserver.org/_matrix/client/r0/account/3pid/email/requestToken

```json
{
  "email": "alice@email-provider.org",
  "client_secret": "TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh",
  "send_attempt": 1
}
```

#### The email is already added to an account

400

```json
{
  "errcode": "M_THREEPID_IN_USE",
  "error": "Email is already in use"
}
```

#### The email is free

Wording: "We've sent you an email to verify your address. Please follow the instructions there and then click the button below."

200

```json
{
  "sid": "bxyDHuJKsdkjMlTJ"
}
```

## User receive an e-mail

> [homeserver.org] Validate your email
>
> A request to add an email address to your Matrix account has been received. If this was you, please click the link below to confirm adding this email:
  https://homeserver.org/_matrix/client/unstable/add_threepid/email/submit_token?token=WUnEhQAmJrXupdEbXgdWvnVIKaGYZFsU&client_secret=TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh&sid=bxyDHuJKsdkjMlTJ
>  
>  If this was not you, you can safely ignore this email. Thank you.

### User clicks on the link

The browser displays the following message:

> Your email has now been validated, please return to your client. You may now close this window.

### User returns on Element

User clicks on CONTINUE

> POST https://homeserver.org/_matrix/client/r0/account/3pid/add

```json
{
  "sid": "bxyDHuJKsdkjMlTJ",
  "client_secret": "TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh"
}
```

401 User Interactive Authentication

```json
{
  "session": "ppvvnozXCQZFaggUBlHJYPjA",
  "flows": [
    {
      "stages": [
        "m.login.password"
      ]
    }
  ],
  "params": {
  }
}
```

### User enters his password

POST https://homeserver.org/_matrix/client/r0/account/3pid/add

```json
{
  "sid": "bxyDHuJKsdkjMlTJ",
  "client_secret": "TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh",
  "auth": {
    "session": "ppvvnozXCQZFaggUBlHJYPjA",
    "type": "m.login.password",
    "user": "@benoitx:matrix.org",
    "password": "weak_password"
  }
}
```

#### The link has not been clicked

400

```json
{
  "errcode": "M_THREEPID_AUTH_FAILED",
  "error": "No validated 3pid session found"
}
```

#### Wrong password

401

```json
{
  "session": "fXHOvoQsPMhEebVqTnIrzZJN",
  "flows": [
    {
      "stages": [
        "m.login.password"
      ]
    }
  ],
  "params": {
  },
  "completed":[
  ],
  "error": "Invalid password",
  "errcode": "M_FORBIDDEN"
}
```

#### The link has been clicked and the account password is correct

200

```json
{}
```

## Remove email

### User want to remove an email from his account

> POST https://homeserver.org/_matrix/client/r0/account/3pid/delete

```json
{
  "medium": "email",
  "address": "alice@email-provider.org"
}
```

#### Email was not bound to an identity server

200

```json
{
  "id_server_unbind_result": "no-support"
}
```

#### Email was bound to an identity server

200

```json
{
  "id_server_unbind_result": "success"
}
```

## Add phone number

> POST https://homeserver.org/_matrix/client/r0/account/3pid/msisdn/requestToken

```json
{
  "country": "FR",
  "phone_number": "611223344",
  "client_secret": "f1K29wFZBEr4RZYatu7xj8nEbXiVpr7J",
  "send_attempt": 1
}
```

Note that the phone number is sent without `+` and without the country code

#### The phone number is already added to an account

400

```json
{
  "errcode": "M_THREEPID_IN_USE",
  "error": "MSISDN is already in use"
}
```

#### The phone number is free

Wording: "A text message has been sent to +33611223344. Please enter the verification code it contains."

200

```json
{
  "msisdn": "33651547677",
  "intl_fmt": "+33 6 51 54 76 77",
  "success": true,
  "sid": "253299954",
  "submit_url": "https://homeserver.org/_matrix/client/unstable/add_threepid/msisdn/submit_token"
}
```

## User receive a text message

> Riot

> Your Riot validation code is 892541, please enter this into the app

### User enter the code to the app

#### Wrong code

> POST https://homeserver.org/_matrix/client/unstable/add_threepid/msisdn/submit_token

```json
{
  "sid": "253299954",
  "client_secret": "f1K29wFZBEr4RZYatu7xj8nEbXiVpr7J",
  "token": "111111"
}
```

400

```json
{
  "errcode": "M_UNKNOWN",
  "error": "Error contacting the identity server"
}
```

This is not an ideal, but the client will display a hint to check the entered code to the user.

#### Correct code

> POST https://homeserver.org/_matrix/client/unstable/add_threepid/msisdn/submit_token

```json
{
  "sid": "253299954",
  "client_secret": "f1K29wFZBEr4RZYatu7xj8nEbXiVpr7J",
  "token": "892541"
}
```

200

```json
{
  "success": true
}
```

Then the app call `https://homeserver.org/_matrix/client/r0/account/3pid/add` as per adding an email and follow the same UIS flow

## Remove phone number

### User wants to remove a phone number from his account

This is the same request and response than to remove email, but with this body:

```json
{
  "medium": "msisdn",
  "address": "33611223344"
}
```

Note that the phone number is provided without `+`, but with the country code.
