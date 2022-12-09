

package im.vector.app.features.home

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ToggleButton
import android.widget.Toolbar
import com.google.android.flexbox.FlexboxLayout
import im.vector.app.R
import io.realm.Realm
import org.json.JSONArray
import org.webrtc.NetworkMonitor
import java.net.HttpURLConnection
import java.net.URL

class CitiesActivity : AppCompatActivity() {
    private lateinit var FLCities: FlexboxLayout
    private lateinit var btnDone: Button

    private var selectedCityButton: Button? = null
    private var selectedCityUuid: String? = null

    private var rootUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cities)

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
                    if (selectedCityButton !== null) {
                        selectedCityButton?.setBackgroundResource(R.drawable.round_shape_btn_inactive)
                    }
                    buttonView.setBackgroundResource(R.drawable.round_shape_btn_active)
                    selectedCityButton = buttonView
                    selectedCityUuid = categoryUuid

                    btnDone.isActivated = true
                    btnDone.backgroundTintList = getColorStateList(R.color.toggle_button_checked)
                }
            }

            FLCities.addView(toggleBtn)
        }
    }

    private fun setUpButtons() {
        Thread {
            val url = URL("$rootUrl/countries/" + (intent.extras?.getString("selectedCountryUuid") + "/cities"))
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
            val preferences = Realm.getApplicationContext()?.getSharedPreferences("bigstar", Context.MODE_PRIVATE)
            val editor = preferences?.edit()
            editor?.putString("city", selectedCityUuid)
            editor?.apply()
            finish()
        }
    }
}
