package im.vector.riotredesign.core.resources

import android.content.res.Resources
import android.support.v4.os.ConfigurationCompat
import java.util.*

class LocaleProvider(private val resources: Resources) {

    fun current(): Locale {
        return ConfigurationCompat.getLocales(resources.configuration)[0]
    }


}