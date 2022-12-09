package im.vector.app.features.home

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import im.vector.app.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MyAdsActivity : AppCompatActivity() {
    private var advertiserUuid: String? = ""
    private var accessToken: String? = ""
    private var rootUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootUrl = getString(R.string.backend_server_url)
        setContentView(R.layout.activity_my_ads)
        login()
    }

    private fun setUpAdvertiser() {
        val request = Request.Builder()
                .addHeader("Authorization", "Bearer $accessToken")
                .url("${rootUrl}/auth/me")
                .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                advertiserUuid = JSONObject(response.body!!.string()).getJSONObject("advertiser")
                        .getString("uuid")
                setupAds()
            }
        })
    }

    private fun setupAds() {
        val request = Request.Builder()
                .addHeader("Authorization", "Bearer $accessToken")
                .url("${rootUrl}/ads/advertiser/$advertiserUuid")
                .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            var mainHandler: Handler = Handler(this@MyAdsActivity.mainLooper)

            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                mainHandler.post(Runnable {
                    val ads = JSONArray(response.body!!.string())
                    if (ads.length() == 0)
                        findViewById<LinearLayout>(R.id.ll_no_ads).visibility = VISIBLE;
                    else
                        for (i in 0 until ads.length()) {
                            val ad = ads.getJSONObject(i)
                            val cardView = LayoutInflater.from(this@MyAdsActivity).inflate(R.layout.fragment_ad, null) as CardView
                            val layoutParams = ViewGroup.MarginLayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            layoutParams.setMargins(30, 10, 30, 10)
                            cardView.layoutParams = layoutParams

                            cardView.findViewById<TextView>(R.id.tv_ad_title).text = ad.getString("title")
                            cardView.findViewById<TextView>(R.id.tv_ad_description).text = ad.getString("description").replace("[\n\r]", " ")
                            cardView.findViewById<TextView>(R.id.tv_ad_clicks_number).text = ad.getString("clicksNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_shows_number).text = ad.getString("showsNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_youtube_clicks).text = ad.getString("youtubeClicksNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_bigstar_clicks).text = ad.getString("bigstarClicksNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_instagram_clicks).text = ad.getString("instagramClicksNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_website_clicks).text = ad.getString("websiteClicksNumber")
                            cardView.findViewById<TextView>(R.id.tv_ad_created_at).text =
                                    LocalDate.parse(ad.getString("createdAt").subSequence(0, 10)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            findViewById<LinearLayout>(R.id.ll_ads).addView(cardView)
                        }
                })
            }
        })
    }

    private fun login() {
        val formBody = FormBody.Builder()
                .add("username", getSharedPreferences("bigstar", MODE_PRIVATE).getString("username", "")!!)
                .add("password", md5Hash(getSharedPreferences("bigstar", MODE_PRIVATE).getString("username", "")!!))
                .add("fingerprint", md5Hash(System.getProperty("http.agent")!!))
                .build()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
                .post(formBody)
                .url("${rootUrl}/auth/login")
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    401 -> {
                        createAdvertiser()
                        login()
                    }
                    200 -> {
                        accessToken = JSONObject(response.body!!.string()).getString("access_token")
                        setUpAdvertiser()
                    }
                }
            }
        })
    }

    private fun createAdvertiser() {
        val formBody = FormBody.Builder()
                .add("username", getSharedPreferences("bigstar", MODE_PRIVATE).getString("username", "")!!)
                .add("password", md5Hash(getSharedPreferences("bigstar", MODE_PRIVATE).getString("username", "")!!))
                .build()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
                .post(formBody)
                .url("${rootUrl}/advertisers")
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                println(response.body?.string())
            }
        })
    }

    private fun md5Hash(str: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bigInt = BigInteger(1, md.digest(str.toByteArray(Charsets.UTF_8)))
        return String.format("%032x", bigInt)
    }
}
