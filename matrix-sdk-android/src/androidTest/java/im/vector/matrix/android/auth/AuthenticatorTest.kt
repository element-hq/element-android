package im.vector.matrix.android.auth

import androidx.test.annotation.UiThreadTest
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.AndroidJUnit4
import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.InstrumentedTest
import im.vector.matrix.android.OkReplayRuleChainNoActivity
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.matrix.android.internal.auth.AuthModule
import im.vector.matrix.android.internal.di.MatrixModule
import im.vector.matrix.android.internal.di.NetworkModule
import okreplay.*
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.standalone.StandAloneContext.loadKoinModules
import org.koin.standalone.inject
import org.koin.test.KoinTest


@RunWith(AndroidJUnit4::class)
internal class AuthenticatorTest : InstrumentedTest, KoinTest {

    init {
        Monarchy.init(context())
        val matrixModule = MatrixModule(context()).definition
        val networkModule = NetworkModule().definition
        val authModule = AuthModule().definition
        loadKoinModules(listOf(matrixModule, networkModule, authModule))
    }

    private val authenticator: Authenticator by inject()
    private val okReplayInterceptor: OkReplayInterceptor by inject()

    private val okReplayConfig = OkReplayConfig.Builder()
            .tapeRoot(AndroidTapeRoot(
                    context(), javaClass))
            .defaultMode(TapeMode.READ_WRITE) // or TapeMode.READ_ONLY
            .sslEnabled(true)
            .interceptor(okReplayInterceptor)
            .build()

    @get:Rule
    val testRule = OkReplayRuleChainNoActivity(okReplayConfig).get()

    @Test
    @UiThreadTest
    @OkReplay(tape = "auth", mode = TapeMode.READ_WRITE)
    fun auth() {

    }

    companion object {
        @ClassRule
        @JvmField
        val grantExternalStoragePermissionRule: GrantPermissionRule =
                GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }


}