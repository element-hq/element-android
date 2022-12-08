package com.contusfly.views

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class AdPopUp(
        activity: Activity,
        context: Context,
        title: String,
        description: String,
        bannerSrc: String,
        ad: JSONObject,
        rootUrl: String
) : Dialog(context, R.style.Theme_Dialog) {
    var activity: Activity?
    var title: String
    var description: String
    var bannerSrc: String
    var ad: JSONObject
    private var rootUrl: String

    init {
        this.activity = activity
        this.title = title
        this.description = description
        this.bannerSrc = bannerSrc
        this.ad = ad
        this.rootUrl = rootUrl

        this.setCancelable(true);
        this.setCanceledOnTouchOutside(true);
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setGravity(Gravity.BOTTOM)
    }

    private fun loadImageWithGlide(context: Context, imgUrl: String?, imgView: ImageView, errorImg: Drawable?) {
        if (imgUrl != null && imgUrl.isNotEmpty()) {
            val options = RequestOptions().placeholder(imgView.drawable ?: errorImg)
                    .error(errorImg).diskCacheStrategy(DiskCacheStrategy.ALL).priority(Priority.HIGH)
            val requestBuilder: RequestBuilder<Drawable> = GlideApp.with(context).asDrawable().sizeMultiplier(0.1f)
            Glide.with(context).load(imgUrl).thumbnail(requestBuilder).apply(options).into(imgView)
        } else imgView.setImageDrawable(errorImg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.fragment_ad_popup)

        window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        val textDescription = findViewById<TextView>(R.id.tv_ad_description)

        textDescription.movementMethod = ScrollingMovementMethod()
        textDescription.text = description

        (findViewById<TextView>(R.id.tv_ad_title)).text = title

        loadImageWithGlide(context, bannerSrc, findViewById(R.id.iv_ad_banner), null)

        val ivAdYoutube = findViewById<ImageView>(R.id.iv_ad_youtube)
        val ivAdInstagram = findViewById<ImageView>(R.id.iv_ad_instagram)
        val ivAdBigstar = findViewById<ImageView>(R.id.iv_ad_bigstar)
        val ivAdWebsite = findViewById<ImageView>(R.id.iv_ad_website)
        val youtubeUrl = ad.getString("youtubeUrl")
        val instagramUrl = ad.getString("instagramUrl")
        val bigstarUrl = ad.getString("bigstarUrl")
        val websiteUrl = ad.getString("websiteUrl")

        if (bigstarUrl.isNotEmpty()) {
            ivAdBigstar.setImageResource(R.drawable.ic_bigstar_active)
            ivAdBigstar.setOnClickListener {
                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .patch(FormBody.Builder().build())
                        .url(rootUrl + "/ads/" + ad.getString("uuid") + "/bigstar/click")
                        .build()
                okHttpClient.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                })

                this.dismiss()
            }
        }


        if (instagramUrl.isNotEmpty()) {
            ivAdInstagram.setImageResource(R.drawable.ic_instagram_active)
            ivAdInstagram.setOnClickListener {
                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .patch(FormBody.Builder().build())
                        .url(rootUrl + "/ads/" + ad.getString("uuid") + "/instagram/click")
                        .build()
                okHttpClient.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                })


                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse(instagramUrl)
                startActivity(context, openURL, null)
            }
        }

        if (youtubeUrl.isNotEmpty()) {
            ivAdYoutube.setImageResource(R.drawable.ic_youtube_active)
            ivAdYoutube.setOnClickListener {
                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .patch(FormBody.Builder().build())
                        .url(rootUrl + "/ads/" + ad.getString("uuid") + "/youtube/click")
                        .build()
                okHttpClient.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                })

                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse(youtubeUrl)
                startActivity(context, openURL, null)
            }
        }

        if (websiteUrl.isNotEmpty()) {
            ivAdWebsite.setImageResource(R.drawable.ic_website_active)
            ivAdWebsite.setOnClickListener {
                val okHttpClient = OkHttpClient()
                val request = Request.Builder()
                        .patch(FormBody.Builder().build())
                        .url(rootUrl + "/ads/" + ad.getString("uuid") + "/website/click")
                        .build()
                okHttpClient.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                    }
                })

                val openURL = Intent(Intent.ACTION_VIEW)
                openURL.data = Uri.parse(websiteUrl)
                startActivity(context, openURL, null)
            }
        }
    }
}
