# Integration tests

<!--- TOC -->

* [Pre requirements](#pre-requirements)
* [Install and run Synapse](#install-and-run-synapse)
* [Run the test](#run-the-test)
* [Stop Synapse](#stop-synapse)
* [Troubleshoot](#troubleshoot)
  * [Android Emulator does cannot reach the homeserver](#android-emulator-does-cannot-reach-the-homeserver)
  * [Tests partially run but some fail with "Unable to contact localhost:8080"](#tests-partially-run-but-some-fail-with-"unable-to-contact-localhost:8080")
  * [virtualenv command fails](#virtualenv-command-fails)

<!--- END -->

Integration tests are useful to ensure that the code works well for any use cases.

They can also be used as sample on how to use the Matrix SDK.

In a ideal world, every API of the SDK should be covered by integration tests. For the moment, we have test mainly for the Crypto part, which is the tricky part. But it covers quite a lot of features: accounts creation, login to existing account, send encrypted messages, keys backup, verification, etc.

The Matrix SDK is able to open multiple sessions, for the same user, of for different users. This way we can test communication between several sessions on a single device.

## Pre requirements

Integration tests need a homeserver running on localhost.

The documentation describes what we do to have one, using [Synapse](https://github.com/matrix-org/synapse/), which is the Matrix reference homeserver.

## Install and run Synapse

Steps:

- Install virtualenv

```bash
python3 -m pip install virtualenv
```

- Clone Synapse repository

```bash
git clone -b develop https://github.com/matrix-org/synapse.git
```
or
```bash
git clone -b develop git@github.com:matrix-org/synapse.git
```

You should have the develop branch cloned by default.

- Run synapse, from the Synapse folder you just cloned

```bash
virtualenv -p python3 env
source env/bin/activate
pip install -e .
demo/start.sh --no-rate-limit

```

Alternatively, to install the latest Synapse release package (and not a cloned branch) you can run the following instead of `git clone` and `pip install -e .`:

```bash
pip install matrix-synapse
```

On your first run, you will want to stop the demo and edit the config to correct the `public_baseurl` to http://10.0.2.2:8080 and restart the server.

You should now have 3 running federated Synapse instances 🎉, at http://127.0.0.1:8080/, http://127.0.0.1:8081/ and  http://127.0.0.1:8082/, which should display a "It Works! Synapse is running" message.

## Run the test

It's recommended to run tests using an Android Emulator and not a real device. First reason for that is that the tests will use http://10.0.2.2:8080 to connect to Synapse, which run locally on your machine.

You can run all the tests in the `androidTest` folders.

It can be done using this command:

```bash
./gradlew vector:connectedAndroidTest matrix-sdk-android:connectedAndroidTest
```

## Stop Synapse

To stop Synapse, you can run the following commands:

```bash
./demo/stop.sh
```

And you can deactivate the virtualenv:

```bash
deactivate
```

## Troubleshoot

You'll need python3 to be able to run synapse

### Android Emulator does cannot reach the homeserver

Try on the Emulator browser to open "http://10.0.2.2:8080". You should see the "Synapse is running" message.

### Tests partially run but some fail with "Unable to contact localhost:8080"

This is because the `public_baseurl` of synapse is not consistent with the endpoint that the tests are connecting to.

Ensure you have the following configuration in `demo/etc/8080.config`.

```
public_baseurl: http://10.0.2.2:8080/
```

After changing this you will need to restart synapse using `demo/stop.sh` and `demo/start.sh` to load the new configuration.

### virtualenv command fails

You can try using
```bash
python3 -m venv env
```
or
```bash
python3 -m virtualenv env
```
instead of
```bash
virtualenv -p python3 env
```
