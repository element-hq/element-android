package org.matrix.android.authentication

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MatrixAuthenticationService : Service() {

    private lateinit var mAuthenticator : MatrixAccountAuthenticator

    override fun onCreate() {
        super.onCreate()
        mAuthenticator = MatrixAccountAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder {
        return mAuthenticator.iBinder
    }
}
