# Module matrix-sdk-android

<!-- Note: the line below will appear only when the documentation is generated from Element-Android project, and not when it's generated from the SDK project -->
**Note**: You are viewing the nightly documentation of the Android Matrix SDK library. The documentation of the released library can be found here: [https://matrix-org.github.io/matrix-android-sdk2/](https://matrix-org.github.io/matrix-android-sdk2/)

## Welcome to the matrix-sdk-android documentation!

This pages list the complete API that this SDK is exposing to a client application.

*We are still building the documentation, so everything is not documented yet.*

A few entry points:

- **Matrix**: The app will have to create and manage a Matrix object.
- From this **Matrix** object, you will be able to get various services, including the **AuthenticationService**.
- With this **AuthenticationService** you will be able to get an existing **Session**, or create one using a **LoginWizard** or a **RegistrationWizard**, which will finally give you a **Session**.
- From the **Session**, you will be able to retrieve many Services, including the **RoomService**.
- From the **RoomService**, you will be able to list the rooms, create a **Room**, and get a specific **Room**.
- And from a **Room**, you will be able to do many things, including get a **Timeline**, send messages, etc.

Please read the whole documentation to learn more!
