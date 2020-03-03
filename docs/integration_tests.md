# Integration tests

Integration tests are useful to ensure that the code works well for any use cases.

They can also be used as sample on how to use the Matrix SDK.

In a ideal world, every API of the SDK should be covered by integration tests. For the moment, we have test mainly for the Crypto part, which is the tricky part. But it covers quite a lot of features: accounts creation, login to existing account, send encrypted messages, keys backup, verification, etc.

The Matrix SDK is able to open multiple sessions, for the same user, of for different users. This way we can test communication between several sessions on a single device.

## Prerequirments

Integration tests need a homeserver running on localhost.

The documentation describe what we do to have one, using [Synapse](https://github.com/matrix-org/synapse/), which is the Matrix reference homeserver.

## Install and run Synapse

Steps:

- Install virtual env

> python3 -m pip install virtualenv

- clone Synapse repository

> git clone git@github.com:matrix-org/synapse.git

You should have the develop branch cloned by default.

- Run synapse, from the Synapse folder you just cloned

> $ virtualenv -p python3 env
> $ source env/bin/activate
> (env) $ pip install -e .
> (env) $ demo/start.sh --no-rate-limit

Alternatively, to install the current Synapse package (and not using the cloned sources), you can run instead of `pip install -e .`:

> (env) $ pip install matrix-synapse

You should now have 3 running federated Synapse instances 🎉, at http://127.0.0.1:8080/, http://127.0.0.1:8081/ and  http://127.0.0.1:8082/, which should display a "It Works! Synapse is running" message.

## Run the test

Its recommended to run tests using an Android Emulator and not a real device.

You can run all the tests in the `androidTest` folders

## Stop Synapse

To stop Synapse, you can run the following commands:

> (env) $ ./demo/stop.sh

And you can deactivate the virtualenv:

> (env) $ deactivate

## Troubleshoot

You'll need python3 to be able to run synapse

TBC