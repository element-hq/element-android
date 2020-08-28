# Adding an email to an account

## User enter the email

> POST https://homeserver.org/_matrix/client/r0/account/3pid/email/requestToken

```json
{
  "email": "alice@email-provider.org",
  "client_secret": "TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh",
  "send_attempt": 1
}
```

### The email is already adding to an account

400

```json
{
  "errcode": "M_THREEPID_IN_USE",
  "error": "Email is already in use"
}
```

### The email is free

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

## User clicks on the link

The browser displays the following message:

> Your email has now been validated, please return to your client. You may now close this window.

## User returns on Element

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

## User enters his password

POST https://homeserver.org/_matrix/client/r0/account/3pid/add

```json
{
  "sid": "bxyDHuJKsdkjMlTJ",
  "client_secret": "TixzvOnw7nLEUdiQEmkHzkXKrY4HhiGh",
  "auth": {
    "session": "ppvvnozXCQZFaggUBlHJYPjA",
    "type": "m.login.password",
    "user": "@benoitx:matrix.org",
    "identifier": {
      "type": "m.id.user",
      "user": "@benoitx:matrix.org"
    },
    "password": "weak_password"
  }
}
```

### The link has not been clicked

400

```json
{
  "errcode": "M_THREEPID_AUTH_FAILED",
  "error": "No validated 3pid session found"
}
```

### Wrong password

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

### The link has been clicked and the account password is correct

200

```json
{}
```

# Remove email

## User want to remove the email from his account

> POST https://homeserver.org/_matrix/client/r0/account/3pid/delete

```json
{
  "medium": "email",
  "address": "alice@email-provider.org"
}
```

### Email was not bound to an identity server

200

```json
{
  "id_server_unbind_result": "no-support"
}
```

### Email was bound to an identity server

200

```json
{
  "id_server_unbind_result": "success"
}
```
