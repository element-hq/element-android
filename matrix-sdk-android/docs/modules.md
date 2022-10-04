# Module matrix-sdk-android

<!-- Note: the line below will appear only when the documentation is generated from Element-Android project, and not when it's generated from the SDK project -->
**Note**: You are viewing the nightly documentation of the Android Matrix SDK library. The documentation of the released library can be found here: [https://matrix-org.github.io/matrix-android-sdk2/](https://matrix-org.github.io/matrix-android-sdk2/)

## Welcome to the matrix-sdk-android documentation!

This pages list the complete API that this SDK is exposing to a client application.

*We are still building the documentation, so everything is not documented yet.*

A few entry points:

- **[Matrix](org.matrix.android.sdk.api.Matrix)**: The app will have to create and manage a **[Matrix](org.matrix.android.sdk.api.Matrix)** object.
- From this **[Matrix](org.matrix.android.sdk.api.Matrix)** object, you will be able to get various services, including the **[AuthenticationService](org.matrix.android.sdk.api.auth.AuthenticationService)**.
- With this **[AuthenticationService](org.matrix.android.sdk.api.auth.AuthenticationService)** you will be able to get an existing **[Session](org.matrix.android.sdk.api.session.Session)**, or create one using a **[LoginWizard](org.matrix.android.sdk.api.auth.login.LoginWizard)** or a **[RegistrationWizard](org.matrix.android.sdk.api.auth.registration.RegistrationWizard)**, which will finally give you a **[Session](org.matrix.android.sdk.api.session.Session)**.
- From the **[Session](org.matrix.android.sdk.api.session.Session)**, you will be able to retrieve many Services, including the **[RoomService](org.matrix.android.sdk.api.session.room.RoomService)**.
- From the **[RoomService](org.matrix.android.sdk.api.session.room.RoomService)**, you will be able to list the rooms, create a **[Room](org.matrix.android.sdk.api.session.room.Room)**, and get a specific **[Room](org.matrix.android.sdk.api.session.room.Room)**.
- And from a **[Room](org.matrix.android.sdk.api.session.room.Room)**, you will be able to do many things, including get a **[Timeline](org.matrix.android.sdk.api.session.room.timeline.Timeline)**, send messages, etc.

Please read the whole documentation to learn more!
