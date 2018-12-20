package im.vector.riotredesign.features

import android.os.Bundle
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.core.platform.RiotActivity
import im.vector.riotredesign.features.home.HomeActivity
import im.vector.riotredesign.features.login.LoginActivity
import org.koin.android.ext.android.inject


class MainActivity : RiotActivity() {

    private val authenticator = Matrix.getInstance().authenticator()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = if (authenticator.hasActiveSessions()) {
            HomeActivity.newIntent(this)
        } else {
            LoginActivity.newIntent(this)
        }
        startActivity(intent)
        finish()
    }

}