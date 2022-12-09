package im.vector.app.core.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.drjacky.imagepicker.ImagePicker
import com.google.android.flexbox.FlexboxLayout
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)

class CabinetActivity : AppCompatActivity() {

    private val price = 4990
    private var total = 0

    private var bannerFile: File? = null
    private var thumbnailFile: File? = null

    private var accessToken: String? = ""
    private var advertiserUuid: String? = ""

    private var bannerFileUuid: String? = ""
    private var thumbnailFileUuid: String? = ""

    private var isBannerUploaded: Boolean = false
    private var isThumbnailUploaded: Boolean = false

    private var mProgressBar: ProgressBar? = null
    private var btnCreateAd: Button? = null
    private var textDays: EditText? = null
    private var rootUrl: String = ""

    private var selectedCities: ArrayList<String> = ArrayList()
    
    private fun ImageView.setLocalImage(uri: Uri, applyCircle: Boolean = false) {
        val glide = Glide.with(this).load(uri).diskCacheStrategy(DiskCacheStrategy.NONE)
        if (applyCircle) {
            glide.apply(RequestOptions.circleCropTransform()).into(this)
        } else {
            glide.into(this)
        }
    }

    private val bannerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val uri = it.data?.data!!
                    val file = ImagePicker.getFile(it.data)!!
                    bannerFile = file
                    findViewById<ImageView>(R.id.iv_banner).setLocalImage(uri)
                    isBannerUploaded = true
                    validateInputs()
                } else parseError(it)
            }

    private val thumbnailLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val uri = it.data?.data!!
                    val file = ImagePicker.getFile(it.data)!!
                    thumbnailFile = file
                    findViewById<ImageView>(R.id.iv_thumbnail).setLocalImage(uri)
                    isThumbnailUploaded = true
                    validateInputs()
                } else parseError(it)
            }

    private fun pickBannerImage() {
        bannerLauncher.launch(
                ImagePicker.with(this)
                        .crop()
                        .galleryOnly()
                        .galleryMimeTypes(  //no gif images at all
                                mimeTypes = arrayOf(
                                        "image/png",
                                        "image/jpg",
                                        "image/jpeg"
                                )
                        )
                        .createIntent()
        )
    }

    private fun pickThumbnailImage() {
        thumbnailLauncher.launch(
                ImagePicker.with(this)
                        .crop()
                        .galleryOnly()
                        .galleryMimeTypes(  //no gif images at all
                                mimeTypes = arrayOf(
                                        "image/png",
                                        "image/jpg",
                                        "image/jpeg"
                                )
                        )
                        .createIntent()
        )
    }

    private fun parseError(activityResult: ActivityResult) {
        if (activityResult.resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(activityResult.data), Toast.LENGTH_SHORT)
                    .show()
        } else {
            Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cabinet)

        mProgressBar = findViewById(R.id.cabinetLoaderProgress)
        btnCreateAd = findViewById(R.id.btnCreateAd)
        textDays = findViewById(R.id.text_days)
        rootUrl = getString(R.string.backend_server_url)

        setUpListeners()
        calculateDates()
        setUpButtons()
    }

    private fun setUpListeners() {

        findViewById<LinearLayout>(R.id.ll_banner).setOnClickListener {
            pickBannerImage()
        }

        findViewById<LinearLayout>(R.id.ll_thumbnail).setOnClickListener {
            pickThumbnailImage()
        }

        btnCreateAd?.setOnClickListener {
            login()
            mProgressBar?.visibility = View.VISIBLE
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
        }

        findViewById<EditText>(R.id.text_description).addTextChangedListener {
            validateInputs()
        }

        findViewById<EditText>(R.id.text_title).addTextChangedListener {
            validateInputs()
        }



        textDays?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s != null) {
                    if (s.toString().isNotEmpty())
                        if ((s.toString().toInt()) > 365) {
                            textDays?.text = "365".toEditable()
                        }
                } else {
                    textDays?.text = "0".toEditable()
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                validateInputs()
                calculateDates()
            }
        })
    }

    private fun calculateDates() {
        if (textDays?.text!!.isNotEmpty()) {
            findViewById<TextView>(R.id.tv_date_to).text =
                    LocalDate.now().plusDays(textDays?.text!!.toString().toLong() + 1)
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            btnCreateAd?.text = getString(R.string.pay) + " " + total + "₸"
        } else {
            btnCreateAd?.text = getString(R.string.pay)
            findViewById<TextView>(R.id.tv_date_to).text = getString(R.string.ddmmyyyy)
        }
        findViewById<TextView>(R.id.tv_date_from).text =
                LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }

    private fun validateInputs() {
        total = if (textDays?.text!!.isNotEmpty()) {
            (price * textDays?.text!!.toString().toInt() * (selectedCities.size))
        } else {
            selectedCities.size * price
        }

        calculateDates()
        if (isBannerUploaded &&
                isThumbnailUploaded &&
                findViewById<EditText>(R.id.text_title).text!!.isNotEmpty() &&
                findViewById<EditText>(R.id.text_description).text!!.isNotEmpty() &&
                textDays?.text!!.isNotEmpty() &&
                selectedCities.isNotEmpty()
        ) {
            btnCreateAd?.backgroundTintList = getColorStateList(R.color.toggle_button_checked)
            btnCreateAd?.isEnabled = true
        } else {
            btnCreateAd?.backgroundTintList = getColorStateList(R.color.toggle_button_unchecked)
            btnCreateAd?.isEnabled = false
        }
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
                uploadThumbnail()
            }
        })
    }

    private fun createAd() {
        val formBody = FormBody.Builder()
                .add("title", findViewById<EditText>(R.id.text_title).text.toString())
                .add("description", findViewById<EditText>(R.id.text_description).text.toString())
                .add("thumbnailUuid", thumbnailFileUuid!!)
                .add("bannerUuid", bannerFileUuid!!)
                .add("advertiserUuid", advertiserUuid!!)
                .add("email", "example@gmail.com")
                .add("phoneNumber", "7777777777")
                .add(
                        "startsAt",
                        LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
                .add(
                        "endsAt",
                        LocalDate.now().plusDays(textDays?.text!!.toString().toLong() + 1)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
                .add("bigstarUrl", getSharedPreferences("bigstar", MODE_PRIVATE).getString("username", "")!!)


        if (findViewById<EditText>(R.id.text_youtube_url).text.isNotEmpty())
            formBody.add("youtubeUrl", findViewById<EditText>(R.id.text_youtube_url).text.toString())
        if (findViewById<EditText>(R.id.text_instagram_url).text.isNotEmpty())
            formBody.add("instagramUrl", findViewById<EditText>(R.id.text_instagram_url).text.toString())
        if (findViewById<EditText>(R.id.text_website_url).text.isNotEmpty())
            formBody.add("websiteUrl", findViewById<EditText>(R.id.text_website_url).text.toString())

        selectedCities.forEachIndexed { index, uuid ->
            formBody.add("countryUuids[$index]", uuid)
        }

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
                .post(formBody.build())
                .url("${rootUrl}/ads")
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            var mainHandler: Handler = Handler(this@CabinetActivity.mainLooper)

            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    201 -> {
                        val redirectUrl = JSONObject(response.body!!.string()).getString("redirectUrl")
                        mainHandler.post {
                            mProgressBar?.visibility = View.GONE;
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl)))
                            finish()
                        }
                    }
                    else -> {
                        mainHandler.post(Runnable {
                            mProgressBar?.visibility = View.GONE;
                            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                            Toast.makeText(this@CabinetActivity, JSONObject(response.body!!.string()).getString("message"), Toast.LENGTH_SHORT)
                                    .show()
                        })
                    }
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setUpButtons() {
        Thread {
            val url = URL("$rootUrl/countries")

            with(url.openConnection() as HttpURLConnection) {
                inputStream.bufferedReader().use {
                    it.lines().forEach { line ->
                        val categories = JSONArray(line);
                        for (i in 0 until categories.length()) {
                            drawButton(
                                    categories.getJSONObject(i).getString("name"),
                                    categories.getJSONObject(i).getString("uuid")
                            )
                        }
                    }
                }

            }
        }.start()
    }

    private fun drawButton(countryName: String, countryUuid: String) {
        runOnUiThread {
            val toggleBtn = LayoutInflater.from(this@CabinetActivity)
                    .inflate(R.layout.fragment_cabinet_btn, null) as ToggleButton;
            toggleBtn.textOn = countryName
            toggleBtn.textOff = countryName
            toggleBtn.text = countryName
            val layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginEnd = 20
            layoutParams.bottomMargin = 20
            toggleBtn.layoutParams = layoutParams

            toggleBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    Thread {
                        val url = URL("$rootUrl/countries/$countryUuid/cities")
                        with(url.openConnection() as HttpURLConnection) {
                            inputStream.bufferedReader().use {
                                it.readLines().forEach { line ->
                                    val cities = JSONArray(line);
                                    for (i in 0 until cities.length()) {
                                        drawCities(buttonView, cities.getJSONObject(i).getString("name"), cities.getJSONObject(i).getString("uuid"))
                                    }
                                }
                            }

                        }

                        runOnUiThread {
                            validateInputs()
                        }

                    }.start()
                    buttonView.backgroundTintList = getColorStateList(R.color.toggle_button_checked)
                } else {
                    Thread {
                        val url = URL("$rootUrl/countries/$countryUuid/cities")

                        with(url.openConnection() as HttpURLConnection) {
                            inputStream.bufferedReader().use {
                                it.readLines().forEach { line ->
                                    val cities = JSONArray(line);
                                    for (i in 0 until cities.length()) {
                                        runOnUiThread {
                                            val index = findViewById<FlexboxLayout>(R.id.fl_countries).indexOfChild(buttonView) + 1
                                            selectedCities.remove(cities.getJSONObject(i).getString("uuid"))
                                            findViewById<FlexboxLayout>(R.id.fl_countries).removeViewAt(index)
                                        }
                                    }
                                }
                            }

                        }

                        runOnUiThread {
                            validateInputs()
                        }
                    }.start()
                    buttonView.backgroundTintList = getColorStateList(R.color.toggle_button_unchecked)
                }
            }

            findViewById<FlexboxLayout>(R.id.fl_countries).addView(toggleBtn)
        }
    }

    private fun drawCities(buttonView: Button, cityName: String, cityUuid: String) {
        runOnUiThread {
            val toggleBtn = LayoutInflater.from(this@CabinetActivity)
                    .inflate(R.layout.fragment_cabinet_btn, null) as ToggleButton;
            toggleBtn.textOn = cityName
            toggleBtn.textOff = cityName
            toggleBtn.text = cityName
            val layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginEnd = 20
            layoutParams.bottomMargin = 20
            toggleBtn.layoutParams = layoutParams

            toggleBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    selectedCities.add(cityUuid)
                    buttonView.backgroundTintList = getColorStateList(R.color.toggle_button_checked)
                } else {
                    selectedCities.remove(cityUuid)
                    buttonView.backgroundTintList = getColorStateList(R.color.toggle_button_unchecked)
                }
                validateInputs()
            }

            findViewById<FlexboxLayout>(R.id.fl_countries).addView(toggleBtn, findViewById<FlexboxLayout>(R.id.fl_countries).indexOfChild(buttonView) + 1)
        }
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

    private fun uploadBanner() {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file", bannerFile!!.name,
                        bannerFile!!.asRequestBody("text/x-markdown; charset=utf-8".toMediaType())
                )
                .build()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
                .post(requestBody)
                .url("${rootUrl}/files/upload")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                bannerFileUuid = JSONObject(response.body!!.string()).getString("uuid")
                createAd()
            }
        })
    }

    private fun uploadThumbnail() {
        val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file", thumbnailFile!!.name,
                        thumbnailFile!!.asRequestBody("text/x-markdown; charset=utf-8".toMediaType())
                )
                .build()

        val okHttpClient = OkHttpClient()
        val request = Request.Builder()
                .post(requestBody)
                .url("${rootUrl}/files/upload")
                .addHeader("Authorization", "Bearer $accessToken")
                .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }

            override fun onResponse(call: Call, response: Response) {
                thumbnailFileUuid = JSONObject(response.body!!.string()).getString("uuid")
                uploadBanner()
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
