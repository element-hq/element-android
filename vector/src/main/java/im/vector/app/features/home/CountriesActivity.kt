package im.vector.app.features.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import im.vector.app.R
import org.json.JSONArray
import org.webrtc.NetworkMonitor
import java.net.HttpURLConnection
import java.net.URL

class CountriesActivity : AppCompatActivity() {
    private lateinit var FLCities: FlexboxLayout
    private lateinit var btnDone: Button

    private var selectedCountryButton: Button? = null
    private var selectedCountryUuid: String? = null

    private var rootUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_countries)

        FLCities = findViewById(R.id.fl_cities)
        btnDone = findViewById(R.id.btn_categories_done)

        rootUrl = getString(R.string.backend_server_url)
        setUpButtons()
    }

    private fun drawButton(categoryName: String, categoryUuid: String) {
        runOnUiThread {
            val toggleBtn = LayoutInflater.from(this)
                    .inflate(R.layout.fragment_city_btn, null) as ToggleButton;
            toggleBtn.textOn = categoryName
            toggleBtn.textOff = categoryName
            toggleBtn.text = categoryName
            val layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.marginEnd = 30
            layoutParams.bottomMargin = 30
            toggleBtn.layoutParams = layoutParams

            toggleBtn.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    if (selectedCountryButton !== null) {
                        selectedCountryButton?.setBackgroundResource(R.drawable.round_shape_btn_inactive)
                    }
                    buttonView.setBackgroundResource(R.drawable.round_shape_btn_active)
                    selectedCountryButton = buttonView
                    selectedCountryUuid = categoryUuid

                    btnDone.isActivated = true
                    btnDone.setBackgroundResource(R.drawable.round_shape_btn_active)
                }
            }

            FLCities.addView(toggleBtn)
        }
    }

    private fun setUpButtons() {
        Thread {
            val url = URL("$rootUrl/countries")
            if (NetworkMonitor.isOnline()) {
                with(url.openConnection() as HttpURLConnection) {
                    inputStream.bufferedReader().use {
                        it.readLines().forEach { line ->
                            val categories = JSONArray(line);
                            for (i in 0 until categories.length()) {
                                drawButton(
                                        categories.getJSONObject(i).getString("name"),
                                        categories.getJSONObject(i).getString("uuid"),
                                )
                            }
                        }
                    }
                }
            }
        }.start()

        btnDone.setOnClickListener {
            if (!btnDone.isActivated) return@setOnClickListener

            val i = Intent(this@CountriesActivity, CitiesActivity::class.java)
            i.putExtra("selectedCountryUuid", selectedCountryUuid)
            startActivity(i)
            finish()
        }
    }
}
