//package im.vector.riotredesign.core.services
//
//import android.app.NotificationManager
//import android.content.Context
//import android.content.Intent
//import android.os.Build.VERSION.SDK_INT
//import android.os.Build.VERSION_CODES
//import android.os.Handler
//import android.os.HandlerThread
//import android.os.Looper
//import androidx.core.content.ContextCompat.startForegroundService
//import im.vector.matrix.android.api.Matrix
//import im.vector.matrix.android.api.session.Session
//import im.vector.riotredesign.R
//import im.vector.riotredesign.features.notifications.NotificationUtils
//import timber.log.Timber
//import java.net.HttpURLConnection
//import java.net.URL
//
//
///**
// *
// * This is used to display message notifications to the user when Push is not enabled (or not configured)
// *
// * This service is used to implement a long pooling mechanism in order to get messages from
// * the home server when the user is not interacting with the app.
// *
// * It is intended to be started when the app enters background, and stopped when app is in foreground.
// *
// * When in foreground, the app uses another mechanism to get messages (doing sync wia a thread).
// *
// */
//class HttpLongPoolingSyncService : VectorService() {
//
//    private var mServiceLooper: Looper? = null
//    private var mHandler: Handler? = null
//    private val currentSessions = ArrayList<Session>()
//    private var mCount = 0
//    private var lastTimeMs = System.currentTimeMillis()
//
//    lateinit var myRun: () -> Unit
//    override fun onCreate() {
//        //Add the permanent listening notification
//        super.onCreate()
//
//        if (SDK_INT >= VERSION_CODES.O) {
//            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val notification = NotificationUtils.buildForegroundServiceNotification(applicationContext, R.string.notification_listening_for_events, false)
//            startForeground(NotificationUtils.NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
//        }
//        val thread = HandlerThread("My service Handler")
//        thread.start()
//
//        mServiceLooper = thread.looper
//        mHandler = Handler(mServiceLooper)
//        myRun = {
//            val diff = System.currentTimeMillis() - lastTimeMs
//            lastTimeMs = System.currentTimeMillis()
//            val isAlive = Matrix.getInstance().currentSession?.isSyncThreadAlice()
//            val state = Matrix.getInstance().currentSession?.syncThreadState()
//            Timber.w(" timeDiff[${diff/1000}] Yo me here $mCount, sync thread is Alive? $isAlive, state:$state")
//            mCount++
//            mHandler?.postDelayed(Runnable { myRun() }, 10_000L)
//        }
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        //START_STICKY mode makes sense for things that will be explicitly started
//        //and stopped to run for arbitrary periods of time
//
//        mHandler?.post {
//            myRun()
//        }
//        return START_STICKY
//    }
//
//
//    override fun onDestroy() {
//        //TODO test if this service should be relaunched (preference)
//        Timber.i("Service is destroyed, relaunch asap")
//        Intent(applicationContext, RestartBroadcastReceiver::class.java).also { sendBroadcast(it) }
//        super.onDestroy()
//    }
//
//    companion object {
//
//        fun startService(context: Context) {
//            Timber.i("Start sync service")
//            val intent = Intent(context, HttpLongPoolingSyncService::class.java)
//            try {
//                if (SDK_INT >= VERSION_CODES.O) {
//                    startForegroundService(context, intent)
//                } else {
//                    context.startService(intent)
//                }
//            } catch (ex: Throwable) {
//                //TODO
//                Timber.e(ex)
//            }
//        }
//    }
//}