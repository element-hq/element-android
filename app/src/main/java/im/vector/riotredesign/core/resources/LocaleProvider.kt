package im.vector.riotredesign.core.resources

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import java.util.*

class LocaleProvider(private val resources: Resources) {

    fun current(): Locale {
        return ConfigurationCompat.getLocales(resources.configuration)[0]
    }


}